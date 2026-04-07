# Stripe Integration — Web

**Platform:** Web (SvelteKit frontend + Spring Boot backend)  
**Status:** Planning

---

## What it does

Replace or supplement the current payment flow with Stripe so customers can pay by card at checkout. Orders are only confirmed once payment succeeds.

---

## Scope

- Card payment at checkout via Stripe Payment Intent
- Order is created in `PENDING_PAYMENT` status; confirmed to `CONFIRMED` on webhook success
- Stripe webhook endpoint on backend to handle `payment_intent.succeeded` and `payment_intent.payment_failed`
- No subscription billing, no saved cards in scope

---

## Backend changes

**Dependencies to add (`pom.xml`):**
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version><!-- latest --></version>
</dependency>
```

**New files:**
- `StripeConfig.java` — reads `STRIPE_SECRET_KEY` from env
- `StripeService.java` — `createPaymentIntent(amount, currency, orderId)`, `constructWebhookEvent(payload, sig)`
- `StripeWebhookController.java` — `POST /api/v1/payments/webhook` (permit anonymous in security config, verified by Stripe signature)

**Modified files:**
- `OrderService.checkout()` — after creating order, call `StripeService.createPaymentIntent()`, return `clientSecret` alongside order in response
- `OrderController` / `CheckoutResponse` DTO — include `stripeClientSecret` field
- `Order` model — add `stripe_payment_intent_id` column (migration needed)
- `order_status` enum — add `PENDING_PAYMENT` value (migration needed)
- `SecurityConfig.java` — permit `POST /api/v1/payments/webhook`

**Environment variables needed:**
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET` (for signature verification)

---

## Frontend changes

- Add `@stripe/stripe-js` package
- Checkout page: after `POST /api/v1/orders` returns `clientSecret`, mount Stripe `PaymentElement`
- On payment confirmation, redirect to order success page
- Handle payment failure inline (show error, do not navigate away)

**Environment variable needed:**
- `PUBLIC_STRIPE_PUBLISHABLE_KEY`

---

## Migration

- Add `stripe_payment_intent_id VARCHAR(255)` to `orders` table
- Add `pending_payment` to `order_status` enum

---

## Open questions

- Is cash/in-store pickup still allowed without Stripe? If so, skip payment intent for those order methods
- Refund flow in scope? (Stripe refund API)
- Test mode vs live mode toggle for demo purposes
