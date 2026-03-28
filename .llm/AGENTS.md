# AI Agent Instructions — Peelin' Good (Workshop7)

You are working on **Workshop7**, a full-stack e-commerce platform for a bakery chain called Peelin' Good. Read this file fully before touching any code.

---

## Step 1: Orient yourself — read the docs first

Before writing a single line of code, read the following files in order:

| File | What it tells you |
|---|---|
| `docs/backend.md` | API architecture, security model, database setup, CI/CD |
| `docs/frontend.md` | SvelteKit structure, routing conventions, styling rules |
| `docs/database-design.md` | Full entity model, column types, relationships |
| `docs/contributing.md` | Branching strategy, commit conventions, PR process |
| `docs/auth-contract.md` | JWT auth flow, endpoints, roles, error codes (once written) |
| `docs/superpowers/specs/` | Design specs for features — read any spec relevant to your task |

Then read:

| File | What it tells you |
|---|---|
| `.llm/CHANGELOG.md` | What AI agents have already changed — prevents duplicating or undoing work |

**Do not assume you know the current state of the codebase from your training data. Read the files.**

---

## Step 2: Understand the system

### Three-tier architecture

| Tier | Project | Stack | Notes |
|---|---|---|---|
| Desktop management app | Workshop5 | JavaFX + MySQL | Staff-only. Direct JDBC to MySQL. Does NOT call this API. Has its own auth. |
| REST API | Workshop7 / `apps/backend` | Spring Boot 3.5 + PostgreSQL | Serves web and mobile clients |
| Web frontend | Workshop7 / `apps/frontend` | SvelteKit 5 + TypeScript | Customer-facing e-commerce site |
| Android mobile app | Workshop06 | Kotlin + Room | Secondary/reference. Calls the same API. |

**Workshop5 is a completely separate system.** Never design Workshop7 features to accommodate it.

### Who calls this API

- SvelteKit web frontend (browser)
- Android mobile app (Workshop06)
- Both use `Authorization: Bearer <token>` headers

### Key constraints

- No git commits — the user handles all commits
- No emojis in code or UI
- Icons via `@lucide/svelte` only
- Frontend styling: Tailwind CSS v4 only — no other CSS frameworks
- Svelte 5 runes only (`$state`, `$derived`, `$effect`, `$props`) — no legacy `$:` or `export let`
- Never modify an applied Flyway migration — always add a new one
- The database schema is the source of truth — align Java entities to it, not the other way around

### Database source of truth

The `V1__baseline.sql` migration defines the canonical schema. When in doubt about a column name, type, or nullability — read that file. Java entity field names must reflect actual column types (e.g., `user_id UUID` → `UUID userId`, not `Integer id`).

### Current migration versions

| File | Purpose |
|---|---|
| `V0__extensions.sql` | pgcrypto extension |
| `V1__baseline.sql` | Full schema baseline |
| `V2__unified_schema.sql` | Schema refinements |
| `V3__seed_data.sql` | Seed/test data |
| `V4+` | Add new migrations here, incrementing version |

---

## Step 3: Work conventions

### Backend (Spring Boot)

- Package root: `com.sait.peelin`
- Layered pattern: `model → repository → service → controller`
- DTOs live in `dto/v1/{domain}/`
- Use Lombok (`@Data`, `@RequiredArgsConstructor`) for boilerplate
- Spring Data JPA derived query names must match Java field names exactly (not column names)
- Enum values in DB are lowercase (`admin`, `employee`, `customer`) — Java enum values must match

### Frontend (SvelteKit)

- File-based routing under `src/routes/`
- Always use `goto()`, `pushState()`, `replaceState()` for internal navigation
- Design tokens: cream `#FAF7F2`, walnut `#2C1A0E`, terracotta `#C4714A`, sage `#8A9E7F`
- Run `bun check` and `bun lint` before considering frontend work done

### Auth

- JWT stored in a regular (non-httpOnly) cookie named `jwt` on the frontend
- All API calls include `Authorization: Bearer <token>` header
- `hooks.server.ts` decodes JWT payload (base64, no signature check) for route protection
- Role claim format from Spring: `ROLE_CUSTOMER`, `ROLE_EMPLOYEE`, `ROLE_ADMIN` — strip prefix when storing in `locals.user`

---

## Step 4: Log your changes

**This is mandatory.** After completing any meaningful work, append an entry to `.llm/CHANGELOG.md`.

### Format

```markdown
## [YYYY-MM-DD] — Short title

**Session context:** What task or feature was being worked on.

### Fixed
- `path/to/file.java` — What was wrong, what you changed, why.

### Added
- `path/to/file.ts` — What you added and why.

### Changed
- `path/to/file.md` — What changed and the reason.

### Migrations
- `V4__description.sql` — What schema change and why it was needed.
```

### Rules for the changelog

- **Append only** — never edit or delete past entries
- **Newest entry at the top**, below the header
- **Be specific** — name the file and the reason, not just "fixed a bug"
- **Include migrations** separately so teammates can track schema changes
- **If you read the docs and made no changes**, you do not need to log anything
- **If a design spec guided your work**, reference it: `(spec: docs/superpowers/specs/YYYY-MM-DD-name.md)`

---

## Step 5: Before you finish

- Run `./mvnw verify` (backend) or `npm run check && npm run lint` (frontend) to confirm nothing is broken
- Check that any new Flyway migration is the correct next version number
- Confirm the changelog entry is written
- Do not commit — the user handles git
