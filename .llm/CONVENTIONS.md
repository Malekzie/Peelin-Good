# Conventions — Peelin' Good (Workshop7)

Quick reference for commands, repo structure, and code conventions.
For system context, architecture explanation, and the changelog process — read `AGENTS.md` first.

---

## Repo Structure

```
Workshop7/
  apps/
    backend/    # Spring Boot 3.5, Java 21, PostgreSQL + Flyway
    frontend/   # SvelteKit 5, Svelte 5, TypeScript, Tailwind CSS v4
  docs/         # backend.md, frontend.md, contributing.md, database-design.md, auth-contract.md
  .llm/         # AI agent instructions and changelog
  .github/
    workflows/
      backend-build.yml   # triggers on apps/backend/** changes only
      frontend-build.yml  # triggers on apps/frontend/** changes only
```

---

## Commands

### Backend (`apps/backend`)

```bash
./mvnw spring-boot:run           # dev server (localhost:8080)
./mvnw verify                    # compile + run all tests
./mvnw clean package -DskipTests # build JAR only
```

> Windows: use `mvnw.cmd` instead of `./mvnw`

Required environment variables:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/peelin
SPRING_DATASOURCE_USERNAME=peelin
SPRING_DATASOURCE_PASSWORD=peelin
```

### Frontend (`apps/frontend`)

```bash
npm run dev      # Vite dev server (localhost:5173)
npm run build    # production build
npm run check    # svelte-check + tsc
npm run lint     # prettier + eslint
npm run format   # auto-format
npm install      # install packages — use npm for installs, not bun
```

---

## Backend — Package Layout

| Package | Contents |
|---|---|
| `com.sait.peelin.model` | JPA entities |
| `com.sait.peelin.repository` | Spring Data JPA repositories |
| `com.sait.peelin.service` | Business logic |
| `com.sait.peelin.controller.v1` | REST controllers |
| `com.sait.peelin.dto.v1.{domain}` | Request/response DTOs |
| `com.sait.peelin.security` | JWT filter, security config |

API prefix: `/api/v1/` — route groups: `auth`, `products`, `bakeries`, `admin`, `employee`, `customer`

---

## Backend — ORM Rules

- Lombok: `@Data`, `@RequiredArgsConstructor` for boilerplate
- Spring Data JPA query method names derive from **Java field names**, not column names
  - `userEmail` (field) → method must be `findByUserEmail`, not `findByEmail`
- DB enum values are lowercase (`admin`, `employee`, `customer`) — Java enum values must match exactly
- `@Enumerated(EnumType.STRING)` on all enum fields

---

## Backend — Migrations

Naming: `V{n}__{description}.sql` (double underscore, incrementing integer)

| File | Purpose |
|---|---|
| `V0__extensions.sql` | pgcrypto extension |
| `V1__baseline.sql` | Full schema baseline — source of truth |
| `V2__unified_schema.sql` | Schema refinements |
| `V3__seed_data.sql` | Seed/test data |
| `V4+` | Next migration starts here |

---

## Frontend — Code Rules

| Rule | Detail |
|---|---|
| State | Svelte 5 runes only: `$state`, `$derived`, `$effect`, `$props` — no `$:` or `export let` |
| Navigation | `goto()`, `pushState()`, `replaceState()` from `$app/navigation` — never bare `<a>` for SPA transitions |
| Styling | Tailwind CSS v4 only — no other CSS frameworks |
| Icons | `@lucide/svelte` only |
| Components | `src/lib/components/ui/` following shadcn-svelte conventions |

### Design tokens

| Name | Hex |
|---|---|
| Cream | `#FAF7F2` |
| Walnut | `#2C1A0E` |
| Terracotta | `#C4714A` |
| Sage | `#8A9E7F` |

---

## Hard Constraints

- No emojis in UI or code
- Do not commit — the user handles all git operations
- Do not add unrelated refactoring, docstrings, or improvements to code you are not directly changing
- Never modify an applied Flyway migration — always add a new one
