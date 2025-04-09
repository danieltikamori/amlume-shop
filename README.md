# Amlume Shop

[![License: Proprietary](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE) <!-- Adjust if license changes -->

UNDER DEVELOPMENT and testing techniques

## Overview

This project is the Spring Boot backend application for Amlume Shop. It provides RESTful APIs with a strong focus on security using PASETO tokens, efficient caching, rate limiting leveraging Valkey/Redis, and observability through Micrometer metrics.

## Features

*   **Robust Security:**
    *   PASETO v4 token-based authentication (Stateless).
    *   Support for both Public (signed) and Local (encrypted) Access Tokens.
    *   Secure Refresh Token mechanism with database persistence and hashing.
    *   JTI (JWT ID) validation using Redis/Valkey to prevent token replay attacks.
    *   Token Revocation mechanism with Redis/Valkey caching.
    *   Detailed token claim validation (expiration, issuer, audience, subject, etc.).
*   **Resilience:**
    *   Redis/Valkey-based Rate Limiting using an efficient Lua script (Sliding Window algorithm).
*   **Performance:**
    *   Redis/Valkey caching for token claims and revocation status.
*   **Observability:**
    *   Integrated Micrometer metrics for monitoring token generation, validation, and potentially other application aspects.
*   **User Management:** (Implied) Basic user data persistence and retrieval.
*   **Configuration:** Flexible configuration via Spring Boot profiles (`application.yml`/`properties`).

## Technologies Used

*   **Language:** Java 17+
*   **Framework:** Spring Boot 3.x
    *   Spring Web (MVC)
    *   Spring Security
    *   Spring Data JPA
    *   Spring Data Redis (Lettuce Client)
    *   Spring Cache Abstraction
    *   Spring Scheduling (`@Scheduled`)
*   **Database:** Relational Database (e.g., PostgreSQL, MySQL - Requires configuration)
*   **Cache/In-Memory Data Grid:** Valkey / Redis
*   **Security Tokens:** PASETO v4 (via `paseto4j` library)
*   **Metrics:** Micrometer
*   **Build Tool:** Maven / Gradle
*   **Utilities:** Lombok

## Prerequisites

*   **JDK:** Version 17 or higher
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

  *   **Gradle:**

```Bash
./gradlew clean build
```

## Running the Application

*   **Using Maven:**

```Bash
mvn spring-boot:run
```

*   **Using Gradle:**

```Bash
./gradlew bootRun
```

*   **Running the JAR:**

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

*(Optional: Add details here if you use Swagger/OpenAPI)*

API documentation can be accessed via Swagger UI at `/swagger-ui.html` once the application is running (if configured).

*Or: API documentation is maintained separately at [Link to Docs].*

## Testing

Run the test suite using your build tool:

*   **Maven:**

```Bash
mvn test
```

*   **Gradle:**

```Bash
./gradlew test
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
