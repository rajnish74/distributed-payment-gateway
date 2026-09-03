# Architecture

## Overview

Seven independent Spring Boot services, each with its own PostgreSQL database. Services communicate synchronously via OpenFeign and asynchronously via Kafka. A centralized config server pulls configuration from a private GitHub repo. Eureka handles service discovery — Feign clients resolve service names through it instead of hardcoded URLs.

The domain split follows four bounded contexts: **merchant identity**, **payment processing**, **card vault**, and **operations** (settlement + webhook delivery). Each boundary is enforced at the code level — no service queries another's database directly.

---

## Service Map

```
┌─────────────────────────────────────────────────────────────┐
│                    Config Server :8888                       │
│         ← GitHub: distributed-payment-gateway-config        │
└──────────────────────┬──────────────────────────────────────┘
                       │  all services bootstrap here
┌──────────────────────▼──────────────────────────────────────┐
│                  Eureka Discovery :8761                      │
│              all services register here                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
          Client ──────▼──────────────
                 API Gateway :8010
                       │
          ┌────────────┼────────────┬──────────────┐
          ▼            ▼            ▼              ▼
    merchant-svc  payment-svc  vault-svc   operations-svc
      :9010         :9020        :9030         :9040
          │            │            │              │
        (DB)         (DB)         (DB)           (DB)
  merchant-db   payment-db   vault-db    operations-db
```

---

## Startup Order

Services must start in this exact order:

```
1. config-service     (8888)  ← must be first — others won't start without it
2. discovery-service  (8761)  ← Eureka must be up before services register
3. merchant-service   (9010)  ─┐
4. payment-service    (9020)   ├─ any order after Eureka is ready
5. vault-service      (9030)   │
6. operations-service (9040)  ─┘
7. api-gateway-service (8010) ← last, after all services are in Eureka
```

---

## Request Flow

### Sequence Diagram — Payment Initiation

```
Client          Gateway         payment-svc     vault-svc      Kafka
  │                │                 │               │             │
  │─POST /v1/payments──────────────► │               │             │
  │                │                 │               │             │
  │         GatewayAuthFilter        │               │             │
  │         decode Basic Auth        │               │             │
  │         Redis cache lookup       │               │             │
  │         (cache miss)             │               │             │
  │         Feign→merchant-svc       │               │             │
  │         BCrypt verify            │               │             │
  │         rate limit check         │               │             │
  │         add X-Merchant-Id header │               │             │
  │                │                 │               │             │
  │                │─forward────────►│               │             │
  │                │                 │               │             │
  │                │            validate order       │             │
  │                │            (pessimistic lock)   │             │
  │                │                 │               │             │
  │                │                 │─Feign charge─►│             │
  │                │                 │               │             │
  │                │                 │◄──token result─│             │
  │                │                 │               │             │
  │                │            create Payment       │             │
  │                │            status=AUTHORIZING   │             │
  │                │            save OutboxEvent     │             │
  │                │            (same transaction)   │             │
  │                │                 │               │             │
  │◄─────201 Created────────────────│               │             │
  │                │                 │               │             │
  │           [5s later]             │               │             │
  │                │            OutboxPoller         │             │
  │                │            picks up event       │             │
  │                │                 │──publish─────────────────►  │
  │                │                 │               │      payments.events
```

### Sequence Diagram — Gateway Auth

```
Client               API Gateway              merchant-svc       Redis
  │                       │                        │               │
  │─Authorization: Basic──►│                        │               │
  │                       │                        │               │
  │              GatewayAuthFilter                 │               │
  │              detect "Basic " prefix            │               │
  │                       │                        │               │
  │                       │──GET apikey:{keyId}────────────────────►│
  │                       │                        │               │
  │                       │◄──cache hit (5min TTL)──────────────────│
  │                       │                        │               │
  │              BCrypt.verify(secret, hash)       │               │
  │              rate limit check (Lua)            │               │
  │              add X-Merchant-Id header          │               │
  │                       │                        │               │
  │              [cache miss path]                 │               │
  │                       │──Feign GET /internal/api-keys/{keyId}──►│
  │                       │◄──ApiKeyCacheEntry──────────────────────│
  │                       │──cache.put(keyId, entry, 5min)─────────►│
```

---

## Gateway Auth — Actual Code

The gateway has a single filter that handles both JWT and API Key auth:

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain filterChain) {

    if (publicRouteMatcher.isPublic(request.getRequestURI())) {
        filterChain.doFilter(request, response);
        return;
    }

    String authHeader = request.getHeader("Authorization");

    Map<String, String> identityHeaders = Map.of();
    if (authHeader.startsWith(BASIC_PREFIX)) {
        identityHeaders = apikeyAuthHandler.authenticate(authHeader, response);
    } else if (authHeader.startsWith(BEARER_PREFIX)) {
        identityHeaders = jwtAuthHandler.authenticate(
                authHeader.substring(BEARER_PREFIX.length()));
    }

    // Wrap request with identity headers — downstream services trust these
    HeaderAugumentingRequestWrapper wrapped = new HeaderAugumentingRequestWrapper(request);
    identityHeaders.forEach(wrapped::putHeader);

    filterChain.doFilter(wrapped, response);
}
```

Downstream services read `X-Merchant-Id` and `X-Key-Id` from the request headers — they don't re-authenticate. The gateway is the single point of truth for identity.

---

## Payment State Machine — Actual Code

The state machine is a static immutable map. A `Transition` is a Java record of (currentState, event). If the combination doesn't exist in the map, the transition is rejected.

```java
private record Transition(PaymentStatus from, PaymentEvent event) {}

private static final Map<Transition, PaymentStatus> TRANSITION = Map.ofEntries(
    Map.entry(new Transition(CREATED,            AUTHORIZE_ATTEMPT),  AUTHORIZING),
    Map.entry(new Transition(AUTHORIZING,        AUTHORIZE_SUCCESS),  AUTHORIZED),
    Map.entry(new Transition(AUTHORIZING,        AUTHORIZE_FAILURE),  FAILED),
    Map.entry(new Transition(AUTHORIZED,         CAPTURE_REQUEST),    CAPTURING),
    Map.entry(new Transition(CAPTURING,          CAPTURE_SUCCESS),    CAPTURED),
    Map.entry(new Transition(CAPTURING,          CAPTURE_FAILURE),    AUTHORIZED),
    Map.entry(new Transition(CAPTURED,           REFUND_INIT),        PARTIALLY_REFUNDED),
    Map.entry(new Transition(PARTIALLY_REFUNDED, REFUND_SUCCESS),     REFUNDED),
    Map.entry(new Transition(CAPTURED,           REFUND_SUCCESS),     REFUNDED),
    Map.entry(new Transition(CAPTURED,           SETTLE),             SETTLED),
    Map.entry(new Transition(SETTLED,            REFUND_INIT),        PARTIALLY_REFUNDED),
    Map.entry(new Transition(CREATED,            CANCEL),             CANCELLED),
    Map.entry(new Transition(AUTHORIZING,        CANCEL),             CANCELLED),
    Map.entry(new Transition(AUTHORIZED,         CAPTURE_TIMEOUT),    AUTH_EXPIRED)
);

public PaymentStatus transition(PaymentStatus current, PaymentEvent event) {
    PaymentStatus next = TRANSITION.get(new Transition(current, event));
    if (next == null) {
        throw new InvalidStateTransitionException(current.name(), event.name());
    }
    return next;
}
```

Every transition goes through `PaymentTransitionLogService.apply()` which persists a `PaymentTransitionLog` record with `fromStatus`, `toStatus`, `event`, `actor`, and `occurredAt`.

---

## Transactional Outbox — Actual Code

The outbox pattern solves the dual-write problem. The event is saved to the DB in the same transaction as the business entity — before Kafka is involved at all.

```java
// OutboxPoller.java — runs every 5 seconds
@Scheduled(fixedDelay = 5000)
public void poll() {
    List<OutboxEvent> pendingEvents = outboxEventRepository
            .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

    for (OutboxEvent event : pendingEvents) {
        try {
            String topic = kafkaProperties.topicFor(event.getAggregateType());
            String key = extractMerchantId(event.getPayload()); // merchant-keyed for ordering

            Map<String, Object> envelope = Map.of(
                    "eventType",     event.getEventType(),
                    "aggregateType", event.getAggregateType().name(),
                    "aggregateId",   event.getAggregateId().toString(),
                    "data",          event.getPayload()
            );

            kafkaTemplate.send(topic, key, envelope)
                    .get(5, TimeUnit.SECONDS); // synchronous with 5s timeout

            outboxResultHandler.handleEventPublished(event);
        } catch (Exception e) {
            outboxResultHandler.handleEventFailed(event, e.getMessage());
            // max 3 attempts → FAILED status
        }
    }
}
```

---

## Webhook Retry Queue — Actual Code

Redis Sorted Set as a time-ordered delivery queue. Score = epoch milliseconds of `nextRetryAt`.

```java
// Enqueue a webhook event for delivery at a specific time
public void enqueue(UUID webhookEventId, LocalDateTime retryAt) {
    long score = retryAt.toInstant(ZoneOffset.UTC).toEpochMilli();
    redisTemplate.opsForZSet().add(key, webhookEventId.toString(), score);
}

// Poll events that are due right now
public Set<UUID> pollDue(int limit) {
    long now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    Set<ZSetOperations.TypedTuple<String>> due = redisTemplate
            .opsForZSet().rangeByScoreWithScores(key, 0, now, 0, limit);

    // Remove atomically before processing
    due.forEach(tuple -> redisTemplate.opsForZSet().remove(key, tuple.getValue()));

    return due.stream()
            .map(tuple -> UUID.fromString(tuple.getValue()))
            .collect(Collectors.toSet());
}

// Used by DB reconciler — no duplicate enqueue on crash recovery
public void enqueueIfAbsent(UUID id, LocalDateTime nextRetryAt) {
    redisTemplate.opsForZSet().addIfAbsent(key, id.toString(),
            nextRetryAt.toInstant(ZoneOffset.UTC).toEpochMilli());
}
```

---

## Settlement Engine — Actual Code

One virtual thread per merchant — I/O-bound DB and Feign calls, no thread pool sizing needed.

```java
@Scheduled(cron = "0 0 23 * * *") // every night at 11 PM
public void runScheduled() {
    run();
}

public void run() {
    List<UUID> merchantIds = merchantServiceClient.listActiveMerchantIds();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> futures = merchantIds.stream()
                .map(merchantId -> executor.submit(() ->
                        settlementTransactionExecutor.processForMerchant(merchantId, LocalDate.now())))
                .toList();

        for (Future<?> f : futures) {
            f.get(); // wait for all merchants to complete
        }
    }
}
```

---

## Cross-Service Communication

### Feign Clients

| Caller | Target | Endpoint | Purpose |
|---|---|---|---|
| api-gateway | merchant-service | `GET /internal/api-keys/{keyId}` | API key lookup for auth |
| payment-service | vault-service | `POST /internal/vault/charge` | Charge a tokenized card |
| payment-service | merchant-service | `POST /internal/customers/find-or-create` | Customer upsert on order creation |
| operations-service | merchant-service | `GET /internal/merchants/{id}/webhook-targets` | Active webhook configs |
| operations-service | merchant-service | `GET /internal/merchants/active-ids` | Active merchant IDs for settlement |
| operations-service | merchant-service | `GET /internal/merchants/{id}/settlement-bank-details` | Bank account for transfer |
| operations-service | payment-service | `GET /internal/payments/unsettled-captured` | Payments to settle |
| operations-service | payment-service | `POST /internal/payments/mark-settled` | Mark payments settled |

### Kafka Topics

| Topic | Publisher | Consumer |
|---|---|---|
| `payments.events` | payment-service OutboxPoller | operations-service WebhookKafkaConsumer |
| `orders.events` | payment-service OutboxPoller | operations-service WebhookKafkaConsumer |
| `refund.events` | payment-service OutboxPoller | operations-service WebhookKafkaConsumer |
| `settlement.events` | operations-service OutboxPoller | operations-service WebhookKafkaConsumer |

---

## Common Library Auto-Configurations

`common-lib` is a shared JAR. Beans register via Spring Boot AutoConfiguration — no manual wiring needed in services.

| Configuration Class | Provides |
|---|---|
| `SharedAuditAutoConfiguration` | JPA auditing, `AuditorAwareImpl` |
| `SharedKafkaAutoConfiguration` | `KafkaProperties`, topic resolution |
| `SharedResilienceAutoConfigurations` | `IdempotencyFilter`, rate limiters (fixed window, sliding window, token bucket) |
| `SharedExceptionAutoConfiguration` | `GlobalExceptionHandler`, structured error responses |
| `SharedSecurityAutoConfiguration` | `JwtUtils`, AES encryptor bean, `SignerUtil` |
| `SharedWebFilterAutoConfiguration` | `MerchantContextFilter`, `RequestContextFilter` |

---

## Database — Sensitive Column Storage

| Column | Table | How stored |
|---|---|---|
| `keySecretHash` | `ApiKey` (merchant-db) | BCrypt — irreversible |
| `previousKeySecretHash` | `ApiKey` (merchant-db) | BCrypt — grace period rotation |
| `encryptedPan` | `VaultCard` (vault-db) | AES-256-GCM with per-card DEK |
| `encryptedDek` | `VaultCard` (vault-db) | AES-256-GCM with master key |
| `webhookSecret` | `MerchantWebhookConfig` (merchant-db) | AES-256-GCM — decrypted only at signing time |