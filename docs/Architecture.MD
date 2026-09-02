# Architecture

## Overview

Seven independent Spring Boot services, each with its own PostgreSQL database. Services communicate synchronously via OpenFeign and asynchronously via Kafka. A centralized config server pulls configuration from a private GitHub repo. Eureka handles service discovery — Feign clients resolve service names through it instead of hardcoded URLs.

The domain split follows bounded contexts: merchant identity, payment processing, card vault, and operations (settlement + webhook delivery). Each boundary is enforced — no service queries another's database directly.

---

## Service Map

```
┌─────────────────────────────────────────────────────────┐
│                    Config Server :8888                   │
│           ← GitHub: distributed-payment-gateway-config  │
└─────────────────────┬───────────────────────────────────┘
                      │  all services bootstrap here
┌─────────────────────▼───────────────────────────────────┐
│                  Eureka Discovery :8761                  │
│              all services register here                  │
└─────────────────────┬───────────────────────────────────┘
                      │
         Client ──────▼──────
                API Gateway :8010
                      │
         ┌────────────┼────────────┬──────────────┐
         ▼            ▼            ▼              ▼
   merchant-svc  payment-svc  vault-svc   operations-svc
     :9010         :9020        :9030         :9040
         │            │                         │
         DB           DB            DB          DB
   merchant-db  payment-db    vault-db   operations-db
```

---

## Startup Order

```
1. config-service     (8888)  ← must be first
2. discovery-service  (8761)  ← needs config server
3. merchant-service   (9010)  ─┐
4. payment-service    (9020)   ├─ need config + Eureka
5. vault-service      (9030)   │
6. operations-service (9040)  ─┘
7. api-gateway-service (8010) ← last, after all services registered
```

---

## Request Flow

### Merchant route (JWT auth)

```
Client → POST /v1/auth/login
  ↓
API Gateway
  → JwtAuthHandler: no token needed for /login → pass through
  ↓
merchant-service
  → authenticate credentials
  → generate JWT (sub=email, merchant_id, role claims, HMAC-SHA256)
  → return { accessToken }

Client → GET /v1/merchants/api-keys
  Authorization: Bearer <token>
  ↓
API Gateway
  → JwtAuthHandler: verify token signature
  → extract merchantId, role
  → add X-Merchant-Id, X-Role headers
  → forward to merchant-service
  ↓
merchant-service
  → read X-Merchant-Id from header (trusts gateway)
  → return API keys for that merchant
```

### Payment route (API Key auth)

```
Client → POST /v1/payments
  Authorization: Basic base64(keyId:secret)
  X-Idempotency-Key: unique-key-123
  ↓
API Gateway
  → ApiKeyAuthHandler:
      decode Base64 → split keyId:secret
      Redis lookup: "apikey:{keyId}" (5min TTL)
      cache miss → Feign → merchant-service GET /internal/api-keys/{keyId}
      BCrypt verify secret
      check rate limit (Redis Lua script, per keyId)
      check idempotency key (Redis, per merchantId:idempotencyKey)
  → add X-Merchant-Id, X-Key-Id headers
  → forward to payment-service
  ↓
payment-service
  → read merchantId from X-Merchant-Id header
  → process payment
```

---

## Cross-Service Communication

### Feign Clients

| Caller | Target | Endpoint | Purpose |
|---|---|---|---|
| api-gateway | merchant-service | `GET /internal/api-keys/{keyId}` | API key lookup for auth |
| payment-service | vault-service | `POST /internal/vault/charge` | Charge a tokenized card |
| payment-service | merchant-service | `POST /internal/customers/find-or-create` | Customer upsert on order creation |
| operations-service | merchant-service | `GET /internal/merchants/{id}/webhook-targets` | Active webhook configs for event |
| operations-service | merchant-service | `GET /internal/merchants/active-ids` | Active merchant IDs for settlement |
| operations-service | merchant-service | `GET /internal/merchants/{id}/settlement-bank-details` | Bank account for transfer |
| operations-service | payment-service | `GET /internal/payments/unsettled-captured` | Payments to settle |
| operations-service | payment-service | `POST /internal/payments/mark-settled` | Mark payments settled after transfer |

All Feign clients resolve service URLs through Eureka — no hardcoded ports.

### Kafka (Async)

```
payment-service
  └── OutboxPoller (@Scheduled 5s)
        → publishes to:
            payments.events
            orders.events
            refund.events
            settlement.events

operations-service
  └── WebhookKafkaConsumer
        → listens on all 4 topics
        → manual ack (does not ack on exception)
        → creates WebhookEvent per merchant target
        → enqueues to Redis ZSet
```

---

## Async Pipeline — Full Flow

```
Payment captured in payment-service
  ↓
OutboxEventPublisher.publish()
  → saves OutboxEvent (status=PENDING) in SAME DB transaction as payment
  [atomicity guaranteed — no dual-write problem]
  ↓
OutboxPoller (runs every 5s)
  → finds PENDING outbox events
  → publishes to Kafka: { eventType, aggregateType, aggregateId, data }
  → key = merchantId (ensures ordering per merchant)
  → marks PUBLISHED on success
  → increments attempt on failure (max 3 → FAILED)
  ↓
WebhookKafkaConsumer (operations-service)
  → extracts merchantId, eventType from envelope
  → calls MerchantWebhookApi.getActiveConfigForEvent() via Feign
  → for each active config:
      decrypt webhook secret (AES-256-GCM)
      sign payload: HMAC-SHA256(payload, secret)
      save WebhookEvent (status=PENDING, nextRetryAt=now)
      enqueue to Redis ZSet (score = nextRetryAt epoch ms)
  → manual ack
  ↓
WebhookDeliveryScheduler (runs every 1s)
  → Redis ZSet rangeByScore(0, now, limit=100)
  → removes due entries atomically
  → dispatches each to virtual thread
  ↓
WebhookDeliverExecutor
  → HTTP POST to merchant targetUrl
  → on success: mark DELIVERED
  → on failure: increment attempts, calculate next retry, re-enqueue
    Retry schedule: 1min → 5min → 30min → 2hr → 8hr → 24hr (max 7 attempts)
  ↓
WebhookDlqRecorder (after max attempts)
  → @Transactional(REQUIRES_NEW)
  → mark WebhookEvent DEAD
  → save DLQEvent with full payload + final error

DB Reconciler (runs every 10s)
  → re-enqueues any PENDING events missed due to crash
  → uses enqueueIfAbsent (no duplicates)
```

---

## Settlement Pipeline

```
SettlementEngine (@Scheduled cron: 0 0 23 * * *)
  → MerchantLookupService.listActiveMerchantIds() via Feign
  → for each merchantId → virtual thread:
      ↓
  SettlementTransactionExecutor.processForMerchant(merchantId, today)
      → PaymentLookupService.findUnsettledCapturedPayments() via Feign
        (@Lock PESSIMISTIC_WRITE — prevents double-settlement)
      → calculate:
          gross  = sum of payment amounts
          fee    = gross × 2%
          gst    = fee × 18%
          net    = gross − fee − gst
      → create Settlement (status=TRANSFER_PENDING)
      → create SettlementPayment records per payment
      → BankTransferProcessor.initiate() → returns TXN_ ref
      ↓
BankSettlementCallbackSimulator (@Scheduled 5s, for local testing)
  → finds TRANSFER_PENDING settlements
  → generates UTR number
  → SettlementTransactionExecutor.resolveTransfer()
      → mark Settlement COMPLETED
      → PaymentLookupService.markSettled() via Feign
          → sets settledAt on each payment in payment-service
```

---

## Common Library

`common-lib` is a shared JAR included by all services. Beans are registered via Spring Boot AutoConfiguration — services get everything without manual wiring.

**Auto-configurations registered in:**
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

| Configuration Class | What it provides |
|---|---|
| `SharedAuditAutoConfiguration` | JPA auditing, `AuditorAwareImpl` (reads from MerchantContext) |
| `SharedKafkaAutoConfiguration` | `KafkaProperties`, topic resolution by `EventAggregateType` |
| `SharedResilienceAutoConfigurations` | `IdempotencyFilter`, rate limiting (fixed/sliding window/token bucket) |
| `SharedExceptionAutoConfiguration` | `GlobalExceptionHandler`, structured error responses |
| `SharedSecurityAutoConfiguration` | `JwtUtils`, `AesEncryptionConfig` (master key encryptor bean), `SignerUtil` |
| `SharedWebFilterAutoConfiguration` | `MerchantContextFilter`, `RequestContextFilter` |

Other shared components: `MerchantContext` (@RequestScope), `Money`, `BaseEntity`, all enums, Feign DTOs.

---

## Database Schema — Key Design Choices

**Sensitive columns:**

| Column | Table | Storage |
|---|---|---|
| `keySecretHash` | `ApiKey` | BCrypt hash — never reversible |
| `previousKeySecretHash` | `ApiKey` | BCrypt hash — grace period rotation |
| `encryptedPan` | `VaultCard` | AES-256-GCM encrypted |
| `encryptedDek` | `VaultCard` | DEK encrypted by master key |
| `webhookSecret` | `MerchantWebhookConfig` | AES-256-GCM encrypted |

**Pessimistic locking queries:**
- `OrderRepository.findByIdAndMerchantIdForUpdate` — prevents duplicate payments on same order
- `PaymentRepository.findByIdAndMerchantIdForUpdate` — capture/refund safety
- `PaymentRepository.findByMerchantIdAndStatusForUpdate` — settlement, prevents double-settlement