-- Approval timestamp is redundant for this workflow (auto-approved path + staff status only).
-- Lifecycle remains on review_status (pending / approved / rejected).
ALTER TABLE review DROP COLUMN IF EXISTS review_approval_date;
