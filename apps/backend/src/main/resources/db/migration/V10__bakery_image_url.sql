-- Nullable storefront/hero image for each bakery (DigitalOcean Spaces, e.g. .../locations/...).
ALTER TABLE bakery
    ADD COLUMN IF NOT EXISTS bakery_image_url VARCHAR(2048);
