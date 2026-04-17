# Workshop7 — Security & Tech-Debt Audit
Branch: `claude/security-debt-audit-78JW6` · Date: 2026-04-16

Three parallel audits (backend / frontend / monorepo+CI) completed. Below is the consolidated, deduplicated, prioritized report. Findings that the agents flagged but that I've downgraded after cross-checking (e.g. the "SQL injection in AnalyticsService" which is actually parameterized, and the Sentry DSN classed as CRITICAL — DSNs are public keys, so it's really Low/Hygiene) are noted inline.

---

## 🔴 CRITICAL — fix before next deploy

### C1. JWT signature never verified on the frontend
`apps/frontend/src/hooks.server.ts:18` decodes the JWT with `JSON.parse(atob(jwt.split('.')[1]))` and trusts `payload.role` / `payload.exp` — **no HMAC/RSA verification**. Anyone who can set the `token` cookie (XSS, MITM on http, a compromised sub-domain, dev tooling) can forge a JWT claiming `role: admin` and the SvelteKit server will honor it in `locals.user`.
**Fix:** verify with `jose`/`jsonwebtoken` using the backend's shared secret / JWKS. Treat the frontend hook as an authorization gate, not a display-only parser.

### C2. Jackson `DefaultTyping.EVERYTHING` in Redis cache (deserialization RCE)
`apps/backend/src/main/java/com/sait/peelin/config/CacheConfig.java:37-43` configures the cache's `ObjectMapper` with `enableDefaultTyping(…, EVERYTHING, …)` and a `BasicPolymorphicTypeValidator` whose allowlist is `Object.class`. That allowlist is a no-op and re-enables the Jackson gadget-chain RCE class (CVE-2017-7525 and successors). Any path that can poison a cache key → RCE.
**Fix:** drop default typing or switch to `NON_FINAL` + an explicit per-class allowlist (domain DTOs only). Prefer Jackson `@JsonTypeInfo` on specific hierarchies.

### C3. JWT secret has an insecure default
`apps/backend/src/main/resources/application.yaml:80`: `secret: ${JWT_SECRET:dev-secret-key-for-local-development-only-32b}`. A missing env var silently falls back to a publicly-known secret — a misconfigured prod boot means any attacker can mint admin JWTs.
**Fix:** remove the default; fail-fast on startup if `JWT_SECRET` is unset. Add a length/entropy check (≥32 random bytes).

### C4. Stripe webhook: missing signature enforcement, idempotency, and amount check
`apps/backend/src/main/java/com/sait/peelin/controller/v1/StripeWebhookController.java:56-81`.
- If `STRIPE_WEBHOOK_SECRET` is unset the handler logs a warning and still processes the event — unsigned webhook events are trusted.
- No `stripe_event_id` idempotency table — replays double-credit rewards / double-send emails.
- `paymentIntent.amount` is never compared to `order.grandTotal` — a forged event can mark a $0.50 charge as paying a $50 order.
**Fix:** reject unsigned webhooks (401), persist processed `event.id` UUIDs, and assert `event.amount == order.grandTotal` before fulfilment.

### C5. Open redirect on login `redirectTo`
`apps/frontend/src/routes/login/+page.server.ts:9` and `/login/+page.svelte:82` + `/auth/callback/+page.svelte:25` feed `redirectTo` straight into `redirect()` / `goto(resolve(...))`. `redirectTo=https://evil.com` phishes authenticated users.
**Fix:** accept only values matching `/^\/(?!\/)[A-Za-z0-9\-_/]*$/` (leading `/`, no `//`, no `:`). Reject everything else to `/`.

### C6. Order totals computed/trusted from the client cart
`apps/frontend/src/routes/checkout/+page.svelte:373-393` posts `$cart.items` along with display totals; there is no evidence the backend recomputes `grandTotal` against current DB prices + active specials before creating the Stripe PaymentIntent. Combined with C4 this is a full price-tampering path.
**Fix:** server-side recompute of subtotal, discount, delivery fee, tax, grandTotal from the DB; ignore any client-supplied price field; compare to `paymentIntent.amount` on fulfilment.

### C7. Three previously-documented findings still unfixed
`docs/security-findings.md` (2026-04-02) — none of the three are patched on this branch:
- `ReviewService.forProduct()` leaks `pending`/`rejected` reviews publicly.
- `ChatService.markRead()` missing `assertCanAccessThread()`.
- `assertCanAccessThread()` lets any employee read unclaimed threads.

---

## 🟠 HIGH

### H1. `localStorage` auth state + no HttpOnly/SameSite discipline
`apps/frontend/src/lib/stores/authStore.js:5-38` persists user identity to `localStorage` and `apps/frontend/src/hooks.server.ts:14` + `/auth/local-logout/+server.ts` set/clear the `token` cookie without explicit `Secure`, `HttpOnly`, `SameSite=Strict`. Backend side, `AuthController.java:59-70` has `@Value("${app.cookie.secure:false}")` — defaults to non-Secure. XSS → full account takeover.
**Fix:** cookies set exclusively by the backend with `HttpOnly; Secure; SameSite=Strict`; remove user objects from `localStorage`; default `app.cookie.secure` to `true`.

### H2. No rate limiting on `/auth/login`, `/auth/register`, password-reset
Controllers in `apps/backend/.../controller/v1/AuthController.java` + `PasswordResetService` have no bucket/throttle — brute-force and user-enumeration wide open.
**Fix:** bucket4j or Redis-backed per-IP + per-username limits (e.g. 5/15min on login, 3/hour on password-reset), with lockout escalation.

### H3. Spring Security: CSRF disabled globally + no security headers
`apps/backend/.../security/SecurityConfig.java:33` `.csrf(csrf -> csrf.disable())` plus no `headers()` config → no HSTS, CSP, `X-Frame-Options`, `X-Content-Type-Options`. Frontend also has no CSP in `hooks.server.ts`/`svelte.config.js`.
**Fix:** keep CSRF disabled only for pure bearer-token APIs but verify you are one (cookies in play here mean CSRF is live); add `.headers(h -> h.contentSecurityPolicy("default-src 'self'; script-src 'self' https://js.stripe.com …").frameOptions(DENY).hsts(…))`.

### H4. `send-default-pii: true` on both Sentry integrations
`apps/backend/src/main/resources/application.yaml:105` and `apps/frontend/src/hooks.client.ts:26`. Ships emails, IPs, Authorization headers to a third party with no consent — GDPR/CCPA exposure.
**Fix:** `sendDefaultPii: false`; implement a `beforeSend` redactor for `Authorization`, `Cookie`, `email`, `password`.

### H5. System.out.println logging PII
`CustomerService.java:421`, `WelcomeEmailService`, `PasswordResetService` emit customer email / userId via `System.out` → unfiltered container logs.
**Fix:** SLF4J with masked formatters; fail the build on `println` via Checkstyle/SpotBugs.

### H6. CI secrets exposed to fork PRs & unpinned actions
`.github/workflows/backend-build.yml` & `frontend-build.yml` run `digitalocean/app_action/deploy@v2` inside the `pull_request` job with `DIGITALOCEAN_ACCESS_TOKEN`, and all `uses:` are pinned by tag (`@v4`, `@v3.3.0`, `@v6.5.0`, `@v7`) not by SHA. A forked PR + a compromised action version = infra token exfiltration.
**Fix:** gate the preview-deploy step behind `if: github.event.pull_request.head.repo.full_name == github.repository`; use GitHub Environments with required approval; pin every action to a full SHA.

### H7. `scripts/stripe.exe` — 27 MB unsigned Windows binary committed
`scripts/stripe.exe`. Not verifiable, inflates every clone, ideal supply-chain vector.
**Fix:** `git rm`, add to `.gitignore`, document `stripe` CLI install via Homebrew/winget or download-with-checksum in a setup script.

### H8. `docker-compose.yml` plaintext `POSTGRES_PASSWORD: Password1`
`docker-compose.yml:7`. Dev-only intent, but credentials are in git history and the compose file exposes `5432` and `6379` on host.
**Fix:** move to `.env` + `env_file:`; bind only to `127.0.0.1`.

### H9. No `.dockerignore` in either app
`apps/backend/Dockerfile`, `apps/frontend/Dockerfile` both `COPY . .` with no `.dockerignore`, so `.git/`, `.env*`, `node_modules/`, IDE files bleed into build context (and image layers if stages are refactored).
**Fix:** add `.dockerignore` to each app with `.git`, `.env*`, `node_modules`, `target`, `build`, `coverage`, `.mvn/wrapper/maven-wrapper.jar` (keep source binaries out entirely).

---

## 🟡 MEDIUM

| # | Finding | Location |
|---|---|---|
| M1 | File upload validates only `Content-Type` header, not magic bytes — trivial to spoof | `CustomerService.java:399-401` |
| M2 | N+1 in `ChatService.threadDto()` — `customerLookupCacheService.findByUserId` per thread | `ChatService.java:272-292` |
| M3 | `@Cacheable` keys built from `SecurityContextHolder.getPrincipal()` username → collisions if users can rename; no matching `@CacheEvict` on updates | `OrderService.java:86` and rewards services |
| M4 | OAuth2 success handler appends `username`, `role`, `userId` to the redirect URL (browser history, Referer, access logs leak) | `AuthController.java:190-193` |
| M5 | STOMP WebSocket endpoints `permitAll()`; no per-thread authorization review done | `SecurityConfig.java:66`, `StompChannelInterceptor.java` |
| M6 | Two parallel frontend API clients (`apiClient.ts` typed vs `api.js` untyped); inconsistent error handling | `src/lib/utils/` |
| M7 | No token-refresh path; 401 → hard redirect mid-session | `src/lib/utils/apiClient.ts:13-26` |
| M8 | Sentry DSN hardcoded (low security impact but tightly couples deploy to one Sentry org) | `application.yaml:102`, `instrumentation.server.ts:4` |
| M9 | `.do/app.yaml:114-122` ships empty Stripe env entries with TODOs; a clean deploy will silently run with dev placeholder payment IDs | `.do/app.yaml` |
| M10 | No CodeQL / Dependabot / Trivy / `npm audit` in CI | `.github/workflows/` |
| M11 | Missing DB indexes on hot filters (`batch(bakery_id, start_date, end_date)`, review status, chat thread status) likely causing seq scans | Flyway migrations |
| M12 | 847-, 822-, 552-line `.svelte` route files mixing API, timezone, cart, and payment logic | `routes/profile/edit/+page.svelte`, `routes/register/+page.svelte`, `routes/checkout/+page.svelte` |

---

## 🟢 LOW / Hygiene
- No JWT refresh tokens — fixed 10-day access tokens force a long-lived bearer window.
- Cart persisted in `localStorage` (non-sensitive but tamperable).
- `vite.config.ts` hardcodes `http://localhost:8080` proxy.
- Postman collection uses literal seeded passwords (`Admin123!`) — fine for an example but should be `{{adminPassword}}`.
- Missing accessibility labels on checkout/profile; no `HEALTHCHECK` in compose frontend service.
- `any` casts around Stripe elements and testimonial data defeat TS.

---

## 🏗 Architectural / Tech-debt (cross-cutting)

1. **Not actually a monorepo.** Root `package-lock.json` is an 88-byte stub; no workspaces; no shared-type package. FE/BE drift is guaranteed — every DTO is hand-mirrored.
   **Proposed:** emit OpenAPI from Spring (`springdoc-openapi`) and generate a TS client (`openapi-typescript-codegen`) into `packages/api-client/`, consumed by the frontend. Kills M6 as well.

2. **Fat services.** `CustomerService` ~729 lines, `OrderService` ~710 lines mix profile, photo upload, cache, reward, auth-linking concerns.
   **Proposed:** split by aggregate (`CustomerProfileService`, `CustomerPhotoService`, `CustomerRewardLinkService`) with a thin façade if callers rely on the umbrella.

3. **Authorization is decentralised.** Role checks live in controllers via `@PreAuthorize`, again in services, and a third time in frontend `+layout.server.ts` files. Any of the three being forgotten is a breach.
   **Proposed:** a single `RoleGuard` filter in `hooks.server.ts` maps path globs → required roles; backend keeps `@PreAuthorize` as the source of truth and tests each controller for a "no annotation = fail" convention.

4. **Error-handling inconsistency.** Backend's `GlobalExceptionHandler` catches `Exception.class` generically; frontend mixes `throw` and `{ok:boolean}` return shapes.
   **Proposed:** define domain exceptions (`NotFound`, `Forbidden`, `InvalidState`, `ConflictingUpdate`) → RFC 7807 `application/problem+json` on the server, and a `Result<T,E>` discriminated union on the client.

5. **Tests are anemic.** ~17 test classes / 61 methods for a 252-file codebase, several `@Disabled`. No Stripe webhook integration test, no auth flow end-to-end.
   **Proposed:** add Testcontainers-based integration tests for `/auth/*`, `/checkout`, `/webhooks/stripe`; wire `jacoco-maven-plugin` into `./mvnw verify` and fail CI under 60% line coverage on `service/` and `controller/`.

6. **No shared contract tests between FE & BE.** `docs/auth-contract.md` exists but nothing enforces it.
   **Proposed:** Pact or a small Playwright login+checkout smoke that runs against a Testcontainers backend in CI.

7. **Mail & Stripe config fails silently.** Missing `MAIL_USERNAME` / `STRIPE_*` → warnings, then degraded runtime.
   **Proposed:** fail-fast at `ApplicationReadyEvent` if required keys are missing in `prod`/`staging` profiles.

---

## Recommended sequencing

| Window | Work |
|---|---|
| **This week (release-blocker)** | C1, C2, C3, C4, C5, C6, C7, H6 |
| **Sprint +1** | H1-H5, H7-H9, M1, M3, M7, M9 |
| **Sprint +2** | M2, M4, M5, M8, M10-M12, Architectural #1 (OpenAPI + generated client), #3 (central RoleGuard) |
| **Backlog** | All Lows, Architectural #2, #4, #5, #6, #7 |

Say the word and I'll start on any of these — I'd suggest opening with C1–C4 on this branch (they're compact, high-leverage, and each has a clear unit-test target).