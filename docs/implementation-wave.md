Here's the dependency map. I'll group by "wave" — everything in a wave can be done in parallel; each wave has a clear prerequisite.

## Wave 1 — Independent, can be done in any order (single-file, no coupling)

These are surgical edits to one file each. Safe to run in parallel.

| # | Fix | File(s) | Touches |
|---|---|---|---|
| A1 | Delete public `/test-error` & `/unhandled` (S6) | `SentryTest.java` | delete |
| A2 | Remove `scripts/stripe.exe`, add to `.gitignore` (H7) | `scripts/`, `.gitignore` | 1 add, 1 delete |
| A3 | `.dockerignore` for backend & frontend (H9) | `apps/*/Dockerfile`, new `.dockerignore` | 2 new |
| A4 | Move `docker-compose` password to `.env` (H8) | `docker-compose.yml`, `.env.example`, `.gitignore` | 3 edits |
| A5 | Open-redirect allowlist (C5) | `login/+page.server.ts`, `login/+page.svelte`, `auth/callback/+page.svelte` | 1 helper + 3 call-sites |
| A6 | Review: filter to `approved` on public endpoint (C7a) | `ReviewService.java` | 1 line |
| A7 | `ChatService.markRead` ownership check (C7b) | `ChatService.java` | 1 line |
| A8 | Chat unassigned-read explicit policy (C7c) | `ChatService.java` | small |
| A9 | `System.out.println` → SLF4J (H5) | `CustomerService.java`, `WelcomeEmailService.java`, `PasswordResetService.java` | line-level |
| A10 | `JWT_SECRET` fail-fast, no default (C3) | `application.yaml`, add startup guard | 2 edits |
| A11 | Jackson cache: drop `DefaultTyping.EVERYTHING` (C2) | `CacheConfig.java` | 1 method |
| A12 | `app.cookie.secure` default `true` (H1b) | `AuthController.java` or config | 1 line |
| A13 | Spring Security headers (HSTS/CSP/XFO/nosniff) (H3a) | `SecurityConfig.java` | 1 block |
| A14 | Sentry client hardening: `sendDefaultPii:false`, `replayIntegration({maskAllInputs:true,maskAllText:true,blockAllMedia:true})`, `beforeSend` redactor (S1/S2/S4 on FE) | `hooks.client.ts` | 1 file |
| A15 | Sentry server hardening: `sendDefaultPii:false`, `beforeSend` redactor, sampling ↓ (S1/S4/S7 on BE) | `application.yaml`, `application-prod.yaml` | 2 files |
| A16 | Sentry DSN → env var in all 3 places (S3) | `instrumentation.server.ts`, `application.yaml`, `application-prod.yaml` | 3 edits |

## Wave 2 — Independent of Wave 1, but each is multi-file / adds a new pattern

Start once Wave 1 is merged (so diffs don't conflict).

| # | Fix | Dependency | Notes |
|---|---|---|---|
| B1 | Server-side recompute of order totals (C6) | none | BE only; adds a `CheckoutPricingService` or hardens `OrderService.checkout`; ignore client-supplied prices |
| B2 | Stripe webhook: require signature, dedupe by `event.id`, verify `amount == order.grandTotal` (C4) | **B1 must land first** — amount check compares to authoritative total | new Flyway migration `stripe_processed_event(event_id PK, processed_at)` |
| B3 | Rate limiting on `/auth/*` (H2) | none | Valkey is already in compose — use bucket4j-redis or a Spring `OncePerRequestFilter` with a Lua counter |
| B4 | CI: pin all GH actions to SHA (H6a) | none | mechanical find-replace in 2 workflow files |
| B5 | CI: fork-PR isolation for preview deploys (H6b) | none | `if: github.event.pull_request.head.repo.full_name == github.repository` guard |
| B6 | Logback Sentry appender redaction filter (S8) | **A15** | keep redactor consistent with Wave 1 |
| B7 | Sentry `release` + `environment` + source-maps auth token wired in CI (S10/S11) | **A16** (so env var name is stable) | adds `SENTRY_RELEASE=${GITHUB_SHA}` and `SENTRY_ENVIRONMENT` to both workflows |

## Wave 3 — **Depends on a design decision first**

These are blocked on the same question: **is the frontend's SvelteKit server going to verify JWTs, or are we moving the auth boundary entirely to the Spring backend?**

| # | Fix | Blocked by |
|---|---|---|
| C1a | JWT signature verification in `hooks.server.ts` (C1) | design decision: shared `JWT_SECRET` vs JWKS endpoint (backend exposes `/.well-known/jwks.json`) |
| C1b | Remove `localStorage` user state; hydrate from server `locals.user` only (H1a) | **C1a** — server must be authoritative before client can drop its copy |
| C1c | Drive `Sentry.setUser` from verified `whoami`, not `localStorage` (S5) | **C1b** |
| C1d | Decide on CSRF model (H3b) — if cookies stay for auth, re-enable CSRF with `CookieCsrfTokenRepository` + SvelteKit `X-CSRF-Token` header on `apiClient` | **C1a**, because the cookie model may change |
| C1e | Centralized `RoleGuard` hook replacing per-layout checks (Arch #3) | **C1a** |

## Wave 4 — Larger, not blocking anything urgent (can run anytime after Wave 1)

- **Dependabot + CodeQL + Trivy** (M10) — add 3 workflow files, no code touched.
- **File-upload magic-byte validation** (M1).
- **N+1 fix in `ChatService.threadDto`** (M2).
- **Cache key collision fix** on `@Cacheable` by user UUID (M3).
- **OAuth2 success handler: drop PII from URL** (M4).
- **Remove parallel FE API clients** (M6) — consolidate `api.js` → `apiClient.ts`.

## Wave 5 — Architectural, multi-sprint
Monorepo #1 (OpenAPI + generated TS client), fat-service split, Result-type error model, shared contract tests. Park these.

---

### Recommended start

I'd open a single PR on `claude/security-debt-audit-78JW6` that knocks out **all of Wave 1** — it's ~16 small, reviewable commits, each with a clear one-liner subject. Then Wave 2 in a second PR, and Wave 3 only after we align on the JWT/CSRF design.

**One question before I start Wave 1:** for **A10** (`JWT_SECRET` fail-fast), do you want me to (a) remove the default entirely so the app refuses to start without the env var, or (b) keep the dev default but log a loud warning + fail only when `spring.profiles.active=prod`? Option (a) is stricter; (b) keeps local dev ergonomic. I'd pick (a).
