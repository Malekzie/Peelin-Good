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
WITH matched AS (
    SELECT
        r.review_id,
        x.order_id,
        x.bakery_id
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
)
UPDATE review r
SET order_id = COALESCE(r.order_id, m.order_id),
    bakery_id = m.bakery_id
FROM matched m
WHERE r.review_id = m.review_id
  AND r.bakery_id IS NULL;

-- Final safety fallback: customer's most recent order bakery.
WITH customer_latest AS (
    SELECT DISTINCT ON (o.customer_id)
        o.customer_id,
        o.order_id,
        o.bakery_id
    FROM "order" o
    ORDER BY o.customer_id, o.order_placed_datetime DESC
)
UPDATE review r
SET order_id = COALESCE(r.order_id, cl.order_id),
    bakery_id = cl.bakery_id
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

