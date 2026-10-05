# Database Architecture Trade-offs

For the SALESTORM flash sale, selecting the right database architecture requires balancing strict consistency (no overselling) with high throughput (10,000 concurrent requests).

## Chosen Architecture: Relational Database (PostgreSQL) for Core Transactions
We select a robust relational database (e.g., PostgreSQL) as the source of truth for Inventory, Orders, and Payments.

### Why SQL over NoSQL?
- **Strict ACID Guarantees**: Relational databases provide strong isolation and consistency natively. Using `CHECK (available_quantity >= 0)` ensures mathematical certainty that overselling is impossible at the database layer.
- **Row-Level Locking**: Optimistic Concurrency Control (OCC) and `SELECT ... FOR UPDATE` are heavily optimized in RDBMS to handle concurrency safely without the eventual consistency risks of many NoSQL stores.

### Trade-offs Made
1. **Consistency over Availability**: Under extreme load, database contention on a single `inventory` row (the flash sale item) will cause requests to fail or queue. We intentionally sacrifice immediate availability (some users will see "Please try again") to guarantee strict consistency (we never sell 101 items).
2. **Database Simplicity vs. Horizontal Scalability**: While a NoSQL database (like Cassandra or DynamoDB) scales horizontally more easily, it complicates the transaction boundaries needed for atomic inventory deduction. We trade the ease of horizontal scaling for the safety of RDBMS ACID transactions.
3. **Synchronous DB vs Asynchronous Queues**: To handle 10,000 requests, we front the RDBMS with an asynchronous message queue (e.g., Kafka or Redis) or API Gateway throttling. The DB acts as the strict serializable bottleneck by design, processing requests from the queue at a sustainable rate.

## Conclusion
The guarantee that "100 available units cannot result in more than 100 successful sales" is the paramount success criterion. A SQL database with strict constraints and OCC provides this guarantee unconditionally.
