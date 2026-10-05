# Order Database Design

The Order model represents the final, confirmed state of a successful purchase. It transitions through a strict state machine.

## 1. ORDER Table

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `order_id` | UUID | PK | Unique identifier for the order. |
| `customer_id` | UUID | FK, INDEX | The purchasing user. |
| `status` | ENUM | | `CREATED`, `PAYMENT_PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`. |
| `total_amount` | DECIMAL | | Total value of the order. |
| `created_at` | TIMESTAMP | | Audit timestamp. |
| `updated_at` | TIMESTAMP | | Last status update. |

## 2. ORDER_ITEM Table

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `order_item_id` | UUID | PK | Unique identifier for the line item. |
| `order_id` | UUID | FK | Reference to the parent order. |
| `product_id` | UUID | FK | The purchased product. |
| `quantity` | INT | `> 0` | Quantity purchased. |
| `price_at_purchase`| DECIMAL | | Snapshot of the price to prevent historical changes from affecting completed orders. |

### Constraints & State Management
- **State Machine Transitions**: Applications must validate state changes. For example, an order can only move to `CONFIRMED` if it is currently in `PAYMENT_PENDING` and a `PaymentSucceeded` event is processed.
- **Payment Relationship**: An order has a one-to-many relationship with payment attempts (since a payment might fail and be retried), but only one `SUCCESS` payment.
- **Order Failure Recovery**: If a payment succeeds but the Order Service fails to update the state to `CONFIRMED`, an asynchronous reconciliation process (reading the `PaymentSucceeded` event) will eventually update the order, providing eventual consistency.
