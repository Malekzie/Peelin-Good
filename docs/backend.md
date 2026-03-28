# Backend — Peelin' Good API

Detailed documentation for the Spring Boot backend located in `apps/backend/`.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Running Locally](#setup--running-locally)
- [Database & Migrations](#database--migrations)
- [Configuration](#configuration)
- [Security](#security)
- [Testing](#testing)
- [Docker](#docker)
- [CI/CD](#cicd)
- [Common Issues](#common-issues)

---

## Platform Context

Peelin' Good is a **three-tier system**. This backend is the API tier — it does not operate in isolation.

| Tier | Project | Stack | Connects to |
| --- | --- | --- | --- |
| Desktop management app | Workshop5 | Java 23 + JavaFX + MySQL | MySQL directly via JDBC |
| REST API (this project) | Workshop7 / `apps/backend` | Spring Boot + PostgreSQL | PostgreSQL via JPA |
| Web frontend | Workshop7 / `apps/frontend` | SvelteKit + TypeScript | This API over HTTP |
| Android mobile app | Workshop06 | Kotlin + Room | This API over HTTP |

**Important:** Workshop5 (the desktop app) connects **directly to MySQL** and never calls this API. It has its own standalone authentication system (BCrypt + in-memory `UserSession`). Do not design Workshop7 features to accommodate Workshop5 — they are independent systems that share a domain but not infrastructure.

The API's clients are the **SvelteKit web frontend** and the **Android mobile app**. Both authenticate using JWT Bearer tokens.

---

## Architecture Overview

The backend is a REST API built with **Spring Boot 3.5.11** running on **Java 21**. It uses:

- **Spring Web** for REST endpoints
- **Spring Data JPA** for database access
- **Spring Security** for authentication and authorization
- **Spring Validation** for request validation
- **Flyway** for database schema migrations
- **Spring Boot Actuator** for health checks and monitoring
- **PostgreSQL** as the production database

---

## Tech Stack

| Technology | Version | Purpose |
| --- | --- | --- |
| Java | 21 | Language runtime |
| Spring Boot | 3.5.11 | Application framework |
| Spring Security | (managed by Boot) | Auth |
| Spring Data JPA | (managed by Boot) | ORM / data access |
| Flyway | (managed by Boot) | Database migrations |
| PostgreSQL | 15+ | Relational database |
| Maven | Wrapper included | Build tool |
| JUnit 5 | (managed by Boot) | Testing framework |

---

## Project Structure

```
apps/backend/
├── .mvn/                          # Maven wrapper support files
├── src/
│   ├── main/
│   │   ├── java/com/sait/peelin/
│   │   │   └── Application.java   # Entry point (@SpringBootApplication)
│   │   └── resources/
│   │       ├── application.yaml   # Spring configuration
│   │       ├── db/
│   │       │   └── migration/     # Flyway SQL migration files
│   │       ├── static/            # Static resources (if any)
│   │       └── templates/         # Server-side templates (if any)
│   └── test/
│       └── java/com/sait/peelin/
│           └── ApplicationTests.java
├── Dockerfile                     # Multi-stage Docker build
├── pom.xml                        # Maven project configuration
├── mvnw                           # Maven wrapper (Linux/macOS)
├── mvnw.cmd                       # Maven wrapper (Windows)
└── HELP.md                        # Spring Boot reference links
```

### Package Convention

The base package is `com.sait.peelin`. All new packages should be created under this namespace so that Spring's component scanning picks them up automatically.

Recommended package layout:

```
com.sait.peelin/
├── config/         # Configuration classes (@Configuration, security config, etc.)
├── controller/     # REST controllers (@RestController)
├── dto/            # Data Transfer Objects (request/response bodies)
├── entity/         # JPA entities (@Entity)
├── exception/      # Custom exceptions and global exception handlers
├── repository/     # Spring Data JPA repositories (@Repository)
└── service/        # Business logic (@Service)
```

---

## Setup & Running Locally

### Prerequisites

- **Java 21+** — [Adoptium Temurin](https://adoptium.net/) is recommended
- **PostgreSQL 15+** — running locally or via Docker

### 1. Start PostgreSQL

If you don't have PostgreSQL installed, run it with Docker:

```sh
docker run -d \
  --name peelin-db \
  -e POSTGRES_DB=peelin \
  -e POSTGRES_USER=peelin \
  -e POSTGRES_PASSWORD=peelin \
  -p 5432:5432 \
  postgres:15-alpine
```

### 2. Configure the database connection

Set environment variables or edit `src/main/resources/application.yaml`:

```sh
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/peelin
export SPRING_DATASOURCE_USERNAME=peelin
export SPRING_DATASOURCE_PASSWORD=peelin
```

### 3. Run the application

```sh
cd apps/backend

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The API will start on **http://localhost:8080**.

> **Tip:** Spring Boot DevTools is included as a dependency. The server will auto-restart when you recompile classes.

---

## Database & Migrations

We use **Flyway** for database schema management. Flyway runs automatically on application startup and applies any pending migrations.

### Migration files

All migration SQL files live in:

```
src/main/resources/db/migration/
```

### Naming convention

Flyway requires a strict naming pattern:

```
V{version}__{description}.sql
```

- `V` — prefix (uppercase)
- `{version}` — numeric version, use underscores for sub-versions (e.g., `1`, `2`, `1_1`)
- `__` — double underscore separator
- `{description}` — human-readable description using underscores

**Examples:**

```
V1__create_users_table.sql
V2__create_products_table.sql
V3__add_email_to_users.sql
```

### Rules

1. **Never modify a migration that has already been applied** (i.e., committed and merged). Flyway checksums will detect the change and refuse to start.
2. **Always add new migrations** with incrementing version numbers.
3. **Test migrations locally** before pushing by running the app and checking that Flyway applies them cleanly.
4. If you need to undo a change, write a **new** migration that reverses it.

---

## Configuration

The main configuration file is `src/main/resources/application.yaml`. Spring Boot also supports profile-specific configs:

- `application.yaml` — base/default config
- `application-dev.yaml` — development overrides
- `application-prod.yaml` — production overrides

Activate a profile at runtime:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Key environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | — | JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | — | Database user |
| `SPRING_DATASOURCE_PASSWORD` | — | Database password |
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | — | Active Spring profile(s) |

---

## Security

The backend uses **stateless JWT authentication** via Spring Security and the Nimbus JOSE library.

### Auth flow

1. Client sends credentials to `POST /api/v1/auth/login` or `POST /api/v1/auth/register`
2. Server validates credentials, returns a signed JWT in the response body
3. Client stores the JWT and includes it as `Authorization: Bearer <token>` on every subsequent request
4. `JwtAuthenticationFilter` validates the token and populates the `SecurityContext` on each request

### Roles

| Role | Access |
| --- | --- |
| `CUSTOMER` | `/api/v1/customer/**` |
| `EMPLOYEE` | `/api/v1/employee/**` and customer endpoints |
| `ADMIN` | `/api/v1/admin/**` and all other endpoints |

Public endpoints (no token required): `/api/v1/auth/**`, `/api/v1/products/**`, `/api/v1/bakeries/**`, `/actuator/**`

### JWT configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `JWT_SECRET` | — (required) | HMAC-SHA256 signing key |
| `JWT_EXPIRATION` | `864000000` (10 days) | Token lifetime in milliseconds |
| `JWT_ISSUER` | `peelin-good` | Token issuer claim |

### Security configuration

The `SecurityConfig` class is in `com.sait.peelin.security`. When adding new endpoints, update the `authorizeHttpRequests` rules there to define access level.

---

## Testing

### Running tests

```sh
# Run all tests
./mvnw verify

# Run tests only (skip other verify phases)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ApplicationTests

# Run tests with verbose output
./mvnw verify -X
```

### Test dependencies

| Dependency | Purpose |
| --- | --- |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, Spring Test |
| `spring-security-test` | Testing security configurations, mock users |

### Writing tests

- Place tests under `src/test/java/com/sait/peelin/` mirroring the main source structure.
- Use `@SpringBootTest` for integration tests that need the full application context.
- Use `@WebMvcTest` for controller-layer unit tests.
- Use `@DataJpaTest` for repository-layer tests.

---

## Docker

The backend uses a **multi-stage Dockerfile** for optimized builds:

### Stage 1 — Build

- Base image: `eclipse-temurin:21-jdk`
- Runs `mvnw package -DskipTests` to produce the JAR

### Stage 2 — Runtime

- Base image: `gcr.io/distroless/java21-debian12` (minimal, no shell, no package manager)
- Runs as a non-root user (`nonroot`)
- Exposes port `8080`
- Uses container-aware JVM settings (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`)

### Build locally

```sh
cd apps/backend
docker build -t peelin-backend .
docker run -p 8080:8080 peelin-backend
```

---

## CI/CD

The backend CI is defined in `.github/workflows/backend-build.yml`. It triggers **only** when files under `apps/backend/` change.

### On pull request

1. Checkout code
2. Set up JDK 21 (Temurin) with Maven cache
3. Run `./mvnw verify` (compile + tests)
4. Build Docker image (smoke test)
5. Deploy a **PR preview** to DigitalOcean
6. Comment the preview URL on the PR (or link to failure logs)

### On push to main

1. Same build & test steps as PR
2. Build and **push** Docker image to GitHub Container Registry (`ghcr.io`)
3. **Deploy to production** on DigitalOcean

### CodeScene Delta Analysis

CodeScene also runs on PRs to analyze code health. Watch for its comments — it flags:

- Increasing complexity in changed files
- Code duplication
- Coupling between files
- Functions that are growing too large

---

## Common Issues

### "Permission denied" on `mvnw`

Make the wrapper executable:

```sh
chmod +x mvnw
```

On Windows, use `mvnw.cmd` instead.

### Flyway checksum mismatch

This means a previously applied migration file was modified. **Never edit applied migrations.** If this happens locally, you can reset your database:

```sh
docker rm -f peelin-db
# Then recreate it (see Setup section above)
```

### Port 8080 already in use

Either stop the process using port 8080, or change the port:

```sh
./mvnw spring-boot:run -Dserver.port=9090
```

### Tests fail with database connection errors

Make sure PostgreSQL is running and your connection environment variables are set. For tests, you may want to configure an embedded database or use Testcontainers.