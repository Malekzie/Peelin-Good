-- V17: Backfill review.order_id so existing seeded reviews can be fetched by bakery.
-- Some earlier seeded reviews were inserted before order_id existed (V14), leaving order_id NULL.
-- We set order_id by matching review.customer_id + review.product_id to the most recent order
-- that contains that product for that customer.
--
-- When this runs late (out-of-order on a DB already constrained by V36), skip any assignment that
-- would duplicate (customer_id, order_id) on another row, and if several NULL reviews would claim
-- the same pair, keep only one (deterministic by submitted date / review_id).

WITH matched AS (
    SELECT r.review_id,
           r.customer_id,
           x.order_id,
           ROW_NUMBER() OVER (
               PARTITION BY r.customer_id, x.order_id
               ORDER BY r.review_submitted_date NULLS LAST, r.review_id
           ) AS rn
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
),
     picked AS (
         SELECT review_id, customer_id, order_id
         FROM matched
         WHERE rn = 1
     )
UPDATE review r
SET order_id = p.order_id
FROM picked p
WHERE r.review_id = p.review_id
  AND NOT EXISTS (SELECT 1
                  FROM review other
                  WHERE other.customer_id = p.customer_id
                    AND other.order_id = p.order_id
                    AND other.review_id <> r.review_id);
