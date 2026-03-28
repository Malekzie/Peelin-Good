# AI Changelog

This file is maintained by AI agents. Every agent that makes changes to this codebase must append an entry here. Read the full entry format and rules in `.llm/AGENTS.md` before contributing.

**Append new entries at the top, below this header.**

---

## [2026-03-28] — Entity alignment, repository fixes, and auth foundation design

**Session context:** CI was failing with `No property 'email' found for type 'User'`. Investigated and fixed the root cause. Discovered additional entity/repository mismatches. Designed the auth foundation feature.

### Fixed
- `apps/backend/src/main/java/com/sait/peelin/repository/UserRepository.java` — Renamed derived query methods to match actual `User` entity field names: `findByEmail` → `findByUserEmail`, `findByUsernameOrEmail` → `findByUsernameOrUserEmail`, `existsByEmail` → `existsByUserEmail`. Spring Data JPA derives queries from Java field names, not column names — `userEmail` (field) vs `user_email` (column).
- `apps/backend/src/main/java/com/sait/peelin/repository/UserRepository.java` — Corrected generic type from `JpaRepository<User, UUID>` to `JpaRepository<User, UUID>` (re-confirmed correct after entity fix below).
- `apps/backend/src/main/java/com/sait/peelin/model/User.java` — Rewrote entity to match V1 baseline schema exactly. Removed spurious `Integer id` `@Id` field and orphaned `UUID uuid` field. The actual PK in the DB is `user_id UUID`. Added `UUID userId` as `@Id` with `GenerationType.UUID`. Removed `isActive` field (no such column in schema).
- `apps/backend/src/main/java/com/sait/peelin/service/AuthService.java` — Updated `findByUsernameOrEmail` call to `findByUsernameOrUserEmail` after repository rename.

### Changed
- `apps/backend/docs/backend.md` — Added **Platform Context** section at the top of Architecture Overview explaining the three-tier system (Workshop5 desktop, Workshop7 API, SvelteKit web, Android mobile) and that Workshop5 never calls this API. Updated the sparse **Security** section to document the actual JWT auth contract, role/route mapping, and JWT environment variables.

### Added
- `docs/superpowers/specs/2026-03-28-auth-foundation-design.md` — Full design spec for the auth foundation feature (JWT bug fixes, registration endpoint, frontend route protection, role guards, OAuth2 skeleton, auth contract doc). Branch: `feature/auth-foundation`.
- `.llm/AGENTS.md` — AI agent onboarding instructions for this repository.
- `.llm/CHANGELOG.md` — This file.

### Notes
- The CI failure was caused by a teammate's entity class not reflecting the actual PostgreSQL schema. The database schema (`V1__baseline.sql`) is the source of truth — always align Java entities to it.
- Auth foundation implementation has not started yet. See the spec above before beginning.
