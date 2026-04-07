# Employee Log Visibility — Desktop

**Platform:** Desktop (Workshop5, JavaFX)  
**Status:** Planning

---

## What it does

Employees should only see log entries they created. Currently all employees can see all logs.

---

## Scope

- Admin: sees all log entries
- Employee: sees only entries where `userId` matches their own session

---

## Desktop changes (Workshop5)

The current log viewer reads from `Log.txt` via `LogData`. Once the shared audit log backend is in place (see `2026-04-02-shared-api-logging-design.md`), this filter is applied at the API query level.

**Short-term (before API log):**
- Filter `Log.txt` entries by the current `UserSession.userId` when role is EMPLOYEE
- `LogData` or the log DAO needs to accept an optional `userId` filter

**Long-term (after API log):**
- `GET /api/v1/admin/audit-log?userId={self}` — backend enforces employee can only query their own entries

---

## Open questions

- Is the current log keyed by userId, or only by username/timestamp?
- Is this change needed before the meeting with Sam?
