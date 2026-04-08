# Delivery Times — 30-Minute Intervals

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

At checkout, customers selecting delivery choose from a list of available time slots in 30-minute increments (e.g. 10:00, 10:30, 11:00…) rather than a free-text or unrestricted time picker.

---

## Scope

- Only applies to orders with `orderMethod = DELIVERY`
- Slots generated from bakery operating hours for the selected delivery date
- No double-booking prevention in scope (not a booking system) — slots are informational

---

## Backend changes

**New endpoint:**
`GET /api/v1/bakeries/{bakeryId}/delivery-slots?date=YYYY-MM-DD`  
Returns list of `{ time: "10:00", label: "10:00 – 10:30" }` objects

Logic:
- Fetch bakery hours for the given day of week
- Generate 30-minute slots from `openTime` to `closeTime - 30min`
- Exclude slots in the past if date is today

**Order changes:**
- `CheckoutRequest` — add `deliverySlot` field (String, e.g. `"10:30"`)
- `Order` model — add `delivery_slot VARCHAR(10)` column (migration needed)

---

## Android changes

- Checkout screen: replace free-text delivery time input with a spinner/dropdown
- Fetch slots from API when bakery and date are selected
- Selected slot sent as `deliverySlot` in checkout request

---

## Open questions

- What's the cutoff for same-day ordering (e.g. must order 1 hour in advance)?
- Should slots be blocked if bakery is at capacity (requires booking logic, likely out of scope)?
