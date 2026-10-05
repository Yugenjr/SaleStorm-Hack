# SALESTORM Requirements

## Core Problem
The system is a high-scale flash-sale e-commerce platform. It must handle sudden spikes in traffic, specifically an extreme scenario of **10,000 customers simultaneously attempting to purchase a product with only 100 available units**.

## Business Objectives & Guarantees
1. **Never oversell inventory.** (Strict guarantee)
2. Successful reservations must never exceed available inventory.
3. Inventory must never become negative.
4. **Idempotency:** Duplicate requests must not create duplicate reservations or duplicate payments.
5. **Reservation Lifecycle:** Unpaid or failed reservations must eventually expire and safely release inventory back into the available pool.
6. **Payment Reliability:** Payment must be idempotent. Payment failure releases the reservation. Payment timeouts require safe reconciliation.
7. **Order Recovery:** If a payment succeeds but the Order Service is down, the system must durably queue the event and eventually recover to create the order.
8. **Scalability:** The architecture must handle horizontal scaling.

## Functional Requirements
- **Reservation System:** Users can reserve limited inventory for a temporary window.
- **Payment Processing:** Secure and idempotent interaction with an external payment gateway.
- **Order Management:** Creation and lifecycle tracking of confirmed orders.

## Non-Functional Requirements
- **High Concurrency:** Must safely resolve thousands of concurrent requests targeting the same inventory record.
- **Consistency:** Strong consistency at the inventory boundary (no eventual consistency for the absolute inventory count).
- **Resilience:** Graceful handling of downstream service failures (e.g., Order Service, Payment Gateway).

## Key Trade-Offs
- We choose strict consistency over absolute availability for the inventory decrement operation to prevent overselling.
- We choose eventual consistency for Order creation (post-payment) to maximize payment capture and decouple the Order Service.
