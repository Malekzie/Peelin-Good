-- Partial guest customers may omit legal name until they complete registration.
ALTER TABLE customer ALTER COLUMN customer_first_name DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN customer_last_name DROP NOT NULL;
