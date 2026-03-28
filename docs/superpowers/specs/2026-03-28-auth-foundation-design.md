# Auth Foundation Design

**Date:** 2026-03-28
**Branch:** `feature/auth-foundation`
**Owner:** Robbie

---

## Overview

This document specifies the authentication and security foundation for the Peelin' Good platform. It covers backend JWT auth, user registration, frontend route protection, role enforcement, and an OAuth2 skeleton.

The system serves two API clients: the SvelteKit web frontend and the Android mobile app (Workshop06). The Workshop5 desktop app is a separate tier that connects directly to MySQL and does not use this API.

---

## 1. Scope

### Fix (existing broken code)

| File | Issue |
|---|---|
| `security/JwtAuthenticationFilter.java` | Inverted null-check exits early on valid Bearer tokens; `SecurityContextHolder.setAuthentication()` is never called — auth never actually applied |
| `service/CustomerDetailsService.java` | Bare `orElseThrow()` throws `NoSuchElementException` instead of `UsernameNotFoundException` |

### Build new

| Item | Description |
|---|---|
| `POST /api/v1/auth/register` | Registration endpoint — creates `User` + linked `Customer` |
| `RegisterRequest` DTO | username, email, password, firstName, lastName, phone |
| `V4__make_customer_address_nullable.sql` | Allows deferred address on registration |
| `hooks.server.ts` | SvelteKit server hook — auth check + role-based route guard |
| `app.d.ts` update | Typed `locals.user` |
| `$lib/api.ts` | Fetch wrapper that auto-attaches `Authorization: Bearer` header |
| OAuth2 skeleton | Dependency + config stub + service stub + stubbed endpoint |
| `docs/auth-contract.md` | Single source of truth for teammates building against auth |

---

## 2. Registration Flow

**Endpoint:** `POST /api/v1/auth/register`

**Request body:**
```json
{
  "username": "rob",
  "email": "rob@example.com",
  "password": "secret",
  "firstName": "Rob",
  "lastName": "Smith",
  "phone": "403-555-0100"
}
```

**Response (201):** Same shape as login — `{ token, username, role }`

**`AuthService.register()` steps:**
1. `existsByUsername` → throw `409 Conflict` if taken
2. `existsByUserEmail` → throw `409 Conflict` if taken
3. BCrypt the password
4. Save `User` with `userRole = customer`
5. Query `RewardTier` ordered by `rewardTierMinPoints ASC`, take first (lowest tier)
6. Save `Customer` with `userId`, `rewardTierId`, `addressId = null`, and provided name/phone/email fields
7. Generate JWT via `JwtService`
8. Return `AuthResponse(token, username, "customer")`

**Migration:**
```sql
-- V4__make_customer_address_nullable.sql
ALTER TABLE customer ALTER COLUMN address_id DROP NOT NULL;
```

---

## 3. JWT Storage and API Auth (Bearer token)

All API clients authenticate using `Authorization: Bearer <token>`.

### Web frontend flow

1. `POST /api/v1/auth/login` or `/register` → Spring returns JWT in response body
2. SvelteKit stores JWT in a regular (non-httpOnly) cookie named `jwt` via `document.cookie`
   - Accessible to both SvelteKit server-side hooks and client-side JS
   - Expires to match JWT expiry
3. `$lib/api.ts` reads the `jwt` cookie and attaches `Authorization: Bearer <token>` on every fetch to the API
4. On logout: delete the `jwt` cookie

### `$lib/api.ts` responsibilities

- Wrap `fetch` with auto-attached `Authorization` header
- Handle `401` responses by clearing the cookie and redirecting to `/login`
- Export typed helper functions: `api.get()`, `api.post()`, `api.put()`, `api.delete()`

---

## 4. Frontend Route Protection

### `hooks.server.ts`

Runs on every server-side request. Logic:

1. Read `jwt` cookie from `event.cookies`
2. Base64-decode the JWT payload (middle segment) — extract `sub` (username) and `roles`
3. Normalize role: strip `ROLE_` prefix, lowercase → `"ROLE_CUSTOMER"` becomes `"customer"`
4. Set `event.locals.user = { username, role }` or `null` if cookie absent/malformed
5. Check route against protection rules (see table below)
6. Redirect as appropriate

### Route protection rules

| Route prefix | Requirement | Redirect if fails |
|---|---|---|
| `/account/**` | Any authenticated user | `/login` |
| `/employee/**` | `employee` or `admin` role | `/?error=forbidden` |
| `/admin/**` | `admin` role only | `/?error=forbidden` |
| All others | Public | — |

### `app.d.ts` addition

```ts
declare global {
  namespace App {
    interface Locals {
      user: { username: string; role: 'admin' | 'employee' | 'customer' } | null;
    }
  }
}
```

---

## 5. OAuth2 Skeleton

**Dependency:** Add `spring-boot-starter-oauth2-client` to `pom.xml`

**`application.yaml` stub** (commented out):
```yaml
# spring:
#   security:
#     oauth2:
#       client:
#         registration:
#           google:
#             client-id: ${GOOGLE_CLIENT_ID}
#             client-secret: ${GOOGLE_CLIENT_SECRET}
#             scope: openid, email, profile
#           microsoft:
#             client-id: ${MICROSOFT_CLIENT_ID}
#             client-secret: ${MICROSOFT_CLIENT_SECRET}
#             scope: openid, email, profile
```

**`OAuth2UserService.java`** stub:
- Annotated `@Service`
- Method signature: `AuthResponse processOAuth2User(String provider, OAuth2User oAuth2User)`
- Body: `// TODO: look up or create User by email, assign customer role, generate JWT`

**`AuthController` endpoint stub:**
```
POST /api/v1/auth/oauth2/callback
→ 501 Not Implemented
→ body: "OAuth2 login is scaffolded but not yet implemented"
```

---

## 6. Auth Contract

See `docs/auth-contract.md` (written as part of this feature).

Summary:

| Endpoint | Auth required | Description |
|---|---|---|
| `POST /api/v1/auth/login` | No | Returns JWT |
| `POST /api/v1/auth/register` | No | Creates user + customer, returns JWT |
| `POST /api/v1/auth/logout` | No | Frontend clears cookie (no server state) |
| `POST /api/v1/auth/oauth2/callback` | No | Stubbed — 501 |

**JWT claims:**
- `sub`: username
- `roles`: `["ROLE_CUSTOMER"]` / `["ROLE_EMPLOYEE"]` / `["ROLE_ADMIN"]`
- `iss`: `peelin-good`
- `exp`: epoch ms (default 10 days)

**Error responses:**
- `401` — missing or invalid token
- `403` — authenticated but wrong role
- `409` — duplicate username or email on register

---

## 7. What Is Not In Scope

- Full OAuth2 implementation (Google/Microsoft flows, redirect handling, token exchange)
- Employee or admin registration (admin creates these accounts separately)
- Password reset / forgot password flow
- Token refresh
- Workshop5 desktop app — separate system, separate database, separate auth

---

## 8. Completion Criteria

- [ ] `contextLoads` test passes (CI green)
- [ ] `POST /api/v1/auth/register` creates a `User` and `Customer` in one transaction
- [ ] `POST /api/v1/auth/login` returns a valid JWT
- [ ] JWT filter correctly populates `SecurityContext` on authenticated requests
- [ ] Role-based API routes (`/admin/**`, `/employee/**`, `/customer/**`) reject wrong roles with `403`
- [ ] SvelteKit `hooks.server.ts` redirects unauthenticated users away from protected routes
- [ ] `employee`/`admin` frontend routes reject `customer` role
- [ ] `$lib/api.ts` auto-attaches Bearer token on all API calls
- [ ] `docs/auth-contract.md` written and accurate
- [ ] OAuth2 skeleton present with clear `// TODO` markers
