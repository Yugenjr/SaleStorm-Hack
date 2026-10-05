# ADR-006: Redis for Cache and Rate Limiting

## Context
During flash sales, the `InventoryService` experiences tremendous read volume (checking if items are available) and write spikes (attempting reservations). If all 10,000 concurrent users hit the PostgreSQL database for every request, the connection pool will instantly exhaust, leading to complete system failure.

## Decision
We chose **Redis** as a caching and rate-limiting optimization layer in front of the core transactional logic.

## Rationale
- **Why Redis?** Single-threaded event loop ensures atomic counter increments (perfect for rate limiting). In-memory speed makes it ideal for offloading read-heavy inventory checks.
- **Cache Strategy:** We implemented a read-aside cache on `InventoryService.getInventory()`. If a cache miss occurs, we fetch from PostgreSQL and populate Redis with a short 60-second TTL.
- **Invalidation:** When a reservation is successfully made (or released/expired), the inventory changes durably in PostgreSQL. Immediately after, we explicitly invalidate the Redis cache so subsequent reads see the updated count.
- **Hot-Sale Protection (Rate Limiting):** A simple distributed counter tracks reservation attempts per customer (`rate_limit:customerId`). If they exceed 5 attempts per minute, we fail fast with a `RateLimitExceededException` (HTTP 429), dropping the load long before it ever touches PostgreSQL.
- **Failure Behavior (Fail Open):** Redis is strictly an optimization. If the Redis server crashes or the network partitions, the `RedisOptimizationService` deliberately suppresses the exceptions and falls back to PostgreSQL. Cache reads return `null` (forcing DB reads), and rate limits return `true` (allowing the request through). **PostgreSQL remains the absolute source of truth and its atomic `UPDATE` guarantees prevent overselling, even if Redis is completely dead.**
