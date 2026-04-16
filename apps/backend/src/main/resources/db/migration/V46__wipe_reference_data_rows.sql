-- Wipes application data rows without dropping tables.
-- flyway_schema_history and tax_rate reference rows are intentionally preserved.

BEGIN;

TRUNCATE TABLE
    staff_message,
    staff_conversation,
    chat_message,
    chat_thread,
    message,
    review,
    reward,
    payment,
    order_item,
    "order",
    employee_customer_link,
    batch_inventory,
    batch,
    customer_preference,
    customer,
    employee,
    inventory,
    product_tag,
    product_special,
    supplier,
    bakery_hours,
    bakery,
    product,
    tag,
    reward_tier,
    password_reset_token,
    address,
    "user"
RESTART IDENTITY CASCADE;

COMMIT;
