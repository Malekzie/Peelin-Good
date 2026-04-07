# Reward Tier Threshold Visual Indicator — Mobile

**Platform:** Android (Workshop06, Kotlin)  
**Status:** Planning

---

## What it does

When a customer's points cross a reward tier threshold (e.g. Silver → Gold), show a clear visual moment — an animation, banner, or modal — so the achievement feels rewarding rather than silent.

---

## Scope

- Triggered when: points total crosses a `RewardTier.rewardTierMinPoints` boundary
- Shown on the rewards screen after an order is completed
- One-time display per tier upgrade (not shown again on revisit)

---

## Backend dependency

Requires the rewards summary endpoint from `2026-04-02-mobile-rewards-flow-fix-design.md`:
- `GET /api/v1/customers/{id}/rewards/summary` → `{ totalPoints, currentTier, pointsToNextTier, previousTier? }`
- The response should include whether a tier upgrade just occurred, OR the app can detect it by comparing current tier against locally stored previous tier

**Recommended approach:** Store `lastKnownTier` in SharedPreferences. After fetching summary, compare — if tier changed, trigger the celebration UI.

---

## Android changes

- `RewardsFragment` / `RewardsViewModel` — compare fetched tier to `lastKnownTier` in SharedPreferences
- On tier upgrade detected:
  - Show celebratory animation (Lottie or custom) with new tier name
  - Update `lastKnownTier` in SharedPreferences after display
- Progress bar on rewards screen showing points toward next tier (always visible, not just on upgrade)

---

## Open questions

- What animation style — full-screen overlay, bottom sheet, or inline card?
- Should there be a sound effect (opt-in)?
- First launch: initialize `lastKnownTier` without triggering the celebration
