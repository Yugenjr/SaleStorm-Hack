# ADR-003: Payment to Order Event Recovery

## Context
When a payment is successfully processed, the system must create a corresponding order. A critical failure scenario occurs if the `OrderService` crashes or is unavailable at the exact moment the `PaymentService` successfully captures funds. We must ensure the customer's order is not lost.

## Mechanism

We chose an **Asynchronous Event-Driven Architecture** for this transition.
- **PaymentService** publishes a `PaymentSucceeded` event immediately after persisting the successful payment.
- **OrderService** consumes this event asynchronously.

### Prototype Implementation
For this 3-hour hackathon prototype, we simulate this pattern using an `InMemoryEventPublisher` and an internal `ConcurrentLinkedQueue` inside `OrderService` to act as a Dead Letter Queue (DLQ). If `OrderService` encounters a failure while processing an event, the event is safely routed to the DLQ where it can be reprocessed later via `processDeadLetterQueue()`.

### Production Implementation
In a real production environment, the in-memory bus must be replaced by a **Durable Message Broker** (like Kafka or RabbitMQ).
- **At-Least-Once Delivery**: The broker guarantees the message remains durable until the `OrderService` successfully processes it and acknowledges it.
- **Consumer Retry**: If `OrderService` fails to process a message, it will NACK it, and the broker will redeliver it later.
- **Dead Letter Queue (DLQ)**: Repeated unprocessable events will eventually route to a Kafka DLQ topic for manual engineering intervention.

## Idempotency
Because the broker provides *At-Least-Once* (not exactly-once) delivery, the `OrderService` must be fully idempotent. 
We guarantee this at the database level: the `Order` table has a unique constraint on `paymentId`. If the `OrderService` receives a duplicate `PaymentSucceeded` event, it will catch the database `DataIntegrityViolationException` and gracefully return the existing order, completely eliminating the risk of duplicate orders.
