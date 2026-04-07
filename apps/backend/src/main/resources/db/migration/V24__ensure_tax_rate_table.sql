-- Idempotent: some local DBs never applied V19 (failed migrate, restored old dump, etc.)
CREATE TABLE IF NOT EXISTS tax_rate (
    province_name VARCHAR(80) PRIMARY KEY,
    tax_percent NUMERIC(5,3) NOT NULL
);

INSERT INTO tax_rate (province_name, tax_percent) VALUES
    ('Alberta', 5.000),
    ('British Columbia', 12.000),
    ('Manitoba', 12.000),
    ('New Brunswick', 15.000),
    ('Newfoundland and Labrador', 15.000),
    ('Northwest Territories', 5.000),
    ('Nova Scotia', 14.000),
    ('Nunavut', 5.000),
    ('Ontario', 13.000),
    ('Prince Edward Island', 15.000),
    ('Quebec', 14.975),
    ('Saskatchewan', 11.000),
    ('Yukon', 5.000)
ON CONFLICT (province_name) DO UPDATE
SET tax_percent = EXCLUDED.tax_percent;

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS order_tax_rate NUMERIC(5,3) NOT NULL DEFAULT 0;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS order_tax_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
