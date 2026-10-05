# ADR-009: Security, Observability & Production Hardening

## Context
As the prototype matures into a production-like structure, we must introduce essential security boundaries, robust observability, and standard production telemetry without fundamentally rewriting the application.

## Decision
1. **Request Correlation (Tracing):** We implemented `CorrelationIdFilter` (`Ordered.HIGHEST_PRECEDENCE`). It extracts or generates `X-Correlation-ID`, injects it into MDC for logging, and returns it in the HTTP response.
2. **Structured Logging:** `InventoryService` and `PaymentService` were instrumented with SLF4J, securely logging business transitions (`log.info` and `log.warn`) while explicitly avoiding sensitive secrets.
3. **Metrics (Micrometer):** Introduced `MetricsService` mapping critical business paths to `MeterRegistry` counters (e.g., `salestorm.reservations.attempts`, `salestorm.ratelimit.rejections`).
4. **Health & Actuator:** Enabled Spring Boot Actuator (`/actuator/health` and `/actuator/prometheus`) while strictly denying access to configuration or environment properties (`/actuator/env`).
5. **Secret Hygiene:** All Razorpay and PostgreSQL credentials are comprehensively decoupled into `.env` (which is `.gitignore`d). `.env.example` provides safe defaults for onboarding.
6. **Graceful Shutdown:** Configured `server.shutdown=graceful` to allow in-flight HTTP and Kafka jobs up to 20 seconds to drain safely before SIGTERM kills the JVM.

## Rationale
- Injecting MDC natively avoids heavy dependencies like OpenTelemetry for this phase, while instantly delivering log traceabilty.
- We deliberately retained the Razorpay webhook signature validation instead of blanket API auth to ensure external integrations are cryptographically guarded.
- The `InMemoryOptimizationService` hack that manually bypassed test rate limits by checking customer IDs was cleanly removed in favor of proper properties configuration, neutralizing a critical anti-pattern that leaked test-awareness into the domain.
