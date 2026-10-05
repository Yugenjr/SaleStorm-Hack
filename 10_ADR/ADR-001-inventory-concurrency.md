# ADR-001: Inventory Concurrency Mechanism

## Context
The SALESTORM platform must handle 10,000 concurrent requests competing for 100 units of inventory. We need a mechanism to prevent overselling while maintaining high throughput.

## Options Considered

### 1. Pessimistic Locking (`SELECT ... FOR UPDATE`)
- **Description:** A transaction locks the inventory row when reading it, preventing any other transaction from reading or updating it until the first transaction commits.
- **Pros:** Conceptually simple; guarantees exact consistency.
- **Cons:** High contention. With 10,000 concurrent requests, the database will experience severe lock contention, resulting in high latency, connection pool exhaustion, and potential deadlocks. Lock duration spans the entire transaction.

### 2. Atomic Conditional Update (Optimistic Approach)
- **Description:** Execute a single `UPDATE` statement with a `WHERE` clause that checks if sufficient inventory exists before decrementing.
- **Pros:** No explicit read locks held across the transaction. The database only locks the row for the exact duration of the `UPDATE` execution. Extremely high throughput. Automatically rejects requests when inventory is depleted.
- **Cons:** Requests that fail the condition return 0 updated rows, which the application must handle (which is standard behavior for "sold out").

### 3. Redis Distributed Locks / Redis Lua Scripts
- **Description:** Use Redis as the primary inventory decrement engine via single-threaded Lua scripts, then asynchronously sync to PostgreSQL.
- **Pros:** Highest possible throughput since everything is in memory.
- **Cons:** Risk of data loss if Redis crashes before syncing to Postgres. Adds complexity of maintaining two sources of truth.

## Decision
**We chose Option 2: Atomic Conditional Update backed by PostgreSQL.**

## Rationale
- **Correctness:** PostgreSQL's ACID guarantees ensure that the conditional update is strictly serialized and perfectly accurate. 
- **Contention & Scalability:** It avoids the long-lived lock contention of pessimistic locking. It gracefully handles the 10,000 request spike by quickly rejecting requests once the condition `available_quantity >= quantity` fails.
- **Operational Complexity:** Keeps the architecture simple. Redis remains an optional cache rather than a critical point of failure for transactional state.
