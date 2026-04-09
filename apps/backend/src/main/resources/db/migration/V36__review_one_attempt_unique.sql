-- Enforce one review attempt per customer/product and per customer/order (race-safe).
-- Clean duplicates produced by concurrent submissions before creating unique indexes.
WITH ranked_product AS (
    SELECT review_id,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id, product_id
               ORDER BY review_submitted_date, review_id
           ) AS rn
    FROM review
    WHERE order_id IS NULL
),
ranked_order AS (
    SELECT review_id,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id, order_id
               ORDER BY review_submitted_date, review_id
           ) AS rn
    FROM review
    WHERE order_id IS NOT NULL
)
DELETE FROM review r
WHERE r.review_id IN (
    SELECT review_id FROM ranked_product WHERE rn > 1
    UNION ALL
    SELECT review_id FROM ranked_order WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_customer_product_attempt
    ON review (customer_id, product_id)
    WHERE order_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_customer_order_attempt
    ON review (customer_id, order_id)
    WHERE order_id IS NOT NULL;
