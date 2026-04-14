-- Undo V25: keep guest snapshot columns for clients (e.g. Svelte) and legacy constraint shape.
ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_id_not_null;

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_name VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_email VARCHAR(254);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(20);

ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_or_guest;
ALTER TABLE "order" ADD CONSTRAINT chk_order_customer_or_guest
    CHECK (customer_id IS NOT NULL OR (guest_name IS NOT NULL AND guest_email IS NOT NULL));
