# Rewards Flow Fix — Mobile

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

The current rewards/points calculations are inconsistent or incorrect. This spec covers auditing and fixing the earn→tier→redeem flow end-to-end.

---

## Known issues to audit

1. Points ratio (how many points per dollar) — separate spec: `2026-04-02-mobile-change-points-ratio-design.md`
2. Tier threshold crossing logic — separate spec: `2026-04-02-mobile-reward-tier-threshold-indicator-design.md`
3. Points displayed on mobile may not match backend values
4. Reward redemption flow may not deduct points correctly

---

## Scope for this spec

Fix the core earn/redeem calculation in the backend so all clients (web, mobile, desktop) are consistent:

- **Earn:** Points awarded = `orderSubtotal × pointsPerDollar` (rounded down), applied on order status → `COMPLETED`
- **Tier upgrade:** Customer tier recalculated after each points earn event — check if cumulative points cross any `RewardTier.rewardTierMinPoints` threshold
- **Display:** `GET /api/v1/customers/{id}/rewards` returns accurate running total

---

## Backend areas to audit

- `OrderService` — where/when are rewards created after order completion?
- `RewardQueryService` — is cumulative total calculated or just raw list returned?
- `RewardTierService` — is tier upgrade triggered automatically or manually?
- Check: is there a `CustomerRewardSummaryDto` or does the frontend sum raw reward entries itself?

**If no summary endpoint exists, add one:**
`GET /api/v1/customers/{id}/rewards/summary` → `{ totalPoints, currentTier, pointsToNextTier }`

---

## Android changes

- Rewards screen: display data from the summary endpoint rather than summing locally
- Ensure points displayed match backend total after every order

---

## Open questions

- Are points ever manually adjusted by admins? If so, is there an adjustment endpoint?
- What happens to points if an order is refunded/cancelled?
