# FleetIQ Backend Engineering Standards (v1.0.0)

This document establishes the official architectural guidelines, code styling, configuration policies, and implementation standards for all FleetIQ Java microservices. All developers and automated code generators must strictly adhere to these specifications to guarantee system consistency, security, observability, and scalability.

---

## 1. Core Technology Stack & Tooling

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.2.x (utilizing `jakarta.*` namespace)
- **Build Tool**: Maven (with `pom.xml` for dependencies, plugins, and build profiles)
- **Database**: PostgreSQL 16 / TimescaleDB 2.x
- **ORM**: Spring Data JPA / Hibernate 6.x (with Hibernate Spatial for geospatial support)
- **Migration**: Flyway (schema migrations placed under `src/main/resources/db/migration/`)
- **Messaging**: Apache Kafka (messaging backbone) & MQTT (device ingestion via HiveMQ broker)
- **Testing**: JUnit 5, AssertJ, Mockito, and Testcontainers (PostgreSQL, Kafka, HiveMQ)

---

## 2. Package & Layering Structure

Every microservice must organize its source files inside the base package `com.fleetiq.{service}` using a strictly layered approach:

```
com.fleetiq.{service}/
├── config/             # Spring Security, database, CORS, MVC, and bean definitions
├── controller/         # REST Controllers exposing REST API endpoints
├── service/            # Business logic interfaces and implementation classes
│   └── impl/           # Class implementations (e.g., VehicleServiceImpl)
├── repository/         # Spring Data JPA / TimescaleDB database access interfaces
├── entity/             # JPA/Hibernate model entities mapping to database tables
├── dto/                # Data Transfer Objects (immutable Java records preferred)
│   ├── request/        # Request payloads (e.g., CreateVehicleRequest)
│   └── response/       # Response payloads (e.g., VehicleResponse)
├── exception/          # Custom exceptions and the RFC 7807 global exception handler
├── listener/           # Kafka message listeners and MQTT inbound handlers
├── client/             # REST/Feign/WebClient integrations for external services
└── utils/              # Context helpers, cryptography, and helper utilities
```

---

## 3. Naming Conventions

### 3.1 Java Code
- **Classes**: `PascalCase` (e.g., `VehicleController`, `JwtAuthenticationFilter`, `CreateApiKeyRequest`).
- **Interfaces**: `PascalCase` without prefixes (e.g., `VehicleService`, not `IVehicleService`).
- **Implementations**: `PascalCase` with `Impl` suffix (e.g., `VehicleServiceImpl`).
- **Methods & Variables**: `camelCase` (e.g., `findActiveVehiclesByTenant()`, `registrationNumber`).
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_SPEED_LIMIT_KMH`).

### 3.2 Database (PostgreSQL / TimescaleDB)
- **Tables & Columns**: `snake_case` singular (e.g., `vehicle`, `registration_number`).
- **Primary Keys**: Always named `id` of type `UUID` auto-generated via `gen_random_uuid()`.
- **Foreign Keys**: Prefix with `fk_{referencing_table}_{referenced_table}` (e.g., `fk_vehicle_tenant`).
- **Indexes**: Prefix with `idx_{table}_{columns}` (e.g., `idx_vehicle_registration`).

---

## 4. Multi-Tenancy & Security Context

Multi-tenancy is enforced on every request to ensure logical data isolation.

### 4.1 Tenant Context
- A thread-local context `TenantContext` stores the active tenant ID (`UUID`) for the duration of the thread execution.
- Every request must clear this context upon completion (implemented in filter `finally` blocks) to prevent context pollution in thread pools.

### 4.2 Web Traffic (JWT Auth)
- Authenticated HTTP requests must include a `Bearer` token in the `Authorization` header.
- The JWT must contain:
  - Subject (`sub`): User email address.
  - Claim (`tenant_id`): Tenant's UUID identifier.
  - Claim (`role`): Authenticated user role code (e.g., `TENANT_ADMIN`, `FLEET_MANAGER`).
  - Claim (`permissions`): Array of permission strings (e.g., `["vehicles:view", "vehicles:create"]`).
- A `JwtAuthenticationFilter` intercepts requests, parses the token, populates the `SecurityContextHolder`, and binds the `tenant_id` to the `TenantContext`.

### 4.3 Device/Backend Traffic (API Key + Cert)
- For IoT device pathways or backend-to-backend calls where JWT is not applicable, the client must supply:
  - `X-API-Key` header (or query param `key` for streaming protocols)
  - `X-Tenant-ID` header
- API keys must be stored in the database hashed using SHA-256 (`key_hash`), while a human-readable prefix (`key_prefix`, e.g., first 12 characters like `flq_live_abc1`) is exposed in logs.
- Gateway services must call `/api/v1/auth/verify-key` to validate the API key dynamically.

---

## 5. REST API Versioning & Request Validation

### 5.1 Endpoint Paths & Standards
- All HTTP REST endpoints must use lowercase, plural paths prefixed with `/api/v1/` (e.g., `/api/v1/vehicles`, `/api/v1/auth/login`).
- Standard HTTP status codes must be returned:
  - `200 OK`: Successful GET, PUT, or POST (returning payload).
  - `201 Created`: Successful creation of resources.
  - `204 No Content`: Successful soft-deletion or void updates.
  - `400 Bad Request`: Input validation or logical parameter errors.
  - `401 Unauthorized`: Missing or invalid auth credentials.
  - `403 Forbidden`: Authenticated user lacks permission.
  - `404 Not Found`: Resource does not exist.
  - `500 Internal Server Error`: Unexpected system failure.

### 5.2 Request Validation
- Input request records must be validated at the controller level using `jakarta.validation` annotations:
  - String inputs: `@NotBlank`, `@Size(max = ...)` (bounds checking is mandatory to prevent buffer overflows).
  - Numeric values: `@Min`, `@Max`, `@PositiveOrZero`.
  - Collections: `@NotEmpty`, `@Size`.
- REST controllers must be annotated with `@Valid` or `@Validated` on request bodies.

---

## 6. RFC 7807 Error Handling Standards

All Spring Boot services must implement a global Exception Handler using `@RestControllerAdvice` to convert exceptions to **RFC 7807 (Problem Details)** JSON bodies.

### 6.1 Problem Details Schema
The response payload must contain the following keys:
- `type`: URI reference identifying the problem type.
- `title`: Short, human-readable summary of the problem.
- `status`: HTTP status code.
- `detail`: Detailed description of the specific error occurrence.
- `instance`: URI of the specific request path that generated the error.
- `timestamp`: Timestamp in ISO 8601 format.
- `correlation_id`: Trace correlation ID extracted from MDC.
- `invalid_params`: Optional list of parameter-level validation failures containing `name` and `reason`.

### 6.2 Error JSON Example
```json
{
  "type": "https://api.fleetiq.com/errors/validation-error",
  "title": "Invalid Request Content",
  "status": 400,
  "detail": "One or more validation constraints failed. Please check the 'invalid_params' list.",
  "instance": "/api/v1/vehicles",
  "timestamp": "2026-06-13T14:44:00Z",
  "correlation_id": "a90a2b53-48bd-4db1-86d3-ee831cb67b7f",
  "invalid_params": [
    {
      "name": "registrationNumber",
      "reason": "Registration number must not exceed 20 characters"
    }
  ]
}
```

---

## 7. Kafka Messaging Guidelines

### 7.1 Topic Naming Conventions
Topics must be lowercase, separated by dots, and split by domain:
- **`raw.telemetry`**: Raw IoT ingestion topic (written by Gateway Service).
- **`processed.telemetry`**: Parsed, validated, and normalized telemetry (written by Tracking Service).
- **`alerts.outbound`**: Alarm and notification alerts emitted by rules engine.

### 7.2 Kafka Message Headers
Every record written to Kafka must include the following metadata headers:
- `tenant-id`: Active tenant UUID.
- `correlation-id`: Request correlation trace ID.
- `producer-id`: Originating microservice name (e.g., `fleetiq-device-gateway-service`).

### 7.3 Transactional Guarantees
To prevent double-counting of telemetries and ensure Exactly-Once Semantics (EOS):
- **Producers** must enable idempotence:
  ```properties
  spring.kafka.producer.properties.enable.idempotence=true
  spring.kafka.producer.acks=all
  spring.kafka.producer.retries=3
  ```
- **Consumers** must read committed records only:
  ```properties
  spring.kafka.consumer.properties.isolation.level=read_committed
  ```

---

## 8. MQTT Ingestion Guidelines

- **Format**: All incoming telemetries must be formatted as JSON payloads containing `apiKey` and `deviceId`.
- **Topics**: Subscriptions must follow the topic wildcard structure: `telemetry/#` (e.g., `telemetry/{tenantId}/{deviceId}`).
- **Key Verification**: The ingestion gateway must intercept the incoming payload, extract the `apiKey`, call the Auth Service dynamically for validation, and capture the associated `tenantId`.
- **DLQ Routing**: Payload parsing failures, schema validation failures (missing location/timestamp), or invalid keys must be serialized into a `DlqEnvelope` and routed immediately to the Kafka `raw.telemetry.dlq` topic.

---

## 9. Live Streaming WebSockets (STOMP)

For live map tracking, services must stream updates over WebSockets using the STOMP protocol.
- **Topics & Channels**:
  - `/topic/vehicle/{vehicleId}`: Live position stream for a specific vehicle.
  - `/topic/fleet/{tenantId}`: Live aggregated stream for an entire tenant fleet.
- **Authentication**: Devices or frontend applications must pass their access token (JWT) or API Key in the query parameter (e.g., `?token=...` or `?key=...`) during the WebSocket handshake to authorize the connection.
- **Scale Boundary**: Use STOMP for standard operations; if client scaling exceeds 10,000 concurrent listeners, transition to raw WebSockets supported by a Redis pub/sub broker.

---

## 10. Observability & Logging

### 10.1 Logging & MDC
Every thread handling a request or consuming a queue message must configure its Mapped Diagnostic Context (MDC) at execution entry and clean it at execution exit:
- `traceId` / `correlationId`: Correlation identifier propagating across microservice borders.
- `tenantId`: Active tenant UUID context.
- `userId`: Active authenticated user email/ID (if applicable).

Console logs must use a standard format containing MDC tags:
```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [Tenant:%X{tenantId}] [Trace:%X{traceId}] - %msg%n
```

### 10.2 OpenTelemetry Tracing
- All microservices must include OpenTelemetry dependencies.
- OpenTelemetry spans and trace contexts must propagate:
  - Across HTTP boundaries using W3C Trace Context headers (`traceparent`).
  - Across Kafka payloads using Record Headers.

---

## 11. Testing Standards

- **Code Coverage**: All services must maintain a strict test coverage threshold of **>80%**.
- **Unit Testing**: JUnit 5, AssertJ, and Mockito.
- **Integration Testing**: SpringBootTest + Testcontainers.
- **No In-Memory Databases**: The use of H2 or other in-memory mock databases for integration testing is strictly prohibited. Tests must run against real database engines (PostgreSQL 16) and message brokers (Kafka, HiveMQ) spun up locally via Docker containers during the build process using Testcontainers.

---

## 12. Git Branching & CI/CD Strategy

### 12.1 Git Branching Model
- **`main`**: Represents production-ready code. Direct commits are forbidden.
- **`develop`**: Primary integration branch.
- **`feature/*`**: Feature branches created from `develop`. Must be merged via Pull Requests.
- **`hotfix/*`**: Emergency production fixes branched from `main`, merged back into both `main` and `develop`.

### 12.2 CI/CD Rules
- Every Pull Request (PR) must trigger a GitHub Actions workflow that:
  - Compiles the code (Java 21, Maven).
  - Runs all unit and integration tests (spinning up PostgreSQL, Kafka, and HiveMQ Testcontainers).
  - Verifies code coverage exceeds 80% (using Jacoco).
  - Runs static code analysis (SonarQube) to check for code smells, bugs, and security vulnerabilities.
- Merges to `main` must trigger automatic deployment pipelines (CI/CD) to staging/production clusters.
