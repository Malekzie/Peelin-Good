# Employee Discounts — Mobile

**Platform:** Android (Workshop06, Kotlin) + Spring Boot backend  
**Status:** Planning

---

## What it does

Employees who log in receive a discount applied automatically at checkout. The discount percentage is configurable and stored server-side.

---

## Scope

- Discount applied at checkout when the authenticated user has role `EMPLOYEE`
- Discount is a flat percentage (e.g. 20% off subtotal)
- Visible in cart/checkout summary as a line item
- No discount stacking with other promotions in scope

---

## Backend changes

**Config:**
- Add `employee.discount.percentage` to `application.yaml` (e.g. `0.20`)

**Service:**
- `OrderService.checkout()` — check caller role; if `EMPLOYEE`, apply discount to subtotal before total calculation
- Return `employeeDiscountApplied` and `discountAmount` in `OrderDto`

**DTO:**
- `OrderDto` — add `discountAmount` (BigDecimal), `discountType` (String, nullable)
- `CheckoutResponse` — reflect discount in line items

**Migration:**
- Add `discount_amount NUMERIC(10,2)` and `discount_type VARCHAR(50)` to `orders` table

---

## Android changes

- Cart summary: show discount line item when `discountAmount > 0`
- Order detail screen: display discount applied
- No UI toggle needed — automatic based on role

---

## Open questions

- Should employee discount apply to delivery fees as well, or subtotal only?
- Is the discount percentage hardcoded or managed through an admin screen?
