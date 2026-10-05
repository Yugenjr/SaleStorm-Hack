# ADR-005: Kafka Event Transport for Payment-to-Order

## Context
As the `PaymentService` successfully captures a charge, the `OrderService` must reliably create a corresponding order. We previously built this locally using an `InMemoryEventPublisher`. However, for production-grade reliability across distributed nodes, we must ensure events are not lost if a server crashes.

## Decision
We chose **Kafka** as the asynchronous event transport for this workflow.

## Rationale
- **Why Kafka?** High throughput, durability, and replayability. Kafka acts as an immutable log.
- **Why Asynchronous Events?** Decoupling payment success from order creation prevents cascading failures. If the Order DB is down, payments can still succeed and the events will safely queue.
- **At-Least-Once Delivery**: Kafka guarantees the message will be delivered at least once. We rely on the `OrderService`'s database-level idempotency (the `paymentId` unique constraint) to safely ignore duplicate events.
- **Retry and DLT**: We configured Spring Kafka's `@RetryableTopic` to automatically retry failing events 3 times with exponential backoff before sending them to a Dead Letter Topic (`sales.payment.events.DLT`).
- **Why `InMemoryEventPublisher` remains**: Kafka integration tests require starting a broker (or embedded Kafka), which slows down standard unit test pipelines. We conditionalized the Kafka publisher (`@ConditionalOnProperty("app.kafka.enabled")`) so the core test suite (the 19 existing tests) runs instantly in memory, while production runs on Kafka.
