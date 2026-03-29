# Auth Contract — Peelin' Good API

Single source of truth for all authentication details. Read this before building any feature that touches login, registration, or protected routes.

---

## Endpoints

| Endpoint | Auth required | Status | Description |
|---|---|---|---|
| `POST /api/v1/auth/login` | No | Implemented | Returns JWT |
| `POST /api/v1/auth/register` | No | Implemented | Creates user + customer, returns JWT |
| `POST /api/v1/auth/logout` | No | Frontend-only | Frontend clears cookie — no server state |
| `POST /api/v1/auth/oauth2/callback` | No | Stubbed (501) | OAuth2 skeleton, not yet implemented |

---

## Login

**Request**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "rob",
  "email": "rob@example.com",
  "password": "secret"
}
```

Either `username` or `email` can be used to identify the user. Both fields are accepted.

**Response (200)**

```json
{
  "token": "<jwt>",
  "username": "rob",
  "role": "customer"
}
```

---

## Registration

**Request**

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "rob",
  "email": "rob@example.com",
  "password": "secret",
  "firstName": "Rob",
  "lastName": "Smith",
  "phone": "403-555-0100"
}
```

**Response (201)**

Same shape as login — `{ token, username, role }`.

**Error responses**

| Status | Condition |
|---|---|
| `409 Conflict` | Username or email already registered |
| `400 Bad Request` | Validation failure (missing required field, invalid email format, etc.) |

---

## JWT

**Storage (web frontend):** regular cookie named `jwt` — accessible to SvelteKit SSR hooks and client-side JS.

**Header (all API calls):**

```http
Authorization: Bearer <token>
```

**Claims**

| Claim | Value |
|---|---|
| `sub` | username |
| `roles` | `["ROLE_CUSTOMER"]` / `["ROLE_EMPLOYEE"]` / `["ROLE_ADMIN"]` |
| `iss` | `peelin-good` |
| `exp` | epoch ms (default 10 days) |

**Algorithm:** HS256 (HMAC-SHA256)

---

## Roles and Route Access

### API routes

| Route prefix | Required role(s) |
|---|---|
| `/api/v1/auth/**` | Public |
| `/api/v1/products/**` | Public |
| `/api/v1/bakeries/**` | Public |
| `/actuator/**` | Public |
| `/api/v1/customer/**` | `CUSTOMER`, `EMPLOYEE`, or `ADMIN` |
| `/api/v1/employee/**` | `EMPLOYEE` or `ADMIN` |
| `/api/v1/admin/**` | `ADMIN` only |

### Frontend routes (enforced by `hooks.server.ts`)

| Route prefix | Requirement | Redirect if fails |
|---|---|---|
| `/account/**` | Any authenticated user | `/login` |
| `/employee/**` | `employee` or `admin` role | `/?error=forbidden` |
| `/admin/**` | `admin` role only | `/?error=forbidden` |
| All others | Public | — |

---

## Role Format

Spring Security emits roles as `ROLE_CUSTOMER`, `ROLE_EMPLOYEE`, `ROLE_ADMIN` (uppercase, with prefix).

The SvelteKit `hooks.server.ts` normalizes this to lowercase without prefix when populating `event.locals.user.role`:

```
"ROLE_CUSTOMER" → "customer"
"ROLE_EMPLOYEE" → "employee"
"ROLE_ADMIN"    → "admin"
```

---

## Error Codes

| Code | Meaning |
|---|---|
| `400` | Validation error — check request body |
| `401` | Missing or invalid/expired JWT |
| `403` | Authenticated but insufficient role |
| `409` | Duplicate username or email on register |
| `501` | OAuth2 endpoint stubbed, not implemented |

---

## What Is Not In Scope

- Full OAuth2 implementation (Google/Microsoft flows)
- Employee or admin registration (admin creates these accounts separately)
- Password reset / forgot password
- Token refresh
- Workshop5 desktop app — separate system with its own MySQL-backed auth
