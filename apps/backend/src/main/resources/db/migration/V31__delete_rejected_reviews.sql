-- Rejected reviews are no longer persisted; clean up historical rows.
DELETE FROM review WHERE review_status = 'rejected';
