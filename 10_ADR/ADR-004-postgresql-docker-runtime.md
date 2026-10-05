# ADR-004: PostgreSQL & Docker Runtime Infrastructure

## Context
As we transition from local prototyping to a reproducible production-like environment, we need a robust, persistent, and horizontally scalable relational database. We also need a repeatable way to package and run the application that minimizes "it works on my machine" issues.

## Decision
We chose to adopt:
- **PostgreSQL** as the authoritative source of truth for transactional state.
- **Docker & Docker Compose** to containerize both the application and the database.
- **H2 (In-Memory)** remains exclusively for rapid execution of the unit and integration test suite (`mvn test`).

## Rationale
- **Why PostgreSQL**: The core inventory consistency mechanism relies on atomic conditional updates (`UPDATE ... WHERE status = ...`). PostgreSQL provides robust MVCC (Multi-Version Concurrency Control) and strict ACID guarantees essential for preventing overselling in high-concurrency flash sales.
- **Why Docker**: Dockerizing the Spring Boot app using a multi-stage `Dockerfile` (Java 21 JRE, running as a non-root user) ensures a secure, minimal runtime footprint. Docker Compose orchestrates the dependencies, ensuring PostgreSQL is fully healthy before the application attempts to connect.
- **Why H2 remains for tests**: Recreating a PostgreSQL container for every unit test slows down the feedback loop. By utilizing Spring profiles, `application.properties` defaults to H2, keeping tests fast, while `application-docker.properties` targets PostgreSQL when the container starts.
- **Configuration Strategy**: We externalized all credentials into environment variables (`.env`). The `docker-compose.yml` mounts a local persistent volume for PostgreSQL so that state survives container restarts, completely decoupling application compute from persistence.
