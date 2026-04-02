-- Daily special discount shown in the storefront (e.g. percent off the featured product for that calendar day).

ALTER TABLE product_special
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;

UPDATE product_special
SET discount_percent = 10.00
WHERE product_special_id IN (1, 2);
