ALTER TABLE "user" ADD COLUMN IF NOT EXISTS photo_approval_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);

UPDATE "user" u
SET photo_approval_pending = c.photo_approval_pending
FROM customer c
WHERE c.user_id = u.user_id
  AND c.photo_approval_pending IS NOT NULL;

UPDATE "user" u
SET photo_approval_pending = e.photo_approval_pending
FROM employee e
WHERE e.user_id = u.user_id
  AND e.photo_approval_pending IS NOT NULL;

UPDATE "user" u
SET profile_photo_path = c.profile_photo_path
FROM customer c
WHERE c.user_id = u.user_id
  AND c.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');

UPDATE "user" u
SET profile_photo_path = e.profile_photo_path
FROM employee e
WHERE e.user_id = u.user_id
  AND e.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');

ALTER TABLE customer DROP COLUMN IF EXISTS profile_photo_path;
ALTER TABLE customer DROP COLUMN IF EXISTS photo_approval_pending;
ALTER TABLE employee DROP COLUMN IF EXISTS profile_photo_path;
ALTER TABLE employee DROP COLUMN IF EXISTS photo_approval_pending;
