# ADR-008: REST API Contract & Controllers

## Context
We need to expose the underlying core business capabilities (inventory, reservation, payment, orders) to frontend clients and other external systems using HTTP APIs.

## Decision
1. **Delegation Model:** Controllers contain absolute zero business logic. They serve strictly as HTTP translation layers that validate inbound JSON against DTO schemas and delegate to existing robust `@Service` components.
2. **Global Exception Handling:** A `@RestControllerAdvice` component centrally intercepts all domain and validation exceptions, mapping them to standard `ErrorResponse` DTOs and uniform HTTP status codes.
3. **Idempotency Strategy:** The API heavily relies on the application's underlying database-backed idempotency keys. Calling `POST /api/reservations` multiple times with the same payload and idempotency key safely yields HTTP 201 with the exact same existing `Reservation` state, completely neutralizing network retry races.
4. **Validation:** Inbound requests are validated via `jakarta.validation` (`@Valid`, `@NotBlank`, `@Positive`). This acts as the first line of defense before the business layer even executes.
5. **OpenAPI Integration:** We adopted `springdoc-openapi` to automatically generate Swagger documentation directly from the Spring Web annotations, ensuring the contract and the code never drift.

## Rationale
- **Controller/Service Separation:** Protects the domain logic from being polluted by web/HTTP concerns. Services remain fully testable without MockMvc.
- **HTTP Status Conventions:** We map `RateLimitExceededException` to `429 Too Many Requests`, `DataIntegrityViolationException` / State Exceptions to `409 Conflict`, missing items to `404 Not Found`, and validation errors to `400 Bad Request`.
- **Webhook Boundary:** Normal REST routes map into standard authentication chains, while the `/api/payments/webhook/razorpay` endpoint uniquely performs its own cryptographic payload verification. This boundary separation prevents a webhook route from accidentally bypassing security checks or normal users spoofing webhook events.
