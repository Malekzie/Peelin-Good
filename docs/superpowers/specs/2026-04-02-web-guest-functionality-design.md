# Guest Functionality — Web

**Platform:** Web (SvelteKit frontend + Spring Boot backend)  
**Status:** Planning

---

## What it does

Allow unauthenticated users to browse products, view bakery locations, and place orders without creating an account. A guest session should feel seamless — cart persists locally, checkout prompts for contact info inline rather than redirecting to a registration wall.

---

## Scope

- Browse products and bakeries: already public — no change needed
- Guest cart: persisted in `localStorage` (no server-side cart session)
- Guest checkout: collect name, email, phone at checkout step — no account required
- Guest order tracking: order confirmation page shown immediately; no post-order access (no `/orders` history)
- Guest cannot access rewards, profile, or chat

---

## Backend changes

`POST /api/v1/orders` currently calls `orderService.listForCurrentUser()` which requires authentication. Checkout needs to accept an unauthenticated request with guest contact fields in the `CheckoutRequest` body.

Options:
1. Permit anonymous on `POST /api/v1/orders` and pass guest info in the request body (simplest)
2. Issue a short-lived guest JWT on first cart interaction (more consistent with auth model)

**Recommendation:** Option 1 — add `guestName`, `guestEmail`, `guestPhone` fields to `CheckoutRequest`, skip user association when caller is anonymous.

Affected backend files:
- `SecurityConfig.java` — permit `POST /api/v1/orders` for anonymous
- `CheckoutRequest.java` — add optional guest fields
- `OrderService.checkout()` — handle null principal, attach guest fields to order
- `Order` model / migration — add nullable `guest_name`, `guest_email`, `guest_phone` columns

---

## Frontend changes

- Cart store: move from session to `localStorage`
- `hooks.server.ts`: guest routes must not redirect to login
- Checkout page: conditional guest info form when `locals.user` is null
- No changes to `/account`, `/orders` — those stay auth-gated

---

## Open questions

- Should guest orders be claimable if the guest later registers with the same email?
- Rate limiting on anonymous checkout to prevent abuse?
