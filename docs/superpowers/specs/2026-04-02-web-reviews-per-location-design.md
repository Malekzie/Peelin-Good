# Reviews on Products per Location — Web

**Platform:** Web (SvelteKit frontend + Spring Boot backend)  
**Status:** Planning

---

## What it does

Reviews are currently global per product. This feature scopes reviews to a product-at-a-bakery combination, so a croissant at the downtown location has separate reviews from the same croissant at the westside location.

---

## Scope

- Reviews attached to `(product_id, bakery_id)` rather than `product_id` alone
- Existing reviews are product-global — migration strategy needed (assign to a default bakery or leave `bakery_id` nullable for legacy)
- Average rating shown per location on the product page
- Submission requires selecting which location the customer ordered from (can be inferred from their order history)

---

## Backend changes

**Migration:**
- Add nullable `bakery_id INTEGER REFERENCES bakeries(bakery_id)` to `reviews` table

**Model:**
- `Review.java` — add `@ManyToOne Bakery bakery` field

**Repository:**
- `ReviewRepository` — add `findByProduct_IdAndBakery_IdAndReviewStatus()`
- Add `averageRatingForProductAndBakery()`

**Service:**
- `ReviewService.forProduct()` — accept optional `bakeryId` param; fall back to all-bakery if null (backward compat)
- `ReviewService.create()` — accept `bakeryId` in `ReviewCreateRequest`; validate customer has an order from that bakery for that product (optional enforcement)

**Controller:**
- `GET /api/v1/products/{productId}/reviews?bakeryId={id}` — add optional query param
- `GET /api/v1/products/{productId}/reviews/average?bakeryId={id}` — add optional query param

**DTO:**
- `ReviewCreateRequest` — add `bakeryId` field
- `ReviewDto` — add `bakeryId` field

---

## Frontend changes

- Product page: show review tab per bakery location (tabs or dropdown selector)
- Review submission form: bakery selector pre-filled from order history if available
- Average rating badge: context-aware (global or per-bakery depending on view)

---

## Open questions

- Should global (non-location) average still be shown anywhere?
- Enforce that reviewer has actually ordered from that location, or trust self-reporting?
