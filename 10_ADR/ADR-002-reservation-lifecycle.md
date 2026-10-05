# ADR-002: Reservation Lifecycle and Atomic Inventory Restoration

## Context
When a reservation is created, inventory is locked. This inventory must eventually be converted to a confirmed order or released back to the available pool. During release or expiry, we face concurrency risks: a duplicate webhook from a payment gateway, a scheduled job triggering expiry, and a user canceling an order could all theoretically attempt to release the *same* reservation at the identical millisecond.

If multiple threads succeed in releasing the same reservation, the inventory could be restored multiple times (e.g., 100 available goes to 101 or 102), breaking the business integrity.

## Options Considered

### 1. Lock the Reservation Row (Pessimistic Locking)
- Use `SELECT ... FOR UPDATE` when loading the reservation. 
- *Cons*: Reduced throughput, increased deadlock risk, heavier load on the DB.

### 2. State-Based Atomic Conditional Update (Optimistic Approach)
- Instead of explicitly locking, use an atomic database statement that updates the status *only if* it is in the expected state.
- `UPDATE Reservation SET status = 'RELEASED' WHERE id = :id AND status = 'RESERVED'`

## Decision
We chose **Option 2: State-Based Atomic Conditional Update**.

## Rationale
- **Absolute Correctness without Locks**: Only one thread can possibly execute the SQL update successfully (it returns `1` row affected). All other concurrent attempts will return `0` rows affected.
- **Tied to Inventory Restoration**: We strictly gate the inventory restoration behind the success of the reservation status update. If the status update returns `1`, we restore the inventory exactly once. If it returns `0`, we do nothing.
- **Race Condition Immunity**: This protects against user cancellation, duplicate payment failures, and the background expiry CRON job all competing at once.
