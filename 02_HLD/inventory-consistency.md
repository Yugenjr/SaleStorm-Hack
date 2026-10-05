# Inventory Consistency 

## The Concurrency Challenge
When 10,000 customers try to purchase 100 available units at the exact same moment, the system is exposed to extreme race conditions. If read and write operations are not strictly serialized or guarded, multiple threads may read `available_quantity = 1` and all successfully decrement it, leading to a negative balance (overselling).

## The Solution: Atomic Conditional Updates
The most critical design decision in SALESTORM is that **inventory consistency is guaranteed at the PostgreSQL transaction boundary**.

We achieve this via an atomic conditional inventory update approach (a form of optimistic concurrency). 

The SQL statement conceptually looks like this:
```sql
UPDATE inventory
SET
    available_quantity = available_quantity - :quantity,
    reserved_quantity = reserved_quantity + :quantity,
    version = version + 1
WHERE product_id = :product_id
  AND available_quantity >= :quantity;
```

### Why this works:
1. **Atomicity**: Relational databases (like PostgreSQL) execute this `UPDATE` atomically. 
2. **Conditionality**: The `WHERE available_quantity >= :quantity` clause ensures that the decrement only succeeds if there is enough stock. 
3. **Database Locks**: While executing the `UPDATE`, the database acquires a brief row-level lock. If 10,000 requests hit the database, they are serialized at the row level. Only the first 100 requests will find the `WHERE` condition to be true. 

### Outcome
- If exactly one row is affected: The reservation succeeds.
- If zero rows are affected: The reservation fails (sold out or insufficient stock).

Redis is **not** used as the authoritative source of inventory because it is prone to data loss during failovers or race conditions unless complex distributed locks are used (which reduce throughput and increase complexity). PostgreSQL provides robust ACID guarantees right out of the box.
