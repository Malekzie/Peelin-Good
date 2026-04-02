-- V17: Backfill review.order_id so existing seeded reviews can be fetched by bakery.
-- Some earlier seeded reviews were inserted before order_id existed (V14), leaving order_id NULL.
-- We set order_id by matching review.customer_id + review.product_id to the most recent order
-- that contains that product for that customer.

WITH matched AS (
    SELECT
        r.review_id,
        x.order_id
    FROM review r
    JOIN LATERAL (
        SELECT o.order_id
        FROM "order" o
        JOIN order_item oi ON oi.order_id = o.order_id
        WHERE o.customer_id = r.customer_id
          AND oi.product_id = r.product_id
        ORDER BY o.order_placed_datetime DESC
        LIMIT 1
    ) x ON TRUE
    WHERE r.order_id IS NULL
)
UPDATE review r
SET order_id = m.order_id
FROM matched m
WHERE r.review_id = m.review_id;

