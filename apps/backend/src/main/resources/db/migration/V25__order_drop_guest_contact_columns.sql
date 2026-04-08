-- Guest contact is stored on customer; orders always reference customer_id.
ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_or_guest;

ALTER TABLE "order" DROP COLUMN IF EXISTS guest_name;
ALTER TABLE "order" DROP COLUMN IF EXISTS guest_email;
ALTER TABLE "order" DROP COLUMN IF EXISTS guest_phone;

ALTER TABLE "order" ADD CONSTRAINT chk_order_customer_id_not_null CHECK (customer_id IS NOT NULL);
