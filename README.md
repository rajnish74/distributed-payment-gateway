<div align="center">

# Distributed Payment Gateway

**A production-grade payment gateway built as a microservices system**

Inspired by Razorpay's architecture — built from scratch to understand how real payment infrastructure works at scale.

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.x-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![Kafka](https://img.shields.io/badge/Kafka-KRaft-black?style=flat-square&logo=apachekafka)](https://kafka.apache.org)
[![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)](https://redis.io)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

[Getting Started](#getting-started) • [Architecture](#architecture) • [Services](#services) • [API Reference](docs/api-reference.md) • [Documentation](#documentation)

</div>

---

## Overview

This is the microservices rewrite of a [monolith payment gateway](https://github.com/rajnish74/Payment-Gateway-Integrations-System) I built earlier. The rewrite wasn't just architecture for architecture's sake — the settlement engine (nightly batch), webhook delivery (retry queue, async HTTP), and real-time payment processing have fundamentally different scaling and failure characteristics. Splitting them means they fail independently and scale independently.

**What it implements:**
- Complete payment lifecycle — merchant onboarding → order → payment → capture → settlement
- 14-state payment state machine with immutable audit trail
- PCI-DSS compliant card vault with AES-256-GCM envelope encryption
- Transactional outbox pattern — zero event loss on service failures
- Webhook delivery with exponential backoff (7 attempts, 1min → 24hr)
- Nightly settlement engine with 2% fee + 18% GST calculation
- Rate limiting (fixed window / sliding window / token bucket via Lua scripts)
- Idempotency on all write endpoints via `X-Idempotency-Key`

---

## Services

| Service | Port | Responsibility |
|---|:---:|---|
| `config-service` | `8888` | Centralized config — pulls from GitHub config repo, serves on startup |
| `discovery-service` | `8761` | Eureka — all services register here, Feign resolves URLs through it |
| `api-gateway-service` | `8010` | Single entry point — auth, routing, rate limiting |
| `merchant-service` | `9010` | Merchant auth, API keys, webhooks, customers |
| `payment-service` | `9020` | Orders, payments, state machine, outbox, bank simulator |
| `vault-service` | `9030` | Card tokenization, AES-256-GCM encryption |
| `operations-service` | `9040` | Settlement engine, webhook delivery, DLQ |
| `common-lib` | — | Shared JAR — filters, rate limiting, idempotency, exceptions, utilities |

Each service has its own PostgreSQL database. No shared tables, no cross-database queries.

---

## Architecture

```
                    ┌──────────────────────────────────────┐
                    │          Config Server :8888          │
                    │    ← GitHub config repo (per svc)    │
                    └───────────────┬──────────────────────┘
                                    │ bootstrap
                    ┌───────────────▼──────────────────────┐
                    │         Eureka Discovery :8761        │
                    └───────────────┬──────────────────────┘
                                    │ register
Client ────────────► API Gateway :8010
                          │
                     Auth filter
                     (JWT / API Key → Redis → Feign → BCrypt)
                     Rate limiter (Redis Lua)
                     Idempotency check (Redis)
                          │
          ┌───────────────┼─────────────────┐
          ▼               ▼                 ▼
    merchant-svc     payment-svc       vault-svc
      :9010            :9020            :9030
                          │
                    operations-svc
                       :9040
                    ┌────┴────┐
                 webhook   settlement
               (Redis ZSet) (nightly cron)
```

**Synchronous communication (OpenFeign):**
```
api-gateway      → merchant-service    API key lookup
payment-service  → vault-service       Card charge
payment-service  → merchant-service    Customer findOrCreate
operations-svc   → merchant-service    Webhook configs, merchant bank details
operations-svc   → payment-service     Unsettled payments, mark-settled
```

**Asynchronous (Kafka):**
```
payment-service → Outbox → Kafka (payments / orders / refund / settlement events)
                               ↓
                        operations-service (WebhookKafkaConsumer)
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### Startup order matters

Config server must be up before anything else. Eureka before business services. Gateway last.

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Config server (wait for startup before next step)
cd config-service && mvn spring-boot:run

# 3. Discovery server (wait for :8761 before next step)
cd discovery-service && mvn spring-boot:run

# 4. Business services (any order)
cd merchant-service    && mvn spring-boot:run
cd payment-service     && mvn spring-boot:run
cd vault-service       && mvn spring-boot:run
cd operations-service  && mvn spring-boot:run

# 5. Gateway (last)
cd api-gateway-service && mvn spring-boot:run
```

Verify at http://localhost:8761 — all 5 services should be registered in Eureka.

Full setup guide: [docs/local-setup.md](docs/local-setup.md)

---

## Quick Example

```bash
# Signup
curl -X POST http://localhost:8010/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"businessName":"Acme","email":"dev@acme.com","password":"secret123"}'

# Login → get JWT
curl -X POST http://localhost:8010/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@acme.com","password":"secret123"}'

# Create API key (use JWT from login)
curl -X POST http://localhost:8010/v1/merchants/api-keys \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"environment":"TEST"}'

# Create order (use keyId:secret from above in Basic Auth)
curl -X POST http://localhost:8010/v1/orders \
  -H "Authorization: Basic <base64(keyId:secret)>" \
  -H "Content-Type: application/json" \
  -d '{"amount":{"value":49900,"currency":"INR"},"receipt":"order_001"}'

# Initiate payment
curl -X POST http://localhost:8010/v1/payments \
  -H "Authorization: Basic <base64(keyId:secret)>" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<orderId>","method":"UPI","methodDetails":{"VPA":"test@axis"}}'
```

The bank callback simulator will automatically resolve the payment within a few seconds.

---

## Key Design Decisions

**Transactional Outbox** — events are saved in the same DB transaction as the business entity, published to Kafka asynchronously. Eliminates the dual-write problem — no events lost even if Kafka is temporarily unavailable.

**Pessimistic locking** — concurrent payment requests on the same order both pass the status check without locking. Added `@Lock(PESSIMISTIC_WRITE)` on order/payment queries — first request locks the row, second waits and fails cleanly.

**AES-256-GCM envelope encryption** — each card gets its own randomly generated DEK. PAN is encrypted with the DEK. DEK is encrypted with the master key. Master key never touches the database. Raw PAN bytes are zeroed from JVM memory after use.

**Redis ZSet for webhook retry queue** — score is the epoch milliseconds of next retry time. `rangeByScore(0, now)` gives exactly what's due. DB reconciler re-enqueues anything missed on crash.

**Virtual threads** — both webhook delivery and settlement use `Executors.newVirtualThreadPerTaskExecutor()`. Both are I/O-bound workloads — waiting on HTTP responses and DB queries. No thread pool sizing needed.

Full reasoning: [docs/design-decisions.md](docs/design-decisions.md)

---

## Documentation

| | |
|---|---|
| [Architecture](docs/architecture.md) | System design, service map, request flows, Feign call graph |
| [Payment Lifecycle](docs/payment-lifecycle.md) | All 14 state machine transitions, bank callback simulator |
| [Security](docs/security.md) | Gateway auth, API key caching, rate limiting strategies, card encryption |
| [Webhook Delivery](docs/webhook-delivery.md) | Kafka → Redis ZSet → virtual threads → exponential backoff → DLQ |
| [Settlement](docs/settlement.md) | Nightly engine, fee calculation, bank transfer flow |
| [Design Decisions](docs/design-decisions.md) | Why outbox pattern, pessimistic locking, virtual threads, separate DBs |
| [API Reference](docs/api-reference.md) | All endpoints with request/response examples |
| [Local Setup](docs/local-setup.md) | Running locally with Docker Compose |

---

## Related

**v1 — Monolith:** https://github.com/rajnish74/Payment-Gateway-Integrations-System

The same domain built as a single Spring Boot application first. The monolith is complete and fully working. This repo is the microservices rewrite.

---

<div align="center">

Built by [Rajnish Kumar](https://github.com/rajnish74) · [LinkedIn](https://www.linkedin.com/in/rajnish-kumar-74/)

</div>