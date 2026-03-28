-- V3: Seed data adapted from unified_postgres.sql for Flyway V1/V2 schema.
-- Surrogate PKs (user_id, employee_id, customer_id, order_id, …) are INTEGER (matches JPA).
-- Stable public identifiers remain in uuid columns where present.
-- Passwords: Admin123! (admin), Emp123! (employees), Cust123! (customers).
-- Run on an empty DB after V2 (or reset). Flyway wraps this in a transaction.

-- address
INSERT INTO address (address_id, address_line1, address_line2, address_city, address_province, address_postal_code)
OVERRIDING SYSTEM VALUE VALUES
    (1,  '1208 4 Ave SW',              'Suite 210',  'Calgary',    'AB', 'T2P 0H3'),
    (2,  '33 10 St NW',                NULL,         'Calgary',    'AB', 'T2N 1V4'),
    (3,  '455 7 Ave SE',               NULL,         'Calgary',    'AB', 'T2G 0J8'),
    (4,  '9805 12 Ave SW',             NULL,         'Calgary',    'AB', 'T2W 1K1'),
    (5,  '2100 16 Ave NW',             'Unit 14',    'Calgary',    'AB', 'T2M 0M5'),
    (6,  '8715 Macleod Trail SE',      'Unit 120',   'Calgary',    'AB', 'T2H 0M3'),
    (7,  '101 9 Ave SW',               'Floor 6',    'Calgary',    'AB', 'T2P 1J9'),
    (8,  '560 2 St SW',                NULL,         'Calgary',    'AB', 'T2P 0S6'),
    (9,  '1180 7 St SW',               NULL,         'Calgary',    'AB', 'T2R 1A5'),
    (10, '4020 4 St NW',               NULL,         'Calgary',    'AB', 'T2K 1A2'),
    (11, '12 100 St NW',               NULL,         'Edmonton',   'AB', 'T5J 1L6'),
    (12, '815 104 Ave NW',             'Unit 5',     'Edmonton',   'AB', 'T5H 0L1'),
    (13, '10425 Jasper Ave',           'Suite 300',  'Edmonton',   'AB', 'T5J 1Z7'),
    (14, '8900 99 St NW',              NULL,         'Edmonton',   'AB', 'T6E 3T9'),
    (15, '150 109 St NW',              'Unit 2',     'Edmonton',   'AB', 'T5J 2X6'),
    (16, '200 Granville St',           'Unit 110',   'Vancouver',  'BC', 'V6C 1S4'),
    (17, '845 Burrard St',             NULL,         'Vancouver',  'BC', 'V6Z 2K6'),
    (18, '1155 W Georgia St',          'Suite 900',  'Vancouver',  'BC', 'V6E 4T6'),
    (19, '777 Hornby St',              NULL,         'Vancouver',  'BC', 'V6Z 1S4'),
    (20, '10 King St W',               'Floor 12',   'Toronto',    'ON', 'M5H 1A1'),
    (21, '220 Bloor St W',             'Unit 7',     'Toronto',    'ON', 'M5S 1T8'),
    (22, '30 Wellington St W',         NULL,         'Toronto',    'ON', 'M5L 1E2'),
    (23, '1555 Rue Sainte-Catherine O','Suite 400',  'Montreal',   'QC', 'H3G 1P2'),
    (24, '1000 Rue De La Gauchetire O',NULL,         'Montreal',   'QC', 'H3B 4W5'),
    (25, '300 Prince of Wales Dr',     'Unit 18',    'Ottawa',     'ON', 'K2C 3T2'),
    (26, '99 Bank St',                 'Suite 500',  'Ottawa',     'ON', 'K1P 5N2'),
    (27, '75 Queen St',                NULL,         'Ottawa',     'ON', 'K1P 1N2'),
    (28, '2500 5 Ave NE',              NULL,         'Calgary',    'AB', 'T2A 6K6'),
    (29, '4400 4 Ave SE',              NULL,         'Calgary',    'AB', 'T2G 4X3'),
    (30, '601 12 Ave SW',              'Unit 3',     'Calgary',    'AB', 'T2R 1H7'),
    (31, '920 17 Ave SW',              NULL,         'Calgary',    'AB', 'T2T 0A8'),
    (32, '350 8 Ave SE',               NULL,         'Calgary',    'AB', 'T2G 0K6'),
    (33, '65 97 St NW',                NULL,         'Edmonton',   'AB', 'T5K 1L5'),
    (34, '730 7 Ave SW',               'Suite 240',  'Calgary',    'AB', 'T2P 0Z9'),
    (35, '888 3 St SW',                'Unit 105',   'Calgary',    'AB', 'T2P 5C5'),
    (36, '145 5 Ave SE',               NULL,         'Calgary',    'AB', 'T2G 2X1'),
    (37, '940 6 Ave SW',               'Suite 180',  'Calgary',    'AB', 'T2P 3T1'),
    (38, '222 4 Ave SE',               NULL,         'Calgary',    'AB', 'T2G 4X7'),
    (39, '1300 1 St SE',               'Unit 12',    'Calgary',    'AB', 'T2G 0G8'),
    (40, '410 10 St NW',               NULL,         'Calgary',    'AB', 'T2N 1V7'),
    (41, '5800 2 St SW',               'Unit 8',     'Calgary',    'AB', 'T2H 0H2');
SELECT setval(pg_get_serial_sequence('address', 'address_id'), 41);

INSERT INTO reward_tier (reward_tier_id, reward_tier_name, reward_tier_min_points, reward_tier_max_points, reward_tier_discount_rate)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'Bronze',   0,      99999,  0.00),
    (2, 'Silver',   100000, 249999, 5.00),
    (3, 'Gold',     250000, 499999, 10.00),
    (4, 'Platinum', 500000, NULL,   15.00);
SELECT setval(pg_get_serial_sequence('reward_tier', 'reward_tier_id'), 4);

INSERT INTO "user" (user_id, uuid, username, user_email, user_password_hash, user_role, is_active, user_created_at)
OVERRIDING SYSTEM VALUE VALUES
    (1, '10000000-0000-4000-8000-000000000001'::uuid, 'alicia.nguyen', 'alicia.nguyen@northharbourmail.ca', '$2b$10$R92cP1dmtXyTDQYq6U53V.fRED4Kb9JHcAscibP8XAc7W1Zzm2hXm', 'admin'::user_role, TRUE, '2025-08-22 12:00:00+00'),
    (2, '10000000-0000-4000-8000-000000000002'::uuid, 'mason.clark', 'mason.clark@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-16 12:00:00+00'),
    (3, '10000000-0000-4000-8000-000000000003'::uuid, 'sophia.patel', 'sophia.patel@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-21 12:00:00+00'),
    (4, '10000000-0000-4000-8000-000000000004'::uuid, 'ethan.wright', 'ethan.wright@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-29 12:00:00+00'),
    (5, '10000000-0000-4000-8000-000000000005'::uuid, 'isabella.chen', 'isabella.chen@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-01 12:00:00+00'),
    (6, '10000000-0000-4000-8000-000000000006'::uuid, 'noah.martin', 'noah.martin@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-05 12:00:00+00'),
    (7, '10000000-0000-4000-8000-000000000007'::uuid, 'ava.roberts', 'ava.roberts@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-11 12:00:00+00'),
    (8, '10000000-0000-4000-8000-000000000008'::uuid, 'logan.scott', 'logan.scott@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-15 12:00:00+00'),
    (9, '10000000-0000-4000-8000-000000000009'::uuid, 'mia.kim', 'mia.kim@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-19 12:00:00+00'),
    (10, '10000000-0000-4000-8000-000000000010'::uuid, 'jackson.hall', 'jackson.hall@northharbourmail.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-21 12:00:00+00'),
    (11, '10000000-0000-4000-8000-000000000011'::uuid, 'olivia.brown', 'olivia.brown@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-23 12:00:00+00'),
    (12, '10000000-0000-4000-8000-000000000012'::uuid, 'liam.thompson', 'liam.thompson@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-26 12:00:00+00'),
    (13, '10000000-0000-4000-8000-000000000013'::uuid, 'emma.wilson', 'emma.wilson@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-31 12:00:00+00'),
    (14, '10000000-0000-4000-8000-000000000014'::uuid, 'benjamin.lee', 'benjamin.lee@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-05 12:00:00+00'),
    (15, '10000000-0000-4000-8000-000000000015'::uuid, 'amelia.johnson', 'amelia.johnson@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-08 12:00:00+00'),
    (16, '10000000-0000-4000-8000-000000000016'::uuid, 'lucas.anderson', 'lucas.anderson@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-10 12:00:00+00'),
    (17, '10000000-0000-4000-8000-000000000017'::uuid, 'charlotte.miller', 'charlotte.miller@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-12 12:00:00+00'),
    (18, '10000000-0000-4000-8000-000000000018'::uuid, 'henry.davis', 'henry.davis@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-15 12:00:00+00'),
    (19, '10000000-0000-4000-8000-000000000019'::uuid, 'evelyn.moore', 'evelyn.moore@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-20 12:00:00+00'),
    (20, '10000000-0000-4000-8000-000000000020'::uuid, 'daniel.taylor', 'daniel.taylor@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-22 12:00:00+00'),
    (21, '10000000-0000-4000-8000-000000000021'::uuid, 'harper.jackson', 'harper.jackson@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-26 12:00:00+00'),
    (22, '10000000-0000-4000-8000-000000000022'::uuid, 'sebastian.white', 'sebastian.white@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-28 12:00:00+00'),
    (23, '10000000-0000-4000-8000-000000000023'::uuid, 'nora.harris', 'nora.harris@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-30 12:00:00+00'),
    (24, '10000000-0000-4000-8000-000000000024'::uuid, 'wyatt.martinez', 'wyatt.martinez@northharbourmail.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-12-02 12:00:00+00');
SELECT setval(pg_get_serial_sequence('"user"', 'user_id'), (SELECT MAX(user_id) FROM "user"));

INSERT INTO bakery (bakery_id, address_id, bakery_name, bakery_phone, bakery_email, status, latitude, longitude)
OVERRIDING SYSTEM VALUE VALUES
    (1, 1,  'North Harbour Bakery - Downtown',         '(403) 555-2101', 'downtown@northharbourbakery.ca',  'open'::bakery_status, 51.044700, -114.071900),
    (2, 11, 'North Harbour Bakery - Edmonton Central',  '(780) 555-4302', 'edmonton@northharbourbakery.ca', 'open'::bakery_status, 53.546100, -113.493800),
    (3, 20, 'North Harbour Bakery - Toronto Financial', '(416) 555-9012', 'toronto@northharbourbakery.ca',  'open'::bakery_status, 43.653200, -79.383200);
SELECT setval(pg_get_serial_sequence('bakery', 'bakery_id'), 3);

INSERT INTO bakery_hours (bakery_hours_id, bakery_id, day_of_week, open_time, close_time, is_closed)
OVERRIDING SYSTEM VALUE VALUES
    (1, 1, 1::smallint, '07:30'::time, '18:00'::time, FALSE),
    (2, 1, 2::smallint, '07:30'::time, '18:00'::time, FALSE),
    (3, 1, 3::smallint, '07:30'::time, '18:00'::time, FALSE),
    (4, 1, 4::smallint, '07:30'::time, '18:00'::time, FALSE),
    (5, 1, 5::smallint, '07:30'::time, '18:00'::time, FALSE),
    (6, 1, 6::smallint, '08:30'::time, '16:30'::time, FALSE),
    (7, 1, 0::smallint, NULL, NULL, TRUE),
    (8, 2, 1::smallint, '08:00'::time, '17:30'::time, FALSE),
    (9, 2, 2::smallint, '08:00'::time, '17:30'::time, FALSE),
    (10, 2, 3::smallint, '08:00'::time, '17:30'::time, FALSE),
    (11, 2, 4::smallint, '08:00'::time, '17:30'::time, FALSE),
    (12, 2, 5::smallint, '08:00'::time, '17:30'::time, FALSE),
    (13, 2, 6::smallint, '09:00'::time, '16:00'::time, FALSE),
    (14, 2, 0::smallint, NULL, NULL, TRUE),
    (15, 3, 1::smallint, '07:00'::time, '18:30'::time, FALSE),
    (16, 3, 2::smallint, '07:00'::time, '18:30'::time, FALSE),
    (17, 3, 3::smallint, '07:00'::time, '18:30'::time, FALSE),
    (18, 3, 4::smallint, '07:00'::time, '18:30'::time, FALSE),
    (19, 3, 5::smallint, '07:00'::time, '18:30'::time, FALSE),
    (20, 3, 6::smallint, '08:00'::time, '17:00'::time, FALSE),
    (21, 3, 0::smallint, '09:00'::time, '14:00'::time, FALSE);
SELECT setval(pg_get_serial_sequence('bakery_hours', 'bakery_hours_id'), 21);

INSERT INTO tag (tag_id, tag_name) OVERRIDING SYSTEM VALUE VALUES
    (1,  'Bread'), (2,  'Cake'), (3,  'Pastry'), (4,  'Cookie'), (5,  'Gluten-Free'), (6,  'Dairy-Free'),
    (7,  'Seasonal'), (8,  'Vegan'), (9,  'Breakfast'), (10, 'Dessert'), (11, 'Nut-Free'), (12, 'Whole Grain');
SELECT setval(pg_get_serial_sequence('tag', 'tag_id'), 12);

INSERT INTO product (product_id, product_name, product_description, product_base_price, product_image_url)
OVERRIDING SYSTEM VALUE VALUES
    (1,  'Sourdough Loaf',            'Naturally leavened sourdough bread',                    6.49,  NULL),
    (2,  'Multigrain Sandwich Bread',  'Whole grain sandwich loaf',                            5.99,  NULL),
    (3,  'Baguette',                   'Classic French-style baguette',                        3.49,  NULL),
    (4,  'Cinnamon Roll',             'Soft roll with cinnamon filling and glaze',             4.25,  NULL),
    (5,  'Butter Croissant',          'Flaky butter croissant',                                3.95,  NULL),
    (6,  'Blueberry Muffin',          'Muffin with blueberries',                               3.25,  NULL),
    (7,  'Banana Bread Slice',        'Moist banana bread slice',                              2.95,  NULL),
    (8,  'Chocolate Chip Cookie',     'Cookie with chocolate chips',                           2.25,  NULL),
    (9,  'Oatmeal Raisin Cookie',     'Oatmeal cookie with raisins',                          2.25,  NULL),
    (10, 'Vanilla Cupcake',           'Vanilla cupcake with buttercream',                      3.50,  NULL),
    (11, 'Chocolate Cupcake',         'Chocolate cupcake with buttercream',                    3.50,  NULL),
    (12, 'Carrot Cake Slice',         'Carrot cake slice with cream cheese icing',             6.95,  NULL),
    (13, 'Chocolate Layer Cake',      'Chocolate cake with ganache',                           29.99, NULL),
    (14, 'Cheesecake Slice',          'Classic cheesecake slice',                              7.25,  NULL),
    (15, 'Apple Turnover',            'Puff pastry turnover with apple filling',               4.10,  NULL),
    (16, 'Spinach Feta Danish',       'Danish pastry with spinach and feta',                   4.75,  NULL),
    (17, 'Lemon Tart',               'Tart with lemon curd filling',                          6.50,  NULL),
    (18, 'Brownie',                   'Fudgy chocolate brownie',                               3.75,  NULL),
    (19, 'Vegan Chocolate Brownie',   'Dairy-free brownie',                                    4.25,  NULL),
    (20, 'Gluten-Free Banana Muffin', 'Gluten-free banana muffin',                            3.95,  NULL),
    (21, 'Seasonal Pumpkin Muffin',   'Pumpkin spice muffin',                                  3.75,  NULL),
    (22, 'Strawberry Shortcake Cup',  'Layered shortcake with strawberries',                   6.95,  NULL),
    (23, 'Almond Biscotti',           'Twice-baked almond biscotti',                           2.75,  NULL),
    (24, 'Whole Wheat Scone',         'Scone made with whole wheat flour',                     3.25,  NULL),
    (25, 'Raspberry Danish',          'Danish pastry with raspberry filling',                   4.75,  NULL),
    (26, 'Chocolate Eclair',          'Choux pastry with cream and chocolate topping',         5.25,  NULL);
SELECT setval(pg_get_serial_sequence('product', 'product_id'), 26);

INSERT INTO product_tag (product_id, tag_id) VALUES
    (1, 1), (2, 1), (2, 12), (3, 1), (4, 3), (4, 9), (5, 3), (5, 9), (6, 3), (6, 9), (7, 9),
    (8, 4), (8, 10), (9, 4), (9, 10), (10, 2), (10, 10), (11, 2), (11, 10), (12, 2), (12, 10), (13, 2), (13, 10),
    (14, 2), (14, 10), (15, 3), (15, 10), (16, 3), (17, 10), (18, 10), (19, 6), (19, 8), (20, 5), (21, 7), (22, 10),
    (23, 4), (24, 12), (25, 3), (26, 3);

INSERT INTO supplier (supplier_id, address_id, supplier_name, supplier_phone, supplier_email)
OVERRIDING SYSTEM VALUE VALUES
    (1, 29, 'Prairie Wholesale Ingredients',  '(403) 555-7001', 'orders@prairiewholesale.ca'),
    (2, 30, 'Summit Packaging Supply',        '(403) 555-7002', 'support@summitpackaging.ca'),
    (3, 33, 'Riverbend Dairy Co.',            '(780) 555-7003', 'sales@riverbenddairy.ca'),
    (4, 16, 'Coastal Produce Distributors',   '(604) 555-7004', 'info@coastalproduce.ca'),
    (5, 24, 'St. Lawrence Dry Goods',         '(514) 555-7005', 'service@stlawrencedrygoods.ca');
SELECT setval(pg_get_serial_sequence('supplier', 'supplier_id'), 5);

INSERT INTO inventory (inventory_id, bakery_id, supplier_id, inventory_item_name, inventory_item_type, inventory_quantity_on_hand, inventory_unit_of_measure)
OVERRIDING SYSTEM VALUE VALUES
    (1,  1, 1, 'All-purpose flour',       'Ingredient', 450.000, 'kg'),
    (2,  1, 1, 'Granulated sugar',        'Ingredient', 220.000, 'kg'),
    (3,  1, 3, 'Unsalted butter',         'Ingredient', 180.000, 'kg'),
    (4,  1, 3, 'Whole milk',              'Ingredient', 600.000, 'L'),
    (5,  1, 4, 'Fresh lemons',            'Ingredient', 95.000,  'kg'),
    (6,  1, 2, 'Bakery boxes (10 inch)',  'Packaging',  800.000, 'count'),
    (7,  1, 2, 'Pastry bags',             'Packaging',  1200.000,'count'),
    (8,  1, 5, 'Baking cocoa',            'Ingredient', 80.000,  'kg'),
    (9,  2, 1, 'All-purpose flour',       'Ingredient', 380.000, 'kg'),
    (10, 2, 1, 'Granulated sugar',        'Ingredient', 210.000, 'kg'),
    (11, 2, 3, 'Unsalted butter',         'Ingredient', 165.000, 'kg'),
    (12, 2, 3, 'Whole milk',              'Ingredient', 520.000, 'L'),
    (13, 2, 4, 'Fresh berries (mixed)',   'Ingredient', 70.000,  'kg'),
    (14, 2, 2, 'Bakery boxes (10 inch)',  'Packaging',  650.000, 'count'),
    (15, 2, 2, 'Cupcake liners',          'Packaging',  5000.000,'count'),
    (16, 2, 5, 'Vanilla extract',         'Ingredient', 18.000,  'L'),
    (17, 3, 1, 'All-purpose flour',       'Ingredient', 520.000, 'kg'),
    (18, 3, 1, 'Granulated sugar',        'Ingredient', 260.000, 'kg'),
    (19, 3, 3, 'Unsalted butter',         'Ingredient', 210.000, 'kg'),
    (20, 3, 3, 'Whole milk',              'Ingredient', 720.000, 'L'),
    (21, 3, 4, 'Apples (fresh)',          'Ingredient', 120.000, 'kg'),
    (22, 3, 2, 'Bakery boxes (10 inch)',  'Packaging',  900.000, 'count'),
    (23, 3, 2, 'Paper bags',             'Packaging',  3000.000,'count'),
    (24, 3, 5, 'Baking powder',           'Ingredient', 65.000,  'kg');
SELECT setval(pg_get_serial_sequence('inventory', 'inventory_id'), 24);

INSERT INTO employee (employee_id, uuid, user_id, address_id, bakery_id, employee_first_name, employee_middle_initial, employee_last_name, employee_position, employee_phone, employee_business_phone, employee_work_email, photo_approval_pending)
OVERRIDING SYSTEM VALUE VALUES
    (1, '30000000-0000-4000-8000-000000000001'::uuid, 2, 2,  1, 'Mason',   NULL, 'Clark',   'Baker',            '(403) 555-3101', '(403) 555-4101', 'mason.clark@northharbourbakery.ca', FALSE),
    (2, '30000000-0000-4000-8000-000000000002'::uuid, 3, 3,  1, 'Sophia',  'R ', 'Patel',   'Baker',            '(403) 555-3102', '(403) 555-4102', 'sophia.patel@northharbourbakery.ca', FALSE),
    (3, '30000000-0000-4000-8000-000000000003'::uuid, 4, 4,  1, 'Ethan',   NULL, 'Wright',  'Shift Lead',       '(403) 555-3103', '(403) 555-4103', 'ethan.wright@northharbourbakery.ca', FALSE),
    (4, '30000000-0000-4000-8000-000000000004'::uuid, 5, 5,  1, 'Isabella','M ', 'Chen',    'Baker',            '(403) 555-3104', '(403) 555-4104', 'isabella.chen@northharbourbakery.ca', FALSE),
    (5, '30000000-0000-4000-8000-000000000005'::uuid, 6, 6,  2, 'Noah',    NULL, 'Martin',  'Baker',            '(403) 555-3105', '(403) 555-4105', 'noah.martin@northharbourbakery.ca', FALSE),
    (6, '30000000-0000-4000-8000-000000000006'::uuid, 7, 7,  2, 'Ava',     NULL, 'Roberts', 'Customer Support', '(403) 555-3106', '(403) 555-4106', 'ava.roberts@northharbourbakery.ca', FALSE),
    (7, '30000000-0000-4000-8000-000000000007'::uuid, 8, 8,  2, 'Logan',   'J ', 'Scott',   'Quality Control',  '(403) 555-3107', '(403) 555-4107', 'logan.scott@northharbourbakery.ca', FALSE),
    (8, '30000000-0000-4000-8000-000000000008'::uuid, 9, 9,  3, 'Mia',     NULL, 'Kim',     'Baker',            '(403) 555-3108', '(403) 555-4108', 'mia.kim@northharbourbakery.ca', FALSE),
    (9, '30000000-0000-4000-8000-000000000009'::uuid, 10, 10, 3, 'Jackson', NULL, 'Hall',    'Baker',            '(403) 555-3109', '(403) 555-4109', 'jackson.hall@northharbourbakery.ca', FALSE);
SELECT setval(pg_get_serial_sequence('employee', 'employee_id'), (SELECT MAX(employee_id) FROM employee));

INSERT INTO customer (customer_id, uuid, user_id, address_id, reward_tier_id, customer_first_name, customer_middle_initial, customer_last_name, customer_phone, customer_business_phone, customer_email, customer_reward_balance, customer_tier_assigned_date, photo_approval_pending)
OVERRIDING SYSTEM VALUE VALUES
    (1, '20000000-0000-4000-8000-000000000001'::uuid, 11, 21, 1, 'Olivia', NULL, 'Brown', '(416) 555-1201', NULL, 'olivia.brown@northharbourmail.ca', 120000, '2025-11-20', FALSE),
    (2, '20000000-0000-4000-8000-000000000002'::uuid, 12, 22, 1, 'Liam', NULL, 'Thompson', '(416) 555-1202', NULL, 'liam.thompson@northharbourmail.ca', 240000, '2025-11-22', FALSE),
    (3, '20000000-0000-4000-8000-000000000003'::uuid, 13, 23, 2, 'Emma', 'J', 'Wilson', '(514) 555-1203', NULL, 'emma.wilson@northharbourmail.ca', 520000, '2025-11-24', FALSE),
    (4, '20000000-0000-4000-8000-000000000004'::uuid, 14, 25, 1, 'Benjamin', NULL, 'Lee', '(613) 555-1204', NULL, 'benjamin.lee@northharbourmail.ca', 80000, '2025-11-26', FALSE),
    (5, '20000000-0000-4000-8000-000000000005'::uuid, 15, 26, 2, 'Amelia', NULL, 'Johnson', '(613) 555-1205', NULL, 'amelia.johnson@northharbourmail.ca', 740000, '2025-11-28', FALSE),
    (6, '20000000-0000-4000-8000-000000000006'::uuid, 16, 27, 1, 'Lucas', 'A', 'Anderson', '(613) 555-1206', NULL, 'lucas.anderson@northharbourmail.ca', 60000, '2025-11-30', FALSE),
    (7, '20000000-0000-4000-8000-000000000007'::uuid, 17, 31, 1, 'Charlotte', NULL, 'Miller', '(403) 555-1207', NULL, 'charlotte.miller@northharbourmail.ca', 210000, '2025-12-02', FALSE),
    (8, '20000000-0000-4000-8000-000000000008'::uuid, 18, 32, 3, 'Henry', NULL, 'Davis', '(403) 555-1208', NULL, 'henry.davis@northharbourmail.ca', 1120000, '2025-12-04', FALSE),
    (9, '20000000-0000-4000-8000-000000000009'::uuid, 19, 34, 2, 'Evelyn', NULL, 'Moore', '(403) 555-1209', NULL, 'evelyn.moore@northharbourmail.ca', 680000, '2025-12-06', FALSE),
    (10, '20000000-0000-4000-8000-000000000010'::uuid, 20, 35, 1, 'Daniel', NULL, 'Taylor', '(403) 555-1210', NULL, 'daniel.taylor@northharbourmail.ca', 140000, '2025-12-08', FALSE),
    (11, '20000000-0000-4000-8000-000000000011'::uuid, 21, 36, 2, 'Harper', NULL, 'Jackson', '(403) 555-1211', NULL, 'harper.jackson@northharbourmail.ca', 810000, '2025-12-10', FALSE),
    (12, '20000000-0000-4000-8000-000000000012'::uuid, 22, 37, 1, 'Sebastian', NULL, 'White', '(403) 555-1212', NULL, 'sebastian.white@northharbourmail.ca', 95000, '2025-12-11', FALSE),
    (13, '20000000-0000-4000-8000-000000000013'::uuid, 23, 38, 1, 'Nora', NULL, 'Harris', '(403) 555-1213', NULL, 'nora.harris@northharbourmail.ca', 260000, '2025-12-12', FALSE),
    (14, '20000000-0000-4000-8000-000000000014'::uuid, 24, 39, 1, 'Wyatt', NULL, 'Martinez', '(403) 555-1214', NULL, 'wyatt.martinez@northharbourmail.ca', 180000, '2025-12-13', FALSE);
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'), (SELECT MAX(customer_id) FROM customer));

INSERT INTO batch (batch_id, bakery_id, product_id, employee_id, batch_production_date, batch_expiry_date, batch_quantity_produced)
OVERRIDING SYSTEM VALUE VALUES
    (1,  1, 1,  1, '2025-12-14'::date, '2025-12-19'::date, 60),
    (2,  1, 3,  2, '2025-12-17'::date, '2025-12-22'::date, 90),
    (3,  1, 5,  3, '2025-12-18'::date, '2025-12-22'::date, 120),
    (4,  1, 8,  4, '2025-12-16'::date, '2025-12-26'::date, 200),
    (5,  1, 13, 3, '2025-12-19'::date, '2025-12-23'::date, 12),
    (6,  1, 21, 2, '2025-12-20'::date, '2025-12-25'::date, 80),
    (7,  2, 2,  5, '2025-12-15'::date, '2025-12-22'::date, 55),
    (8,  2, 6,  6, '2025-12-18'::date, '2025-12-23'::date, 140),
    (9,  2, 10, 7, '2025-12-18'::date, '2025-12-24'::date, 110),
    (10, 2, 14, 8, '2025-12-19'::date, '2025-12-23'::date, 40),
    (11, 2, 18, 9, '2025-12-20'::date, '2025-12-26'::date, 90),
    (12, 3, 4,  6, '2025-12-17'::date, '2025-12-22'::date, 70),
    (13, 3, 7,  7, '2025-12-14'::date, '2025-12-21'::date, 120),
    (14, 3, 12, 8, '2025-12-18'::date, '2025-12-25'::date, 30),
    (15, 3, 15, 9, '2025-12-19'::date, '2025-12-23'::date, 75),
    (16, 3, 16, 5, '2025-12-20'::date, '2025-12-24'::date, 65),
    (17, 3, 17, 4, '2025-12-20'::date, '2025-12-24'::date, 40),
    (18, 3, 26, 2, '2025-12-19'::date, '2025-12-22'::date, 50);
SELECT setval(pg_get_serial_sequence('batch', 'batch_id'), 18);

INSERT INTO batch_inventory (batch_id, inventory_id, quantity_used, unit_of_measure_at_time, usage_recorded_date) VALUES
    (1,  1,  18.500, 'kg',    '2025-12-14 12:00:00+00'),
    (1,  3,  6.000,  'kg',    '2025-12-14 12:00:00+00'),
    (2,  1,  22.000, 'kg',    '2025-12-17 12:00:00+00'),
    (2,  4,  18.000, 'L',     '2025-12-17 12:00:00+00'),
    (3,  1,  20.000, 'kg',    '2025-12-18 12:00:00+00'),
    (3,  3,  9.000,  'kg',    '2025-12-18 12:00:00+00'),
    (4,  1,  14.500, 'kg',    '2025-12-16 12:00:00+00'),
    (4,  2,  7.500,  'kg',    '2025-12-16 12:00:00+00'),
    (4,  8,  3.200,  'kg',    '2025-12-16 12:00:00+00'),
    (5,  1,  25.000, 'kg',    '2025-12-19 12:00:00+00'),
    (5,  2,  12.000, 'kg',    '2025-12-19 12:00:00+00'),
    (5,  8,  4.500,  'kg',    '2025-12-19 12:00:00+00'),
    (6,  1,  16.000, 'kg',    '2025-12-20 12:00:00+00'),
    (6,  2,  9.000,  'kg',    '2025-12-20 12:00:00+00'),
    (7,  9,  16.500, 'kg',    '2025-12-15 12:00:00+00'),
    (7,  11, 5.500,  'kg',    '2025-12-15 12:00:00+00'),
    (8,  9,  18.000, 'kg',    '2025-12-18 12:00:00+00'),
    (8,  10, 8.000,  'kg',    '2025-12-18 12:00:00+00'),
    (9,  9,  12.000, 'kg',    '2025-12-18 12:00:00+00'),
    (9,  10, 6.000,  'kg',    '2025-12-18 12:00:00+00'),
    (10, 9,  10.500, 'kg',    '2025-12-19 12:00:00+00'),
    (10, 10, 4.500,  'kg',    '2025-12-19 12:00:00+00'),
    (11, 9,  11.000, 'kg',    '2025-12-20 12:00:00+00'),
    (11, 10, 5.000,  'kg',    '2025-12-20 12:00:00+00'),
    (12, 17, 13.000, 'kg',    '2025-12-17 12:00:00+00'),
    (12, 19, 6.500,  'kg',    '2025-12-17 12:00:00+00'),
    (13, 17, 9.000,  'kg',    '2025-12-14 12:00:00+00'),
    (13, 20, 4.000,  'kg',    '2025-12-14 12:00:00+00'),
    (14, 17, 7.000,  'kg',    '2025-12-18 12:00:00+00'),
    (14, 18, 3.500,  'kg',    '2025-12-18 12:00:00+00'),
    (15, 21, 8.000,  'kg',    '2025-12-19 12:00:00+00'),
    (15, 22, 900.000,'count', '2025-12-19 12:00:00+00'),
    (16, 21, 6.500,  'kg',    '2025-12-20 12:00:00+00'),
    (16, 23, 1200.000,'count','2025-12-20 12:00:00+00'),
    (17, 20, 3.000,  'kg',    '2025-12-20 12:00:00+00'),
    (17, 21, 4.800,  'kg',    '2025-12-20 12:00:00+00'),
    (18, 17, 5.500,  'kg',    '2025-12-19 12:00:00+00'),
    (18, 18, 2.200,  'kg',    '2025-12-19 12:00:00+00');

INSERT INTO "order" (order_id, uuid, order_number, customer_id, bakery_id, address_id, order_placed_datetime, order_scheduled_datetime, order_delivered_datetime, order_method, order_comment, order_total, order_discount, order_status)
OVERRIDING SYSTEM VALUE VALUES
    (1, '40000000-0000-4000-8000-000000000001'::uuid, 'ORD-0001', 1, 3, 21, '2025-12-08 12:00:00+00', '2025-12-09 12:00:00+00', '2025-12-09 12:00:00+00', 'delivery'::order_method, 'Ring buzzer upon arrival', 26.95, 0.0, 'completed'::order_status),
    (2, '40000000-0000-4000-8000-000000000002'::uuid, 'ORD-0002', 2, 3, NULL, '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', 'pickup'::order_method, NULL, 12.98, 0.0, 'completed'::order_status),
    (3, '40000000-0000-4000-8000-000000000003'::uuid, 'ORD-0003', 3, 2, 23, '2025-12-11 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'delivery'::order_method, 'Leave with concierge', 34.2, 2.0, 'completed'::order_status),
    (4, '40000000-0000-4000-8000-000000000004'::uuid, 'ORD-0004', 4, 2, NULL, '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'pickup'::order_method, NULL, 9.75, 0.0, 'completed'::order_status),
    (5, '40000000-0000-4000-8000-000000000005'::uuid, 'ORD-0005', 5, 1, 26, '2025-12-13 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'delivery'::order_method, 'Call on arrival', 41.9, 4.0, 'completed'::order_status),
    (6, '40000000-0000-4000-8000-000000000006'::uuid, 'ORD-0006', 6, 1, NULL, '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'pickup'::order_method, NULL, 18.2, 0.0, 'completed'::order_status),
    (7, '40000000-0000-4000-8000-000000000007'::uuid, 'ORD-0007', 7, 1, 31, '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', NULL, 'delivery'::order_method, 'Please ensure items are sealed', 22.45, 0.0, 'scheduled'::order_status),
    (8, '40000000-0000-4000-8000-000000000008'::uuid, 'ORD-0008', 8, 1, NULL, '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', 'pickup'::order_method, NULL, 7.25, 0.0, 'completed'::order_status),
    (9, '40000000-0000-4000-8000-000000000009'::uuid, 'ORD-0009', 9, 3, 34, '2025-12-16 12:00:00+00', '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', 'delivery'::order_method, NULL, 58.48, 5.0, 'completed'::order_status),
    (10, '40000000-0000-4000-8000-000000000010'::uuid, 'ORD-0010', 10, 3, NULL, '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', NULL, 'pickup'::order_method, NULL, 6.49, 0.0, 'placed'::order_status),
    (11, '40000000-0000-4000-8000-000000000011'::uuid, 'ORD-0011', 11, 2, 36, '2025-12-17 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'delivery'::order_method, 'Front desk drop-off', 27.7, 0.0, 'completed'::order_status),
    (12, '40000000-0000-4000-8000-000000000012'::uuid, 'ORD-0012', 12, 2, NULL, '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'pickup'::order_method, NULL, 14.5, 0.0, 'completed'::order_status),
    (13, '40000000-0000-4000-8000-000000000013'::uuid, 'ORD-0013', 13, 1, 38, '2025-12-18 12:00:00+00', '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', 'delivery'::order_method, NULL, 19.95, 0.0, 'cancelled'::order_status),
    (14, '40000000-0000-4000-8000-000000000014'::uuid, 'ORD-0014', 14, 1, NULL, '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', NULL, 'pickup'::order_method, NULL, 29.99, 0.0, 'cancelled'::order_status),
    (15, '40000000-0000-4000-8000-000000000015'::uuid, 'ORD-0015', 8, 1, 41, '2026-03-25 15:31:54+00', '2026-04-03 18:00:00+00', NULL, 'pickup'::order_method, NULL, 17.45, 0.0, 'ready'::order_status);
SELECT setval(pg_get_serial_sequence('"order"', 'order_id'), (SELECT MAX(order_id) FROM "order"));

INSERT INTO order_item (order_item_id, order_id, product_id, batch_id, order_item_quantity, order_item_unit_price_at_time, order_item_line_total)
OVERRIDING SYSTEM VALUE VALUES
    (1,  1,  14, 10, 1, 7.25,  7.25),
    (2,  1,  8,  4,  2, 2.25,  4.50),
    (3,  1,  5,  3,  2, 3.95,  7.90),
    (4,  2,  1,  2,  2, 6.49,  12.98),
    (5,  3,  13, 5,  1, 29.99, 29.99),
    (6,  3,  8,  4,  1, 2.25,  2.25),
    (7,  3,  21, 6,  1, 3.75,  3.75),
    (8,  4,  18, 11, 1, 3.75,  3.75),
    (9,  4,  6,  8,  2, 3.00,  6.00),
    (10, 5,  12, 14, 2, 6.95,  13.90),
    (11, 5,  26, 18, 1, 5.25,  5.25),
    (12, 5,  5,  3,  2, 3.95,  7.90),
    (13, 5,  8,  4,  1, 2.25,  2.25),
    (14, 5,  17, 17, 1, 6.50,  6.50),
    (15, 6,  15, 15, 1, 4.10,  4.10),
    (16, 6,  16, 16, 1, 4.75,  4.75),
    (17, 6,  18, 11, 1, 3.75,  3.75),
    (18, 6,  8,  4,  2, 2.25,  4.50),
    (19, 6,  6,  8,  1, 3.10,  3.10),
    (20, 7,  10, 9,  2, 3.50,  7.00),
    (21, 7,  5,  3,  1, 3.95,  3.95),
    (22, 7,  1,  2,  1, 6.49,  6.49),
    (23, 7,  8,  4,  2, 2.25,  4.50),
    (24, 8,  14, 10, 1, 7.25,  7.25),
    (25, 9,  13, 5,  1, 29.99, 29.99),
    (26, 9,  12, 14, 1, 6.95,  6.95),
    (27, 9,  17, 17, 2, 6.50,  13.00),
    (28, 9,  26, 18, 1, 5.25,  5.25),
    (29, 9,  8,  4,  2, 2.25,  4.50),
    (30, 10,  1,  2,  1, 6.49,  6.49),
    (31, 11,  4,  12, 1, 4.25,  4.25),
    (32, 11,  5,  3,  1, 3.95,  3.95),
    (33, 11,  18, 11, 2, 3.75,  7.50),
    (34, 11,  14, 10, 1, 7.25,  7.25),
    (35, 11,  8,  4,  2, 2.25,  4.50),
    (36, 12,  6,  8,  2, 3.25,  6.50),
    (37, 12,  8,  4,  2, 2.25,  4.50),
    (38, 12,  9,  4,  1, 2.25,  2.25),
    (39, 12,  24, 7,  1, 3.25,  3.25),
    (40, 13,  21, 6,  2, 3.75,  7.50),
    (41, 13,  5,  3,  1, 3.95,  3.95),
    (42, 13,  16, 16, 1, 4.75,  4.75),
    (43, 13,  8,  4,  1, 2.25,  2.25),
    (44, 13,  6,  8,  1, 1.50,  1.50),
    (45, 14,  13, 5,  1, 29.99, 29.99),
    (46, 15,  3,  NULL,5, 3.49,  17.45);
SELECT setval(pg_get_serial_sequence('order_item', 'order_item_id'), 46);

INSERT INTO payment (payment_id, uuid, order_id, payment_amount, payment_method, payment_transaction_id, payment_status, payment_paid_at)
OVERRIDING SYSTEM VALUE VALUES
    (1, '50000000-0000-4000-8000-000000000001'::uuid, 1,  26.95, 'credit_card'::payment_method, 'TRX-98314501', 'completed'::payment_status, '2025-12-09 12:00:00+00'),
    (2, '50000000-0000-4000-8000-000000000002'::uuid, 2,  12.98, 'debit_card'::payment_method,  'TRX-98314502', 'completed'::payment_status, '2025-12-10 12:00:00+00'),
    (3, '50000000-0000-4000-8000-000000000003'::uuid, 3,  32.20, 'credit_card'::payment_method, 'TRX-98314503', 'completed'::payment_status, '2025-12-12 12:00:00+00'),
    (4, '50000000-0000-4000-8000-000000000004'::uuid, 4,  9.75,  'credit_card'::payment_method, 'TRX-98314504', 'completed'::payment_status, '2025-12-12 12:00:00+00'),
    (5, '50000000-0000-4000-8000-000000000005'::uuid, 5,  37.90, 'credit_card'::payment_method, 'TRX-98314505', 'completed'::payment_status, '2025-12-14 12:00:00+00'),
    (6, '50000000-0000-4000-8000-000000000006'::uuid, 6,  18.20, 'debit_card'::payment_method,  'TRX-98314506', 'completed'::payment_status, '2025-12-14 12:00:00+00'),
    (7, '50000000-0000-4000-8000-000000000007'::uuid, 7,  22.45, 'credit_card'::payment_method, 'TRX-98314507', 'authorized'::payment_status, NULL),
    (8, '50000000-0000-4000-8000-000000000008'::uuid, 8,  7.25,  'credit_card'::payment_method, 'TRX-98314508', 'completed'::payment_status, '2025-12-15 12:00:00+00'),
    (9, '50000000-0000-4000-8000-000000000009'::uuid, 9,  53.48, 'credit_card'::payment_method, 'TRX-98314509', 'completed'::payment_status, '2025-12-17 12:00:00+00'),
    (10, '50000000-0000-4000-8000-000000000010'::uuid, 10, 6.49,  'debit_card'::payment_method,  'TRX-98314510', 'pending'::payment_status, NULL),
    (11, '50000000-0000-4000-8000-000000000011'::uuid, 11, 27.70, 'credit_card'::payment_method, 'TRX-98314511', 'completed'::payment_status, '2025-12-18 12:00:00+00'),
    (12, '50000000-0000-4000-8000-000000000012'::uuid, 12, 14.50, 'credit_card'::payment_method, 'TRX-98314512', 'completed'::payment_status, '2025-12-18 12:00:00+00'),
    (13, '50000000-0000-4000-8000-000000000013'::uuid, 13, 19.95, 'debit_card'::payment_method,  'TRX-98314513', 'completed'::payment_status, '2025-12-19 12:00:00+00'),
    (14, '50000000-0000-4000-8000-000000000014'::uuid, 14, 29.99, 'credit_card'::payment_method, 'TRX-98314514', 'pending'::payment_status, NULL);
SELECT setval(pg_get_serial_sequence('payment', 'payment_id'), (SELECT MAX(payment_id) FROM payment));

INSERT INTO reward (reward_id, uuid, customer_id, order_id, reward_points_earned, reward_transaction_date)
OVERRIDING SYSTEM VALUE VALUES
    (1, '60000000-0000-4000-8000-000000000001'::uuid, 1, 1,  26950, '2025-12-09 12:00:00+00'),
    (2, '60000000-0000-4000-8000-000000000002'::uuid, 2, 2,  12980, '2025-12-10 12:00:00+00'),
    (3, '60000000-0000-4000-8000-000000000003'::uuid, 3, 3,  32200, '2025-12-12 12:00:00+00'),
    (4, '60000000-0000-4000-8000-000000000004'::uuid, 4, 4,  9750,  '2025-12-12 12:00:00+00'),
    (5, '60000000-0000-4000-8000-000000000005'::uuid, 5, 5,  37900, '2025-12-14 12:00:00+00'),
    (6, '60000000-0000-4000-8000-000000000006'::uuid, 6, 6,  18200, '2025-12-14 12:00:00+00'),
    (7, '60000000-0000-4000-8000-000000000007'::uuid, 8, 8,  7250,  '2025-12-15 12:00:00+00'),
    (8, '60000000-0000-4000-8000-000000000008'::uuid, 9, 9,  53480, '2025-12-17 12:00:00+00'),
    (9, '60000000-0000-4000-8000-000000000009'::uuid, 11, 11, 27700, '2025-12-18 12:00:00+00'),
    (10, '60000000-0000-4000-8000-000000000010'::uuid, 12, 12, 14500, '2025-12-18 12:00:00+00'),
    (11, '60000000-0000-4000-8000-000000000011'::uuid, 13, 13, 19950, '2025-12-19 12:00:00+00');
SELECT setval(pg_get_serial_sequence('reward', 'reward_id'), (SELECT MAX(reward_id) FROM reward));

INSERT INTO review (review_id, uuid, customer_id, product_id, employee_id, review_rating, review_comment, review_submitted_date, review_status, review_approval_date)
OVERRIDING SYSTEM VALUE VALUES
    (1, '70000000-0000-4000-8000-000000000001'::uuid, 1,  5,  7, 5, 'Fresh and flaky, exactly what I hoped for.',     '2025-12-11 12:00:00+00', 'approved'::review_status, '2025-12-12 12:00:00+00'),
    (2, '70000000-0000-4000-8000-000000000002'::uuid, 2,  1,  7, 4, 'Good loaf with a nice crust.',                  '2025-12-12 12:00:00+00', 'approved'::review_status, '2025-12-13 12:00:00+00'),
    (3, '70000000-0000-4000-8000-000000000003'::uuid, 3,  13, 7, 5, 'Excellent cake, rich and not overly sweet.',     '2025-12-13 12:00:00+00', 'approved'::review_status, '2025-12-14 12:00:00+00'),
    (4, '70000000-0000-4000-8000-000000000004'::uuid, 4,  6,  NULL, 4, 'Muffin was soft and well-balanced.',             '2025-12-13 12:00:00+00', 'pending'::review_status, NULL),
    (5, '70000000-0000-4000-8000-000000000005'::uuid, 5,  12, 7, 5, 'Great flavour and texture.',                     '2025-12-14 12:00:00+00', 'approved'::review_status, '2025-12-15 12:00:00+00'),
    (6, '70000000-0000-4000-8000-000000000006'::uuid, 6,  16, 7, 3, 'Filling was good, pastry slightly dry.',         '2025-12-14 12:00:00+00', 'approved'::review_status, '2025-12-15 12:00:00+00'),
    (7, '70000000-0000-4000-8000-000000000007'::uuid, 7,  10, NULL, 4, 'Cupcake was moist and frosting was smooth.',     '2025-12-15 12:00:00+00', 'pending'::review_status, NULL),
    (8, '70000000-0000-4000-8000-000000000008'::uuid, 8,  14, 7, 5, 'Very creamy slice and good crust.',              '2025-12-15 12:00:00+00', 'approved'::review_status, '2025-12-16 12:00:00+00'),
    (9, '70000000-0000-4000-8000-000000000009'::uuid, 9,  17, 7, 4, 'Bright flavour and a nice finish.',              '2025-12-16 12:00:00+00', 'approved'::review_status, '2025-12-17 12:00:00+00'),
    (10, '70000000-0000-4000-8000-000000000010'::uuid, 10, 3,  NULL, 4, 'Crisp outside and soft inside.',                 '2025-12-17 12:00:00+00', 'pending'::review_status, NULL),
    (11, '70000000-0000-4000-8000-000000000011'::uuid, 11, 18, 7, 5, 'Perfect brownie, very fudgy.',                   '2025-12-18 12:00:00+00', 'approved'::review_status, '2025-12-18 12:00:00+00'),
    (12, '70000000-0000-4000-8000-000000000012'::uuid, 12, 8,  7, 4, 'Classic cookie, good texture.',                  '2025-12-18 12:00:00+00', 'approved'::review_status, '2025-12-19 12:00:00+00'),
    (13, '70000000-0000-4000-8000-000000000013'::uuid, 13, 21, NULL, 4, 'Nice seasonal option, would buy again.',         '2025-12-19 12:00:00+00', 'pending'::review_status, NULL),
    (14, '70000000-0000-4000-8000-000000000014'::uuid, 14, 13, NULL, 5, 'Great for an occasion, everyone enjoyed it.',    '2025-12-19 12:00:00+00', 'pending'::review_status, NULL);
SELECT setval(pg_get_serial_sequence('review', 'review_id'), (SELECT MAX(review_id) FROM review));

INSERT INTO customer_preference (customer_id, tag_id, preference_type, preference_strength) VALUES
    (1, 1, 'like'::preference_type, 4::smallint),
    (1, 10, 'like'::preference_type, 3::smallint),
    (2, 5, 'dislike'::preference_type, 4::smallint),
    (2, 9, 'like'::preference_type, 3::smallint),
    (3, 2, 'like'::preference_type, 4::smallint),
    (3, 10, 'like'::preference_type, 4::smallint),
    (4, 4, 'like'::preference_type, 3::smallint),
    (4, 11, 'allergic'::preference_type, 5::smallint),
    (5, 3, 'like'::preference_type, 4::smallint),
    (5, 7, 'like'::preference_type, 3::smallint),
    (6, 6, 'dislike'::preference_type, 4::smallint),
    (6, 8, 'like'::preference_type, 3::smallint),
    (7, 5, 'allergic'::preference_type, 5::smallint),
    (7, 9, 'like'::preference_type, 3::smallint),
    (8, 2, 'like'::preference_type, 4::smallint),
    (8, 10, 'like'::preference_type, 4::smallint),
    (9, 7, 'like'::preference_type, 4::smallint),
    (9, 9, 'like'::preference_type, 3::smallint),
    (10, 4, 'like'::preference_type, 3::smallint),
    (10, 11, 'allergic'::preference_type, 5::smallint),
    (11, 3, 'dislike'::preference_type, 2::smallint),
    (11, 9, 'like'::preference_type, 3::smallint),
    (12, 6, 'dislike'::preference_type, 4::smallint),
    (12, 10, 'like'::preference_type, 4::smallint),
    (13, 5, 'allergic'::preference_type, 5::smallint),
    (13, 7, 'like'::preference_type, 3::smallint),
    (14, 2, 'like'::preference_type, 4::smallint),
    (14, 10, 'like'::preference_type, 3::smallint);
