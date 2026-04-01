ALTER TABLE "user" ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);

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
