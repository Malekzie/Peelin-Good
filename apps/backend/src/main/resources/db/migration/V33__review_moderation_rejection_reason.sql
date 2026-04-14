ALTER TABLE review
    ADD COLUMN IF NOT EXISTS moderation_rejection_reason VARCHAR(500);
