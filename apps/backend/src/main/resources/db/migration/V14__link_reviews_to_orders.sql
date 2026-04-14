-- Allow review moderation to complete the exact delivered order that generated the review.
ALTER TABLE review
    ADD COLUMN IF NOT EXISTS order_id UUID REFERENCES "order"(order_id);

CREATE INDEX IF NOT EXISTS idx_review_order ON review (order_id);

