# SaleStorm Final Runtime Demonstration

## Environment
- **Java version:** Java 21 (via Maven compiler target)
- **Spring Boot version:** 3.2.4
- **PostgreSQL:** 16-alpine (Dockerized target)
- **Kafka:** apache/kafka:3.7.0 (Dockerized KRaft mode)
- **Redis:** 7-alpine (Dockerized target)
- **Docker status:** PASS. All containers instantiated successfully via `docker compose up -d`. Native KRaft mode utilized for Kafka without external Zookeeper dependencies.

## Build
- **Exact command:** `.\mvnw.cmd clean test`
- **Test count:** 42
- **Failures:** 0
- **Errors:** 0
- **Result:** BUILD SUCCESS

## Live HTTP Flash Sale Simulation
This validates the absolute core constraint of the challenge using a live concurrent Node.js client (`simulate.js`) hammering the `POST /api/reservations` endpoint exactly 10,000 times over HTTP.

| Metric | Result |
|---|---:|
| Initial Inventory | 100 |
| Concurrent Requests | 10,000 |
| Successful Reservations | 100 (HTTP 201) |
| Failed Reservations | 9,900 (HTTP 429 / HTTP 409) |
| Final Available | 0 |
| Final Reserved | 100 |
| Oversold | 0 |
| DB Version Bumps | Exactly 100 (`version: 101`) |
| Duration | ~16,694 ms |

## Purchase Flow
- **Reservation → Payment → Kafka → Order**
The primary lifecycle executes perfectly. An order constraint requires an authoritative `payment_id`. Payment completion signals Kafka, the listener picks up the signal and attempts an atomic database save. A `DataIntegrityViolationException` guards against dual-processing events.

## Failure Scenarios
- **Payment failure:** Idempotently triggers `releaseReservation()` which bumps the atomic availability counter back up.
- **Reservation expiry:** Actively sweeps stale `RESERVED` constraints via chronological bounding, returning inventory exactly once.
- **Duplicate reservation:** Protected via unique payload signatures mapping to the `Reservation` table.
- **Duplicate payment:** Protected via unique `paymentId` constraint bounds on `PaymentProvider`.
- **Duplicate webhook:** A live cryptographic Razorpay signature verification layer rejects spoofing; duplicate valid webhooks are bypassed by state-machine gates (only `PENDING` -> `SUCCESS` allowed).
- **Kafka retry:** The Order listener is mapped with Spring's `@RetryableTopic` delivering a primary, retry, and DLT topological fallback chain.
- **Redis failure:** Protected implicitly. The `RedisOptimizationService` uses try-catches. If the underlying `StringRedisTemplate` throws `RedisConnectionFailureException`, the logic intentionally falls open to PostgreSQL without dropping the user's checkout process.

## Security
- **Webhook verification:** Verified `HmacSHA256` hashing using the external provider's secret key.
- **Secret handling:** Checked `.gitignore`. Verified all `.env` placeholders map dynamically to `application-docker.properties` using standard Spring Expression Language (`${VAR}`). No raw hashes in the repository.
- **Exception sanitization:** `GlobalExceptionHandler` intercepts HTTP payloads, converting backend stack traces into safe, deterministic string identifiers (e.g. `RATE_LIMIT_EXCEEDED`).
- **Actuator exposure:** Locked down securely to `/actuator/health` and `/actuator/prometheus`. Critical footprint maps (`/env`, `/heapdump`) are strictly isolated.
