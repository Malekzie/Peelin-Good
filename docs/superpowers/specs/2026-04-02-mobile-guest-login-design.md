# Guest Login — Mobile

**Platform:** Android (Workshop06, Kotlin)  
**Status:** Planning

---

## What it does

Allow users to browse and place orders on the Android app without creating an account. Mirrors the web guest functionality requirement.

---

## Scope

- "Continue as Guest" option on the login/register screen
- Guest can browse products and bakeries, add to cart, and checkout with name/email/phone
- No rewards, no order history, no profile screen for guests
- Guest state is local only — no guest JWT issued

---

## Backend dependency

Requires the web guest checkout backend change first:
- `POST /api/v1/orders` must accept anonymous requests with guest contact fields in the body
- See `docs/superpowers/specs/2026-04-02-web-guest-functionality-design.md`

---

## Android changes

- `LoginActivity` / `LoginFragment` — add "Continue as Guest" button
- App state / `UserSession` — add `isGuest` flag; gate authenticated screens (rewards, profile, order history) on this flag
- Cart: persist locally (Room or SharedPreferences) rather than server-side
- Checkout screen: show guest info form (name, email, phone) when `isGuest = true`
- Bottom nav: hide or disable Profile and Rewards tabs for guests

---

## Open questions

- Should guest be prompted to create an account after placing an order?
- Cart persistence: cleared on app close or retained across sessions?
