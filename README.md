# Amlume Shop

[![License: Proprietary](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE) UNDER DEVELOPMENT (see dev branch) and testing techniques

## Overview

This project is the Spring Boot backend application for Amlume Shop. It provides RESTful APIs with a strong focus on security using **Passkeys** (replacing PASETO and MFA), efficient caching, resilience, rate limiting leveraging Valkey/Redis, and observability through Micrometer metrics.

## Features

* **Robust Security:**
    * **Spring Authorization Server** for user authentication and authorization (replacing Keycloak OATH2 and MFA).
    * **Passkeys** for strong, phishing-resistant multi-factor authentication (replacing traditional MFA).
    * Secrets management using Spring Vault, HashiCorp Vault with fallback retrieval.
    * ~~PASETO v4 token-based authentication (Stateless).~~ **(REPLACED BY JWTs from SAS)**
    * Support for both Public (signed) and Local (encrypted) ~~Access~~ Tokens. **(NOW STANDARD JWTs)**
    * Secure Refresh Token mechanism with database persistence and hashing.
    * JTI (JWT ID) validation using Redis/Valkey to prevent token replay attacks.
    * Token Revocation mechanism with Redis/Valkey caching.
    * Detailed token claim validation (expiration, issuer, audience, subject, etc.).
    * Support for multiple token types (Access, Refresh, etc.) with different expiration policies.
* **Resilience:**
    * Redis/Valkey-based Rate Limiting using an efficient Lua script (Sliding Window counter algorithm).
* **Performance:**
    * Redis/Valkey caching for user, category, products, token claims, revocation status, etc.
* **Observability:**
    * Integrated Micrometer metrics for monitoring token generation, validation, and potentially other application aspects.
    * Logging with SLF4J and Logback. Data sent to Loki via Promtail container.
    * Integration with Spring Boot Actuator for health checks and metrics.
    * Health checks and readiness/liveness probes for Kubernetes (probably in the parallel project geared towards Kubernetes).
* **User Management:** (Implied) Role based authentication/authorization using **Spring Authorization Server** and **Passkeys**.
* **Configuration:** Flexible configuration via Spring Boot profiles (`application.yml`/`properties`).

## Technologies Used

* **Language:** Java 21+
* **Framework:** Spring Boot 3.x
    * Spring Web (MVC)
    * Spring Security
    * Spring Data JPA
    * Spring Data Redis (Lettuce Client)
    * Spring Boot Actuator
    * Spring Boot Starter for Valkey/Redis
    * Spring Vault
    * Spring Cache Abstraction
    * Spring Scheduling (`@Scheduled`)
    * **Spring Authorization Server** (`org.springframework.boot:spring-boot-starter-oauth2-authorization-server`)
    * **Spring Security OAuth2 Resource Server** (`org.springframework.boot:spring-boot-starter-oauth2-resource-server`)
* **Database:** Relational Database (e.g., PostgreSQL, MySQL - Requires configuration)
* **Cache/In-Memory Data Grid:** Valkey / Redis
* **Security Tokens:** **JWTs (issued by Spring Authorization Server)** ~~PASETO v4 (via `paseto4j` library)~~
* **Authentication:** **Passkeys (via WebAuthn - implementation details to be added)** ~~Keycloak OATH2 and MFA~~
* **Metrics:** Micrometer, Loki, Prometheus, Promtail, Grafana
* **Build Tool:** Maven
* **Utilities:** MapStruct, JUnit 5, Mockito, AssertJ, Testcontainers (for testing)

## Prerequisites

* **JDK:** Version 21 or higher
* **Build Tool:** Maven 3.8+ or Gradle 7.x+
* **Valkey / Redis:** A running instance accessible by the application.
* **Database:** A running instance of a supported relational database (e.g., PostgreSQL 13+).
* **(Optional) Docker & Docker Compose:** For easily running dependencies locally.

## Getting Started

1.  **Clone the repository:**

    ```Bash
    git clone <repository-url>
    cd amlume-shop
    ```

2.  **Configure Application:**
    * Generate or get the required credentials for database, Loki, Valkey, etc.
    * Create a .env file to store environment variables (sensitive data). Do not commit this file to version control. 
    * If running the application could not load the .env file, load through your IDE or OS.
    * Update `application-local.yml` (if using the local profile) with your specific settings:
        * **Valkey/Redis Connection:** `valkey.host`, `valkey.port`, `valkey.password`.
        * **Database Connection:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`.
        * ~~**PASETO Keys:** Configure paths or values for `paseto.access.public/private`, `paseto.access.secret`, `paseto.refresh.secret` and associated Key IDs (`kid`). **Ensure these are kept secure and are not committed to version control if they contain sensitive key material.**~~ **(REPLACED BY SAS)**
        * **Spring Authorization Server:** Configure issuer URI (`spring.security.oauth2.authorizationserver.issuer-uri`).
        * **Client Registration:** Configure client details for `amlume-shop` (client-id, client-secret, redirect-uris, scopes) in SAS configuration (e.g., in a `RegisteredClientRepository` bean).
        * **JWK Set URI:** (Typically auto-configured by SAS, but verify if needed).
        * **Cache TTLs:** Review `cache.*.ttl-seconds` values if needed.
        * **Rate Limiting:** Configure parameters if exposed via `application.yml`.
        * **(Optional) JTI Settings:** Configure Bloom Filter size/error rate (`jti.bloomfilter.*`) and cleanup rate (`jti.cleanup.rate-ms`) if using the Redisson-based JTI service (Note: The latest context suggests a simpler Redis TTL approach might be used now).
        * **Passkey Configuration:** (Details to be added based on WebAuthn library used).

3.  **Build the project:**
    * **Maven:**

    ```Bash
    mvn clean install
    ```

## Running the Application

* **Using Maven:**

    ```Bash
    mvn spring-boot:run
    ```

* **Running the JAR:**

    ```Bash
    java -jar target/amlume-shop-[version].jar
    ```

  *(Adjust JAR name based on build output)*

The application will start, connect to Valkey/Redis and the database, and be available (typically on port 8080, unless configured otherwise). Ensure Spring Authorization Server is also running and accessible if it's deployed separately.

## Configuration Reference

Key configuration properties are typically found in `src/main/resources/application.yml` (or `.properties`). Pay close attention to:

* `server.port`
* `spring.datasource.*` (Database connection)
* `valkey.*` (Valkey/Redis connection)
* `spring.security.oauth2.authorizationserver.issuer-uri` (SAS issuer URI)
* **Client Registration Details** (in SAS configuration)
* `cache.*` (Cache Time-To-Live settings)
* `logging.level.*` (Logging configuration)
* Rate limiting parameters (e.g., window size, max requests - check `TokenConfigurationService` or similar)
* JTI service parameters (if applicable)
* **Passkey related configurations** (to be added)

## API Documentation

Documentation will be available in HTML format, using the official swagger-ui jars.

The Swagger UI page will then be available at http://server:port/context-path/swagger-ui.html and the OpenAPI description will be available at the following url for json format: http://server:port/context-path/v3/api-docs

* **server:** The server name or IP
* **port:** The server port
* **context-path:** The context path of the application

Documentation can be available in yaml format as well, on the following path : /v3/api-docs.yaml

## Testing

Run the test suite using your build tool:

* **Maven:**

    ```Bash
    mvn test
    ```

Integration tests might require running instances of Valkey/Redis and the database. Consider using Testcontainers for managing dependencies during testing. **Testing for Passkey integration will require specific strategies and potentially mocking browser/authenticator behavior.**

## Kubernetes Configurations (To be implemented)

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