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

UPDATE "order" o
SET order_tax_rate = tr.tax_percent,
    order_tax_amount = ROUND(o.order_total * tr.tax_percent / 100.0, 2)
FROM address a
JOIN tax_rate tr ON tr.province_name = CASE UPPER(TRIM(a.address_province))
    WHEN 'AB' THEN 'Alberta'
    WHEN 'BC' THEN 'British Columbia'
    WHEN 'MB' THEN 'Manitoba'
    WHEN 'NB' THEN 'New Brunswick'
    WHEN 'NL' THEN 'Newfoundland and Labrador'
    WHEN 'NF' THEN 'Newfoundland and Labrador'
    WHEN 'NT' THEN 'Northwest Territories'
    WHEN 'NS' THEN 'Nova Scotia'
    WHEN 'NU' THEN 'Nunavut'
    WHEN 'ON' THEN 'Ontario'
    WHEN 'PE' THEN 'Prince Edward Island'
    WHEN 'PEI' THEN 'Prince Edward Island'
    WHEN 'QC' THEN 'Quebec'
    WHEN 'PQ' THEN 'Quebec'
    WHEN 'SK' THEN 'Saskatchewan'
    WHEN 'YT' THEN 'Yukon'
    WHEN 'YK' THEN 'Yukon'
    ELSE TRIM(a.address_province)
END
WHERE o.address_id = a.address_id
  AND COALESCE(o.order_tax_rate, 0) = 0
  AND COALESCE(o.order_tax_amount, 0) = 0;
