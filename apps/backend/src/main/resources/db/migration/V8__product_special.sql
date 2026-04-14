-- product_special: calendar-based featured product for the storefront.
-- Lookup: match "date" to current date (local/app timezone). If multiple rows share a date, use the first row returned (ORDER BY product_special_id LIMIT 1).

CREATE TABLE IF NOT EXISTS product_special (
    product_special_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id         INTEGER NOT NULL REFERENCES product (product_id) ON DELETE CASCADE,
    "date"             DATE    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_special_date ON product_special ("date");

INSERT INTO product_special (product_special_id, product_id, "date")
OVERRIDING SYSTEM VALUE VALUES
    (1, 1, DATE '2026-04-01'),
    (2, 1, DATE '2026-04-02');

SELECT setval(pg_get_serial_sequence('product_special', 'product_special_id'), (SELECT MAX(product_special_id) FROM product_special));
