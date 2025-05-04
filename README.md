# Amlume Shop

[![License: Proprietary](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE) <!-- Adjust if license changes -->

UNDER DEVELOPMENT (see dev branch) and testing techniques

## Overview

This project is the Spring Boot backend application for Amlume Shop. It provides RESTful APIs with a strong focus on security using PASETO tokens, efficient caching, resilience, rate limiting leveraging Valkey/Redis, and observability through Micrometer metrics.

## Features

*   **Robust Security:**
    *   Keycloak OATH2 and MFA for user authentication and authorization.
    *   Secrets management using Spring Vault, HashiCorp Vault with fallback retrieval.
    *   PASETO v4 token-based authentication (Stateless).
    *   Support for both Public (signed) and Local (encrypted) Access Tokens.
    *   Secure Refresh Token mechanism with database persistence and hashing.
    *   JTI (JWT ID) validation using Redis/Valkey to prevent token replay attacks.
    *   Token Revocation mechanism with Redis/Valkey caching.
    *   Detailed token claim validation (expiration, issuer, audience, subject, etc.).
    *   Support for multiple token types (Access, Refresh, etc.) with different expiration policies.
*   **Resilience:**
    *   Redis/Valkey-based Rate Limiting using an efficient Lua script (Sliding Window counter algorithm).
*   **Performance:**
    *   Redis/Valkey caching for user, category, products, token claims, revocation status, etc.
*   **Observability:**
    *   Integrated Micrometer metrics for monitoring token generation, validation, and potentially other application aspects.
    *   Logging with SLF4J and Logback. Data sent to Loki via Promtail container.
    *   Integration with Spring Boot Actuator for health checks and metrics.
    *   Health checks and readiness/liveness probes for Kubernetes (probably in the parallel project geared towards Kubernetes).
*   **User Management:** (Implied) Role based authentication/authorization using Keycloak OATH2 and MFA.
*   **Configuration:** Flexible configuration via Spring Boot profiles (`application.yml`/`properties`).

## Technologies Used

*   **Language:** Java 21+
*   **Framework:** Spring Boot 3.x
    *   Spring Web (MVC)
    *   Spring Security
    *   Spring Data JPA
    *   Spring Data Redis (Lettuce Client)
    *   Spring Boot Actuator
    *   Spring Boot Starter for Valkey/Redis
    *   Spring Vault
    *   Spring Cache Abstraction
    *   Spring Scheduling (`@Scheduled`)
*   **Database:** Relational Database (e.g., PostgreSQL, MySQL - Requires configuration)
*   **Cache/In-Memory Data Grid:** Valkey / Redis
*   **Security Tokens:** PASETO v4 (via `paseto4j` library)
*   **Metrics:** Micrometer, Loki, Prometheus, Promtail, Grafana
*   **Build Tool:** Maven
*   **Utilities:** MapStruct, JUnit 5, Mockito, AssertJ, Testcontainers (for testing)

## Prerequisites

*   **JDK:** Version 21 or higher
*   **Build Tool:** Maven 3.8+ or Gradle 7.x+
*   **Valkey / Redis:** A running instance accessible by the application.
*   **Database:** A running instance of a supported relational database (e.g., PostgreSQL 13+).
*   **(Optional) Docker & Docker Compose:** For easily running dependencies locally.

## Getting Started

1.  **Clone the repository:**

2.  **Configure Application:**
    *   Copy `src/main/resources/application.yml.example` (or `.properties`) to `src/main/resources/application.yml`.
    *   Update `application.yml` with your specific settings:
        *   **Valkey/Redis Connection:** `valkey.host`, `valkey.port`, `valkey.password`.
        *   **Database Connection:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`.
        *   **PASETO Keys:** Configure paths or values for `paseto.access.public/private`, `paseto.access.secret`, `paseto.refresh.secret` and associated Key IDs (`kid`). **Ensure these are kept secure and are not committed to version control if they contain sensitive key material.**
        *   **Cache TTLs:** Review `cache.*.ttl-seconds` values if needed.
        *   **Rate Limiting:** Configure parameters if exposed via `application.yml`.
        *   **(Optional) JTI Settings:** Configure Bloom Filter size/error rate (`jti.bloomfilter.*`) and cleanup rate (`jti.cleanup.rate-ms`) if using the Redisson-based JTI service (Note: The latest context suggests a simpler Redis TTL approach might be used now).

3.  **Build the project:**
    *   **Maven:**

```Bash
mvn clean install
```

## Running the Application

*   **Using Maven:**

```Bash
mvn spring-boot:run
```

Running the JAR:**

```Bash
java -jar target/amlume-shop-[version].jar
```

*(Adjust JAR name based on build output)*

The application will start, connect to Valkey/Redis and the database, and be available (typically on port 8080, unless configured otherwise).

## Configuration Reference

Key configuration properties are typically found in `src/main/resources/application.yml` (or `.properties`). Pay close attention to:

*   `server.port`
*   `spring.datasource.*` (Database connection)
*   `valkey.*` (Valkey/Redis connection)
*   `paseto.*` (PASETO key configuration and Key IDs)
*   `cache.*` (Cache Time-To-Live settings)
*   `logging.level.*` (Logging configuration)
*   Rate limiting parameters (e.g., window size, max requests - check `TokenConfigurationService` or similar)
*   JTI service parameters (if applicable)

## API Documentation

Documentation will be available in HTML format, using the official swagger-ui jars.

The Swagger UI page will then be available at http://server:port/context-path/swagger-ui.html and the OpenAPI description will be available at the following url for json format: http://server:port/context-path/v3/api-docs

*   **server:** The server name or IP

*   **port:** The server port

*   **context-path:** The context path of the application

Documentation can be available in yaml format as well, on the following path : /v3/api-docs.yaml

## Testing

Run the test suite using your build tool:

*   **Maven:**

```Bash
mvn test
```

Integration tests might require running instances of Valkey/Redis and the database. Consider using Testcontainers for managing dependencies during testing.

## Kubernetes Configurations

This directory contains Kubernetes configurations for the amlume-shop application.

### Structure
- `configmap.yml`: Application configuration
- `deployment.yml`: Deployment configuration
- `service.yml`: Service configuration
- `secret.yml`: Sensitive data configuration

### Usage
Apply configurations:

```kubectl apply -k ./```

or

```kubectl apply -k src/main/resources/k8s/```

## License

This software is proprietary and not intended for public distribution, open source, or commercial use. All rights are reserved. Please refer to the copyright notice in the source files or contact the copyright holder for inquiries. See the LICENSE file for more details (if one exists).
