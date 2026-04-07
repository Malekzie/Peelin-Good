# Change Points Ratio — Mobile + Backend

**Platform:** Spring Boot backend (affects all clients)  
**Status:** Planning

---

## What it does

Adjust the rate at which customers earn loyalty points per dollar spent. Currently the ratio may be hardcoded or incorrectly configured.

---

## Scope

- Points per dollar (e.g. 1 point per $1, or 10 points per $1) should be configurable without a code deploy
- Change should take effect for new orders only — do not retroactively recalculate past rewards

---

## Backend changes

**Current state:** Audit `OrderService` to find where points are calculated. Likely hardcoded.

**Target state:**
- Add `rewards.points-per-dollar` to `application.yaml` (e.g. `10`)
- Inject via `@Value("${rewards.points-per-dollar}")` in `OrderService` or a new `RewardsConfig` component
- Points earned = `floor(orderSubtotal × pointsPerDollar)`

**No migration needed** unless the ratio is to be stored in the DB (admin-configurable via UI). For now, config file is sufficient.

---

## Open questions

- Should the ratio be admin-configurable through the UI, or is a config file change acceptable for the demo?
- Are there bonus multipliers (e.g. double points on weekends) in scope?
