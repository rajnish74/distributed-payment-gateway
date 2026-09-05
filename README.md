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
[![Prometheus](https://img.shields.io/badge/Prometheus-metrics-E6522C?style=flat-square&logo=prometheus)](https://prometheus.io)
[![Grafana](https://img.shields.io/badge/Grafana-dashboards-F46800?style=flat-square&logo=grafana)](https://grafana.com)
[![Zipkin](https://img.shields.io/badge/Zipkin-tracing-FFA500?style=flat-square)](https://zipkin.io)

[Getting Started](#getting-started) • [Architecture](#architecture) • [Services](#services) • [Observability](#observability) • [API Reference](docs/api-reference.md) • [Documentation](#documentation)

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
- Full observability — distributed tracing (Zipkin), metrics (Prometheus), dashboards (Grafana)

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
          ┌───────────────┼──────────────┬────────────────┐
          ▼               ▼              ▼                ▼
    merchant-svc     payment-svc    vault-svc     operations-svc
      :9010            :9020          :9030            :9040
                          │                        ┌────┴────┐
                    Outbox → Kafka            webhook     settlement
                                           (Redis ZSet) (nightly cron)

Observability (all services):
  → Micrometer → Prometheus :9090 → Grafana :3000
  → Spring Cloud Sleuth → Zipkin :9411
```

**Synchronous (OpenFeign):**
```
api-gateway      → merchant-service    API key lookup
payment-service  → vault-service       Card charge
payment-service  → merchant-service    Customer findOrCreate
operations-svc   → merchant-service    Webhook configs, bank details
operations-svc   → payment-service     Unsettled payments, mark-settled
```

**Asynchronous (Kafka):**
```
payment-service → Outbox → Kafka (payments / orders / refund / settlement events)
                               ↓
                        operations-service (WebhookKafkaConsumer)
```

---

## Observability

Every service exposes metrics, traces, and logs. No manual instrumentation needed — Micrometer and Spring Cloud Sleuth wire everything automatically via `spring-boot-starter-actuator`.

### Distributed Tracing — Zipkin

Every request gets a `traceId` that follows it across service boundaries. Feign calls, Kafka messages, and DB queries are all captured.

```
http://localhost:9411
```

Find a payment by its ID → see the full trace:
```
API Gateway → payment-service → vault-service → Kafka → operations-service
```

### Metrics — Prometheus + Grafana

Prometheus scrapes `/actuator/prometheus` from all 5 services every 5 seconds:

| Service | Scrape target |
|---|---|
| api-gateway-service | `host.docker.internal:8010` |
| merchant-service | `host.docker.internal:9010` |
| payment-service | `host.docker.internal:9020` |
| vault-service | `host.docker.internal:9030` |
| operations-service | `host.docker.internal:9040` |

**Grafana dashboards:** `http://localhost:3000` (admin/admin)

Datasource is auto-provisioned — Prometheus is connected automatically on startup. Dashboards are loaded from `provisioning/dashboards/`.

**Key metrics available:**
- HTTP request rate, latency, error rate per service
- JVM memory, GC, thread pool stats
- Kafka consumer lag, publish rate
- Custom payment metrics — payment initiation rate, capture rate, failure rate
- Settlement processing time per merchant

### Structured Logging

All services use SLF4J with MDC — `traceId` and `spanId` are injected into every log line automatically. Correlate logs with Zipkin traces using the traceId.

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 1. Start infrastructure + observability stack

```bash
docker compose up -d
```

This starts: PostgreSQL, Redis, Kafka, Prometheus, Grafana, Zipkin.

Verify:
- Eureka: http://localhost:8761
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411

### 2. Start services in order

```bash
# Config server first
cd config-service && mvn spring-boot:run

# Discovery server second
cd discovery-service && mvn spring-boot:run

# Business services (any order)
cd merchant-service    && mvn spring-boot:run
cd payment-service     && mvn spring-boot:run
cd vault-service       && mvn spring-boot:run
cd operations-service  && mvn spring-boot:run

# Gateway last
cd api-gateway-service && mvn spring-boot:run
```

All 5 services should appear in Eureka at http://localhost:8761 within 30 seconds.

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

# Create API key
curl -X POST http://localhost:8010/v1/merchants/api-keys \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"environment":"TEST"}'

# Create order
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

The bank callback simulator resolves the payment automatically within a few seconds. Check Zipkin for the full trace.

---

## Key Design Decisions

**Transactional Outbox** — events saved in the same DB transaction as the business entity, published to Kafka asynchronously. No events lost even if Kafka is temporarily unavailable.

**Pessimistic locking** — `@Lock(PESSIMISTIC_WRITE)` on order/payment queries prevents concurrent requests from creating duplicate payments on the same order.

**AES-256-GCM envelope encryption** — each card gets its own DEK. PAN encrypted with DEK, DEK encrypted with master key. Master key never touches the database. Raw PAN bytes zeroed from JVM memory after use.

**Redis ZSet for webhook retry queue** — score is epoch ms of next retry time. `rangeByScore(0, now)` gives exactly what's due. DB reconciler re-enqueues anything missed on crash.

**Virtual threads** — webhook delivery and settlement both use `Executors.newVirtualThreadPerTaskExecutor()`. I/O-bound workloads — no thread pool sizing needed.

Full reasoning: [docs/design-decisions.md](docs/design-decisions.md)

---

## Documentation

| | |
|---|---|
| [Architecture](docs/architecture.md) | System design, service map, request flows, Feign call graph, sequence diagrams |
| [Payment Lifecycle](docs/payment-lifecycle.md) | All 14 state machine transitions, bank callback simulator |
| [Security](docs/security.md) | Gateway auth, API key caching, rate limiting, card encryption |
| [Webhook Delivery](docs/webhook-delivery.md) | Kafka → Redis ZSet → virtual threads → exponential backoff → DLQ |
| [Settlement](docs/settlement.md) | Nightly engine, fee calculation, bank transfer flow |
| [Observability](docs/observability.md) | Zipkin tracing, Prometheus metrics, Grafana dashboards |
| [Design Decisions](docs/design-decisions.md) | Why outbox pattern, pessimistic locking, virtual threads, separate DBs |
| [API Reference](docs/api-reference.md) | All endpoints with request/response examples |
| [Local Setup](docs/local-setup.md) | Running locally with Docker Compose |

---

## Related

**v1 — Monolith:** https://github.com/rajnish74/Payment-Gateway-Integrations-System — same domain, single deployable unit. Built first to understand the business logic before splitting into services.

---

<div align="center">

Built by [Rajnish Kumar](https://github.com/rajnish74) · [LinkedIn](https://www.linkedin.com/in/rajnish-kumar-74/)

</div>