# Admin API Logging — Consistent Across All Programs

**Platform:** Spring Boot backend + Desktop (Workshop5) + Mobile (Workshop06)  
**Status:** Planning

---

## What it does

Every admin action (create, update, delete, status change) should be logged to a central, queryable log. The log should be consistent in format across the web API and the desktop app, and searchable with severity levels.

Covers meeting items:
- "Logging admin hits API and is consistent over all programs"
- "Logging search" (Desktop)
- "Severity levels to logging" (Desktop)

---

## Scope

- Structured audit log for admin/employee actions
- Stored in the database (queryable) rather than only in log files
- Log entries include: timestamp, user, role, action, target entity, severity, result (success/failure)
- Searchable via admin UI (web + desktop)

---

## Backend changes (Workshop7)

**Migration — new `audit_log` table:**
```sql
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id     UUID REFERENCES users(user_id),
    username    VARCHAR(255),
    role        VARCHAR(50),
    severity    VARCHAR(20) NOT NULL,  -- INFO, WARN, ERROR
    action      VARCHAR(100) NOT NULL, -- e.g. ORDER_STATUS_CHANGED
    entity_type VARCHAR(100),          -- e.g. Order
    entity_id   VARCHAR(255),
    detail      TEXT,
    source      VARCHAR(50)            -- API, DESKTOP, MOBILE
);
```

**New service: `AuditLogService`**
- `log(userId, action, entityType, entityId, severity, detail, source)`
- Called from service layer, not controllers

**New endpoint:**
`GET /api/v1/admin/audit-log?search=&severity=&from=&to=&page=`  
Returns paginated log entries. Requires ADMIN role.

**Where to call `AuditLogService.log()`:**
- `OrderService.updateStatus()` — WARN if status is regressed
- `CustomerService.patch()`, `approvePhoto()`, `rejectPhoto()`
- `AdminUserService.setActive()`
- `BakeryService.create/update/delete()`
- `EmployeeAdminService.create/update/delete()`

---

## Desktop changes (Workshop5)

- Replace current `Log.txt` file-based logging with HTTP calls to `POST /api/v1/admin/audit-log` (or keep file as local backup + push to API)
- Add search field to log viewer screen
- Add severity filter (INFO / WARN / ERROR) to log viewer

---

## Mobile changes (Workshop06)

- Admin/employee actions (order status changes) call the same backend log endpoint automatically via the API — no extra mobile code needed

---

## Open questions

- Should the desktop app call the API directly, or continue writing locally and sync periodically?
- Retention policy: how long are log entries kept?
- Integrate Sentry for ERROR-level entries? (see `2026-04-02-desktop-sentry-design.md`)
