# Fix CRUD for New Tables — Desktop

**Platform:** Desktop (Workshop5, JavaFX)  
**Status:** Planning

---

## What it does

The desktop app's CRUD operations are out of sync with schema changes made since the initial build. New tables or columns added via migrations are not reflected in the DAOs or UI.

---

## Scope

Audit which tables have changed since the last desktop update and bring the DAOs and controllers up to date.

---

## What to audit

1. Compare `V1__baseline.sql` and any subsequent migrations against current Workshop5 DAO implementations
2. Identify: new tables with no DAO, renamed columns causing query failures, new columns not shown in UI
3. Known likely gaps (based on recent Workshop7 migrations):
   - `reviews` table changes (status enum, approval date)
   - `audit_log` table (new — needs read DAO if desktop is to display it)
   - Any `V4+` migrations added after Workshop5 was last updated

---

## Desktop changes

For each gap found:
- Add or update DAO (`*DAO.java`) with correct SQL
- Update controller to use new fields
- Update FXML view to display new columns where relevant

---

## Open questions

- Which specific migrations post-date the last desktop sync? Run `git log --follow` on migration files vs desktop DAOs to find the cutoff.
- Is the desktop connecting to the same PostgreSQL DB as the backend, or MySQL? (CLAUDE.md says Workshop5 uses MySQL — schema divergence may be intentional)
