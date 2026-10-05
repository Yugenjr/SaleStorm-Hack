# Inventory Database Design

The inventory and reservation tables are the most concurrency-critical components of the SALESTORM platform, ensuring that 10,000 concurrent requests for 100 items do not result in overselling.

## 1. INVENTORY Table

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `inventory_id` | UUID | PK | Unique identifier |
| `product_id` | UUID | FK, UNIQUE, INDEX | Reference to Product. One-to-one mapping for concurrency control. |
| `available_quantity` | INT | `>= 0` (CHECK) | Units available to be reserved. Must never drop below 0. |
| `reserved_quantity` | INT | `>= 0` (CHECK) | Units currently held in temporary reservations. |
| `sold_quantity` | INT | `>= 0` (CHECK) | Units successfully paid for and confirmed. |
| `version` | INT | | Optimistic Concurrency Control (OCC) version number. |
| `updated_at` | TIMESTAMP | | Audit timestamp for the last modification. |

### Concurrency Strategy: Optimistic Concurrency Control (OCC) with Check Constraints
- **CHECK Constraint**: The database itself enforces `available_quantity >= 0`. This is a hard guardrail against overselling.
- **Optimistic Locking**: Updates must include `WHERE product_id = ? AND version = ?`. If the update fails (0 rows affected), the application knows another transaction modified the row and can retry or fail.
- **Equation**: Total Stock = `available_quantity` + `reserved_quantity` + `sold_quantity`.

## 2. INVENTORY_RESERVATION Table

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `reservation_id` | UUID | PK | Unique reservation identifier. |
| `product_id` | UUID | FK, INDEX | The reserved product. |
| `customer_id` | UUID | FK, INDEX | The customer holding the reservation. |
| `quantity` | INT | `> 0` | The number of units reserved. |
| `status` | ENUM | | `RESERVED`, `PAYMENT_PENDING`, `CONFIRMED`, `RELEASED`. |
| `idempotency_key`| VARCHAR | UNIQUE | Prevents duplicate reservations from the same customer/request. |
| `expires_at` | TIMESTAMP | INDEX | The exact time this reservation expires if not confirmed. |
| `created_at` | TIMESTAMP | | Audit field. |

### Transaction Boundaries & Constraints
- **Reservation Creation**: Must atomically insert into `INVENTORY_RESERVATION` and decrement `available_quantity` while incrementing `reserved_quantity` in `INVENTORY`. 
- **Unique Constraints**: A unique index on `(idempotency_key)` guarantees that duplicate "Buy Now" clicks from the same user yield the exact same reservation state.
- **Expiry Handling**: A background worker or cron job frequently queries `WHERE status = 'RESERVED' AND expires_at < NOW()`. It atomically updates the status to `RELEASED`, restores `available_quantity`, and decrements `reserved_quantity`.
