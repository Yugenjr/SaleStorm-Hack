# Asynchronous Event Design

To decouple services, improve reliability, and handle failures gracefully (e.g., Order service failing after Payment succeeds), the architecture utilizes asynchronous event-driven communication (e.g., via Kafka or RabbitMQ).

| Event Name | Producer | Consumer | Purpose | Idempotent Processing Required? |
| :--- | :--- | :--- | :--- | :--- |
| `ReservationCreated` | Inventory Service | Order Service, Notification Service | Pre-creates the Order in `PAYMENT_PENDING` state and informs the user they have a limited time to pay. | Yes |
| `ReservationExpired` | Inventory Service | Order Service, Notification Service | Triggers cancellation of the Order and notifies the customer that their cart has expired. | Yes |
| `PaymentSucceeded` | Payment Service | Order Service, Inventory Service | Moves Order to `CONFIRMED`. Instructs Inventory to move `reserved_quantity` to `sold_quantity`. | **Yes**. Crucial to avoid double-confirming or double-deducting. |
| `PaymentFailed` | Payment Service | Order Service, Inventory Service | Moves Order to `CANCELLED`. Instructs Inventory to release reservation back to `available_quantity`. | Yes |
| `OrderCreated` | Order Service | Notification Service | Sends initial confirmation to the customer. | Yes |
| `OrderConfirmed` | Order Service | Fulfilment / Shipment Service | Triggers warehouse packing process once payment is fully secured. | Yes |
| `ShipmentCreated` | Shipment Service | Order Service, Notification Service | Updates Order to `SHIPPED` and provides tracking info to the customer. | Yes |

## Event Processing Guarantees
- **At-Least-Once Delivery**: Message brokers guarantee delivery. Consumers must implement idempotency (e.g., checking if `order_id` is already `CONFIRMED` before processing a `PaymentSucceeded` event).
- **Outbox Pattern**: Producers (like the Payment Service) must use the Transactional Outbox pattern to write the `PaymentSucceeded` event to the database in the same transaction as the payment status update, ensuring events are never lost if the producer crashes before publishing to the broker.
