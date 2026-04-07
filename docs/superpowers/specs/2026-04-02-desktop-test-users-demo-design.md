# Test Users for Sam Demo — Desktop

**Platform:** Desktop (Workshop5, JavaFX) + Database  
**Status:** Planning — time-sensitive (needed before meeting)

---

## What it does

Provision a set of known test users (admin + employee roles) that can be reliably used during the demo with Sam. Ensures the demo doesn't depend on live data or require manual setup steps.

---

## Scope

- At least 1 ADMIN user and 2-3 EMPLOYEE users with known credentials
- Employees assigned to specific bakeries
- Enough order/log data to make screens look populated
- Reproducible: runnable SQL script that can reset to a clean demo state

---

## Implementation

**Option A — Extend `V3__seed_data.sql`** (if not yet run in the demo DB)
- Add demo user INSERT statements with BCrypt-hashed passwords

**Option B — New script `demo_reset.sql`** (preferred — doesn't touch Flyway migrations)
- `DELETE` existing test users by email pattern (e.g. `%@demo.peelin.com`)
- Re-insert fresh demo users, employees, and bakery assignments
- Run manually before each demo: `psql -f demo_reset.sql`

**Credentials to create:**

| Username | Email | Password | Role | Bakeries |
|---|---|---|---|---|
| `admin` | `admin@demo.peelin.com` | `Demo1234!` | ADMIN | All |
| `alice` | `alice@demo.peelin.com` | `Demo1234!` | EMPLOYEE | Bakery 1 |
| `bob` | `bob@demo.peelin.com` | `Demo1234!` | EMPLOYEE | Bakery 2 |

BCrypt hash `Demo1234!` and include directly in the SQL.

**Workshop5 — `GenerateTestUserSQL.java`** already exists for this purpose. Run it and add output to the demo script.

---

## Open questions

- Does Sam need customer accounts as well, or just admin/employee?
- Should test orders and log entries be seeded too for populated screens?
- What database is the demo running against — local or DigitalOcean?
