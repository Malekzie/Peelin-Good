-- V18: Make reviews bakery-owned with non-null bakery_id.
-- Backfill bakery_id from order first, then from customer's most recent matching order.

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS bakery_id INTEGER;

-- Primary backfill: where order_id exists, copy bakery directly.
UPDATE review r
SET bakery_id = o.bakery_id
FROM "order" o
WHERE r.order_id = o.order_id
  AND r.bakery_id IS NULL;

-- Secondary backfill: infer from customer+product to most recent matching order.
-- Safe when re-run / out-of-order: avoid duplicate (customer_id, order_id) (see V36 unique index).
WITH matched AS (
    SELECT r.review_id,
           r.customer_id,
           x.order_id,
           x.bakery_id,
           ROW_NUMBER() OVER (
               PARTITION BY r.customer_id, x.order_id
               ORDER BY r.review_submitted_date NULLS LAST, r.review_id
           ) AS rn
    FROM review r
             JOIN LATERAL (
        SELECT o.order_id, o.bakery_id
        FROM "order" o
                 JOIN order_item oi ON oi.order_id = o.order_id
        WHERE o.customer_id = r.customer_id
          AND oi.product_id = r.product_id
        ORDER BY o.order_placed_datetime DESC
        LIMIT 1
        ) x ON TRUE
    WHERE r.bakery_id IS NULL
),
     picked AS (
         SELECT review_id, customer_id, order_id, bakery_id
         FROM matched
         WHERE rn = 1
     )
UPDATE review r
SET order_id = COALESCE(r.order_id, p.order_id),
    bakery_id = p.bakery_id
FROM picked p
WHERE r.review_id = p.review_id
  AND r.bakery_id IS NULL
  AND (
    COALESCE(r.order_id, p.order_id) IS NULL
        OR NOT EXISTS (SELECT 1
                       FROM review other
                       WHERE other.customer_id = p.customer_id
                         AND other.order_id = COALESCE(r.order_id, p.order_id)
                         AND other.review_id <> r.review_id)
    );

-- Final safety fallback: copy bakery from customer's most recent order only (do not mass-assign
-- order_id — that would duplicate (customer_id, order_id) for multiple product reviews per customer).
WITH customer_latest AS (
    SELECT DISTINCT ON (o.customer_id) o.customer_id,
                                        o.bakery_id
    FROM "order" o
    ORDER BY o.customer_id, o.order_placed_datetime DESC
)
UPDATE review r
SET bakery_id = cl.bakery_id
FROM customer_latest cl
WHERE r.customer_id = cl.customer_id
  AND r.bakery_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM review WHERE bakery_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot enforce NOT NULL on review.bakery_id; unresolved rows remain.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'review_bakery_id_fkey'
    ) THEN
        ALTER TABLE review
            ADD CONSTRAINT review_bakery_id_fkey
            FOREIGN KEY (bakery_id) REFERENCES bakery(bakery_id);
    END IF;
END $$;

ALTER TABLE review
    ALTER COLUMN bakery_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_review_bakery ON review (bakery_id);

