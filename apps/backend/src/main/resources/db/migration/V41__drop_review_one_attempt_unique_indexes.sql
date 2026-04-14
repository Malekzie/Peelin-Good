-- Allow multiple review submissions per customer/product and per customer/order
-- (e.g. retries after moderation, anonymous flows). Replaces V36 partial unique indexes.
DROP INDEX IF EXISTS uq_review_customer_product_attempt;
DROP INDEX IF EXISTS uq_review_customer_order_attempt;
