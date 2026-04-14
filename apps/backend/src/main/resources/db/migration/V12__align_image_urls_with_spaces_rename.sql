-- Align image URLs after Spaces folder/file rename:
-- - old "locations/" folder is now "bakery/"
-- - old broad "bakery/" product folder is now "products/"
-- - product image filenames were cleaned up

-- Bakery hero images now live in /bakery.
UPDATE bakery
SET bakery_image_url = CASE bakery_id
    WHEN 1 THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-downtown.jpg'
    WHEN 2 THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-edmonton-central.jpg'
    WHEN 3 THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-toronto-financial.jpg'
    ELSE bakery_image_url
END
WHERE bakery_id IN (1, 2, 3);

-- Product images now live in /products with renamed files.
-- Keep existing value for anything not explicitly mapped yet.
UPDATE product
SET product_image_url = CASE product_name
    WHEN 'Sourdough Loaf' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/sourdough.jpg'
    WHEN 'Multigrain Sandwich Bread' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/multigrain-sandwich-bread.jpg'
    WHEN 'Baguette' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/baguette.png'
    WHEN 'Cinnamon Roll' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/cinnamon-roll.jpg'
    WHEN 'Butter Croissant' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/butter-croissant.jpg'
    WHEN 'Carrot Cake Slice' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/carrot-cake-slice.jpg'
    WHEN 'Spinach Feta Danish' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/spinach-feta-danish.jpg'
    WHEN 'Lemon Tart' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/lemon-tart.jpg'
    WHEN 'Brownie' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/brownie.jpg'
    WHEN 'Strawberry Shortcake Cup' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/strawberry-shortcake-cup.jpg'
    ELSE product_image_url
END
WHERE product_name IN (
    'Sourdough Loaf',
    'Multigrain Sandwich Bread',
    'Baguette',
    'Cinnamon Roll',
    'Butter Croissant',
    'Carrot Cake Slice',
    'Spinach Feta Danish',
    'Lemon Tart',
    'Brownie',
    'Strawberry Shortcake Cup'
);
