-- Product image URLs after Spaces rename / file cleanup.
-- Assumes product images live at:
--   https://peelin-good-storage.tor1.digitaloceanspaces.com/products/<filename>.jpg

UPDATE product
SET product_image_url = CASE product_name
    WHEN 'Blueberry Muffin' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/blueberry-muffin.jpg'
    WHEN 'Banana Bread Slice' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/banana-bread-slice.jpg'
    WHEN 'Chocolate Chip Cookie' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-chip-cookie.jpg'
    WHEN 'Oatmeal Raisin Cookie' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/oatmeal-raisin-cookie.jpg'
    WHEN 'Vanilla Cupcake' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/vanilla-cupcake.jpg'
    WHEN 'Chocolate Cupcake' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-cupcake.jpg'
    WHEN 'Chocolate Layer Cake' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-layer-cake.jpg'
    WHEN 'Cheesecake Slice' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/cheesecake-slice.jpg'
    WHEN 'Apple Turnover' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/apple-turnover.jpg'
    WHEN 'Vegan Chocolate Brownie' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/vegan-chocolate-brownie.jpg'
    WHEN 'Gluten-Free Banana Muffin' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/gluten-free-banana-muffin.jpg'
    WHEN 'Seasonal Pumpkin Muffin' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/seasonal-pumpkin-muffin.jpg'
    WHEN 'Almond Biscotti' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/almond-biscotti.jpg'
    WHEN 'Whole Wheat Scone' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/whole-wheat-scone.jpg'
    WHEN 'Raspberry Danish' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/raspberry-danish.jpg'
    WHEN 'Chocolate Eclair' THEN 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-eclair.jpg'
    ELSE product_image_url
END
WHERE product_name IN (
    'Blueberry Muffin',
    'Banana Bread Slice',
    'Chocolate Chip Cookie',
    'Oatmeal Raisin Cookie',
    'Vanilla Cupcake',
    'Chocolate Cupcake',
    'Chocolate Layer Cake',
    'Cheesecake Slice',
    'Apple Turnover',
    'Vegan Chocolate Brownie',
    'Gluten-Free Banana Muffin',
    'Seasonal Pumpkin Muffin',
    'Almond Biscotti',
    'Whole Wheat Scone',
    'Raspberry Danish',
    'Chocolate Eclair'
);

