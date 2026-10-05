# ADR-007: Razorpay Payment Provider Integration

## Context
The application needs to integrate with a live payment gateway (Razorpay) for production use, but we must strictly avoid breaking our isolated automated tests or making live HTTP calls during CI/CD. Furthermore, we must securely handle asynchronous payment status updates (webhooks) without compromising internal database idempotency.

## Decision
1. **Provider Abstraction:** We extended the `PaymentProvider` interface to support two distinct implementations: `MockPaymentProvider` and `RazorpayPaymentProvider`.
2. **Conditional Loading:** Spring's `@ConditionalOnProperty(name="app.payment.provider")` strictly controls which provider is loaded at runtime. By default, test environments use `mock`.
3. **Webhook Controller:** We added an isolated `RazorpayWebhookController` that intercepts Razorpay server-to-server events.
4. **Signature Verification:** We use the official `razorpay-java` SDK specifically to utilize `Utils.verifyWebhookSignature()`, ensuring all inbound webhooks are cryptographically authenticated against our `RAZORPAY_WEBHOOK_SECRET`.
5. **Idempotency Strategy:** Webhook idempotency fundamentally relies on the authoritative database state of the `Payment` entity. If a payment transitions from `PENDING` to `SUCCESS`, any subsequent duplicate webhooks for the same payment are fast-rejected.

## Rationale & Security
- **No Domain Leakage:** Razorpay SDK types (like `Order` or `Transaction`) never leak into `PaymentService` or domain entities. They are immediately normalized into our standard `PaymentResult` DTO.
- **Credential Management:** Secrets (`RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`) are rigorously externalized via `.env`. They are **never** hardcoded.
- **State Transition Rules:** We do not allow a `SUCCESS` payment to transition to `FAILED` silently. The `reconcilePaymentWithWebhook` method strictly guards this state machine logic using atomic transactional updates.
- **Testing:** The integration test suite mocks the Razorpay client to test the adapter logic and directly tests the signature validation edge cases without generating actual network requests.
