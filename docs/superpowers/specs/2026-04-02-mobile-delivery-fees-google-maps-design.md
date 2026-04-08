# Delivery Fees via Google Maps API

**Platform:** Spring Boot backend (affects all clients)  
**Status:** Planning

---

## What it does

Calculate delivery fees dynamically based on distance from the bakery to the customer's address, using the Google Maps Distance Matrix API. Fee = base fee + per-km rate × distance.

---

## Scope

- Replaces any flat/hardcoded delivery fee
- Calculated at checkout when `orderMethod = DELIVERY`
- Customer sees the fee before confirming the order
- Fee stored on the order record

---

## Backend changes

**Dependencies:**
- Google Maps Java Client library, or plain HTTP call to Distance Matrix API

**Config (`application.yaml`):**
```yaml
delivery:
  base-fee: 3.00
  per-km-rate: 0.75
  max-fee: 15.00
google:
  maps-api-key: ${GOOGLE_MAPS_API_KEY}
```

**New service: `DeliveryFeeService`**
- `calculateFee(bakeryAddress, customerAddress): BigDecimal`
- Calls `https://maps.googleapis.com/maps/api/distancematrix/json`
- Returns `baseFee + (distanceKm × perKmRate)`, capped at `maxFee`
- Cache results by `(bakery_id, customer_address_hash)` to avoid redundant API calls

**New endpoint:**
`GET /api/v1/orders/delivery-fee?bakeryId={id}&addressId={id}`  
Returns `{ fee: 7.50, distanceKm: 6.0 }` — called before checkout to show fee to customer

**Order changes:**
- `CheckoutRequest` — no change; fee is calculated server-side, not passed by client
- `OrderService.checkout()` — call `DeliveryFeeService` and attach fee to order
- `Order` model — ensure `delivery_fee` column exists (check migration)

**Environment variable:**
- `GOOGLE_MAPS_API_KEY`

---

## Android changes

- Checkout screen: fetch and display delivery fee estimate before the user confirms
- Show distance and fee as separate line items in order summary

---

## Open questions

- Fallback if Google Maps API is unavailable (flat fee? block delivery orders?)
- Does the customer address need to be geocoded first, or is a formatted string address sufficient for Distance Matrix?
- Cap at max fee or pass through the full cost?
