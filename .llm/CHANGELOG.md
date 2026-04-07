# AI Changelog

This file is maintained by AI agents. Every agent that makes changes to this codebase must append an entry here. Read the full entry format and rules in `.llm/AGENTS.md` before contributing.

**Append new entries at the top, below this header.**

---

## [2026-04-02] — Swagger doc annotations, build fixes, and security audit

**Session context:** PR #44 (branch `robbie`) — fixing CI build failures and improving API documentation.

### Fixed

- `apps/backend/src/main/java/com/sait/peelin/config/UserIdKeyGenerator.java` — Missing `import org.springframework.security.core.GrantedAuthority` and `import java.util.stream.Collectors` caused compilation failure in CI.
- `apps/backend/src/test/java/com/sait/peelin/controller/v1/BakeryControllerTest.java` — Both `BakeryDto` constructor calls used 8 args; record gained a 9th field (`address`). Added `null` for `address` in both instantiations.
- `.github/workflows/backend-build.yml` — PR preview deploy step was failing with "secret env value must not be encrypted before app is created" because `.do/app.yaml` contains `EV[...]` encrypted secrets tied to the production app, which DigitalOcean cannot decrypt for a new ephemeral app. Removed the DO deploy + comment steps from the `pr-preview` job; job now builds and smoke-tests the Docker image only.

### Added

- `apps/backend/src/main/java/com/sait/peelin/controller/v1/*.java` — Added `@Operation`, `@ApiResponse`/`@ApiResponses`, `@SecurityRequirement`, `@Parameter`, and `@Tag(description)` annotations to all 21 controllers. Every endpoint now has a summary, description, documented response codes, and correct lock-icon visibility in Swagger UI.
- `docs/security-findings.md` — Security audit of all controllers and backing services. Two actionable findings (pending reviews exposed publicly; `markRead` missing ownership check) plus one informational note on employee thread access scope.

---

## [2026-03-28] — Auth foundation implementation (spec: docs/superpowers/specs/2026-03-28-auth-foundation-design.md)

**Session context:** Implementing the auth foundation feature from the approved spec. Branch: `feature/auth-foundation`.

### Fixed

- `apps/backend/src/main/java/com/sait/peelin/security/JwtAuthenticationFilter.java` — Inverted null-check was exiting early on valid Bearer tokens (the exact tokens that should be processed). Fixed condition from `if (authHeader != null && authHeader.startsWith("Bearer "))` to `if (authHeader == null || !authHeader.startsWith("Bearer "))`. Also added the missing `SecurityContextHolder.getContext().setAuthentication(authToken)` call — auth token was constructed but never applied, so every request ran as anonymous.
- `apps/backend/src/main/java/com/sait/peelin/service/CustomerDetailsService.java` — `orElseThrow()` threw `NoSuchElementException` instead of the expected `UsernameNotFoundException`, breaking Spring Security's authentication flow. Changed to `orElseThrow(() -> new UsernameNotFoundException(...))`.
- `apps/backend/src/main/java/com/sait/peelin/service/JwtService.java` — Three bugs: (1) `@Value("${app.jwt.secret")` and `@Value("${app.jwt.issuer")` were missing closing `}`, causing startup failure. (2) `isTokenExpired` was infinitely recursive — called itself via `String.valueOf(signedJWT)` which serializes back to the same token string. Fixed to read expiry directly from `getJWTClaimsSet().getExpirationTime()`. (3) `isTokenValid` called `isTokenExpired(String.valueOf(signedJWT))` instead of `isTokenExpired(token)`.

### Added

- `apps/backend/src/main/java/com/sait/peelin/dto/v1/auth/RegisterRequest.java` — DTO for registration: username, email, password, firstName, lastName, phone with validation annotations.
- `apps/backend/src/main/java/com/sait/peelin/repository/CustomerRepository.java` — Spring Data JPA repository for `Customer`.
- `apps/backend/src/main/java/com/sait/peelin/repository/RewardTierRepository.java` — Spring Data JPA repository for `RewardTier` with `findFirstByOrderByRewardTierMinPointsAsc()` for lowest-tier lookup on registration.
- `apps/backend/src/main/java/com/sait/peelin/service/OAuth2UserService.java` — Stub service for future OAuth2 login. Throws `UnsupportedOperationException` with a clear TODO comment.
- `apps/backend/src/main/resources/db/migration/V4__make_customer_address_nullable.sql` — Drops `NOT NULL` from `customer.address_id` so new customers can register without an address on file.
- `apps/frontend/src/hooks.server.ts` — SvelteKit server hook: reads `jwt` cookie, base64-decodes payload, normalizes role (strips `ROLE_` prefix, lowercases), populates `event.locals.user`. Enforces route protection: `/account/**` requires auth, `/employee/**` requires employee or admin, `/admin/**` requires admin.
- `apps/frontend/src/lib/api.ts` — Fetch wrapper that reads the `jwt` cookie, attaches `Authorization: Bearer` header on all requests, handles `401` by clearing the cookie and redirecting to `/login`. Exports `api.get`, `api.post`, `api.put`, `api.delete`.
- `docs/auth-contract.md` — Auth contract documentation covering endpoints, JWT claims, role/route mapping, error codes.

### Changed

- `apps/backend/src/main/java/com/sait/peelin/service/AuthService.java` — Added `register()` method (transactional: creates `User` + `Customer` atomically). Added `PasswordEncoder`, `CustomerRepository`, `RewardTierRepository` injections.
- `apps/backend/src/main/java/com/sait/peelin/controller/v1/AuthController.java` — Added `POST /register` (201 Created) and `POST /oauth2/callback` (501 stub) endpoints.
- `apps/backend/src/main/java/com/sait/peelin/model/Customer.java` — Removed `@NotNull`, `optional = false`, `nullable = false` from `address` field so the entity accepts null address (aligned with V4 migration).
- `apps/backend/pom.xml` — Added `spring-boot-starter-oauth2-client` dependency for OAuth2 skeleton.
- `apps/backend/src/main/resources/application.yaml` — Added commented-out OAuth2 configuration stub (Google + Microsoft). Added default fallback for `JWT_SECRET` (`dev-secret-key-for-local-development-only-32b`) so the app starts locally without the env var set.
- `apps/backend/src/main/resources/application-dev.yaml` — Added default fallbacks for `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` so the dev profile connects to `localhost:5432/peelin` without requiring env vars to be set manually.
- `apps/frontend/src/app.d.ts` — Uncommented and typed `Locals` interface with `user: { username, role } | null`.

### Migrations

- `V4__make_customer_address_nullable.sql` — `ALTER TABLE customer ALTER COLUMN address_id DROP NOT NULL`. Needed so customers can register without providing an address.

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
