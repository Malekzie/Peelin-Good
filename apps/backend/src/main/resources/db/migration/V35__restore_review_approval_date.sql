-- Restores staff / workflow approval timestamp (V34 dropped it; product still uses review_status).
ALTER TABLE review ADD COLUMN IF NOT EXISTS review_approval_date TIMESTAMP WITH TIME ZONE;

UPDATE review
SET review_approval_date = review_submitted_date
WHERE review_status = 'approved'
  AND review_approval_date IS NULL;
