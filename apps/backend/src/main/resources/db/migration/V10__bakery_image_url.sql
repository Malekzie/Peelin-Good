-- Nullable hero/list image per bakery; full HTTPS URL to object in Spaces (e.g. .../locations/...).
ALTER TABLE bakery
    ADD COLUMN IF NOT EXISTS bakery_image_url VARCHAR(2048);
