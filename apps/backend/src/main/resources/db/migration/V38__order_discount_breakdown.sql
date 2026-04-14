ALTER TABLE "order"
    ADD COLUMN IF NOT EXISTS order_special_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS order_tier_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS order_employee_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

-- Backfill: legacy rows only had a single tier discount bucket in order_discount.
UPDATE "order"
SET order_tier_discount_amount = order_discount
WHERE order_special_discount_amount = 0
  AND order_tier_discount_amount = 0
  AND order_employee_discount_amount = 0
  AND order_discount IS NOT NULL;
