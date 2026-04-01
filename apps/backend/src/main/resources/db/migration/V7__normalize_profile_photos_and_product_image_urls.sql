-- Clear legacy profile-photo URLs that were left in user records.
UPDATE "user"
SET profile_photo_path = NULL
WHERE profile_photo_path IS NOT NULL
  AND btrim(profile_photo_path) <> '';

-- Normalize product image URLs to DigitalOcean CDN-hosted assets.
UPDATE product
SET product_image_url = CASE product_name
    WHEN 'Sourdough Loaf' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/sourdough.jpg'
    WHEN 'Multigrain Sandwich Bread' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/About_Us_Bread.jpg'
    WHEN 'Baguette' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/bread_background.png'
    WHEN 'Cinnamon Roll' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/cinnamon-rolls.jpg'
    WHEN 'Butter Croissant' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/butter-croissant.jpg'
    WHEN 'Carrot Cake Slice' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/carrot-cake.jpg'
    WHEN 'Spinach Feta Danish' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/almond-danish.jpg'
    WHEN 'Lemon Tart' THEN 'https://peelin-good-storage.tor1.cdn.digitaloceanspaces.com/bakery/lemon-tart.jpg'
    ELSE product_image_url
END;
