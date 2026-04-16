-- =============================================================================
-- V47__reference_seed_v0.sql — Flyway VERSIONED seed migration (PostgreSQL)
-- Converted from legacy repeatable seed to a one-time ordered migration.
--
-- Pipeline (optional): classpath V0–V41+ → V43__drop_and_recreate_empty_schema.sql → this file
-- (point Flyway locations at this repo root, or copy these files into your Flyway locations).
--
-- NOTE: table row wipe now occurs in V46__wipe_reference_data_rows.sql.
--
-- Intended for local / CI / disposable databases. Do not run against
-- production unless you explicitly adopt wipe-and-reseed.
--
-- Passwords (bcrypt, same as legacy V3):
--   Admin123!   — admin (john.doe)
--   Emp123!     — all employees
--   Cust123!    — all customers
--
-- Changes vs fragmented V3 + follow-ups:
--   Login / contact emails: <username>@bakery.ca (matches user.username for people).
--   (admin username john.doe per V15); no user profile photos (NULL paths).
--   Product + bakery image URLs = final Spaces literals (same paths V12/V13 used; not re-applied in V43).
--   Reviews follow V27 rules (product vs location); employee_customer_link demo.
--   Orders include tax + V38 discount breakdown columns.
--   Loyalty: customer_reward_balance matches sum(reward_points_earned) for that
--   customer; reward_tier_id matches balance vs reward_tier thresholds. No points
--   for cancelled orders; payments for cancelled orders are refunded / not earned.
--   Fulfillment: each order_item.batch_id references a batch with the same bakery_id
--   as the parent order and the same product_id as the line (plus batch_inventory).
--   Reviews: product/location moderation employee works at review.bakery_id (or the
--   order’s bakery for service reviews).
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- address (same geometry as V3)
-- ---------------------------------------------------------------------------
INSERT INTO address (address_id, address_line1, address_line2, address_city, address_province, address_postal_code)
OVERRIDING SYSTEM VALUE VALUES
    (1,  '528 17 Ave SW',              NULL,         'Calgary',    'AB', 'T2S 0B1'),
    (2,  '33 10 St NW',                NULL,         'Calgary',    'AB', 'T2N 1V4'),
    (3,  '455 7 Ave SE',               NULL,         'Calgary',    'AB', 'T2G 0J8'),
    (4,  '9805 12 Ave SW',             NULL,         'Calgary',    'AB', 'T2W 1K1'),
    (5,  '2100 16 Ave NW',             'Unit 14',    'Calgary',    'AB', 'T2M 0M5'),
    (6,  '8715 Macleod Trail SE',      'Unit 120',   'Calgary',    'AB', 'T2H 0M3'),
    (7,  '101 9 Ave SW',               'Floor 6',    'Calgary',    'AB', 'T2P 1J9'),
    (8,  '560 2 St SW',                NULL,         'Calgary',    'AB', 'T2P 0S6'),
    (9,  '1180 7 St SW',               NULL,         'Calgary',    'AB', 'T2R 1A5'),
    (10, '4020 4 St NW',               NULL,         'Calgary',    'AB', 'T2K 1A2'),
    (11, '10330 82 Ave NW',            NULL,         'Edmonton',   'AB', 'T6E 1Z8'),
    (12, '815 104 Ave NW',             'Unit 5',     'Edmonton',   'AB', 'T5H 0L1'),
    (13, '10425 Jasper Ave',           'Suite 300',  'Edmonton',   'AB', 'T5J 1Z7'),
    (14, '8900 99 St NW',              NULL,         'Edmonton',   'AB', 'T6E 3T9'),
    (15, '150 109 St NW',              'Unit 2',     'Edmonton',   'AB', 'T5J 2X6'),
    (16, '200 Granville St',           'Unit 110',   'Vancouver',  'BC', 'V6C 1S4'),
    (17, '845 Burrard St',             NULL,         'Vancouver',  'BC', 'V6Z 2K6'),
    (18, '1155 W Georgia St',          'Suite 900',  'Vancouver',  'BC', 'V6E 4T6'),
    (19, '777 Hornby St',              NULL,         'Vancouver',  'BC', 'V6Z 1S4'),
    (20, '120 Front St E',             NULL,         'Toronto',    'ON', 'M5A 4T9'),
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

INSERT INTO tag (tag_id, tag_name, is_dietary) OVERRIDING SYSTEM VALUE VALUES
    (1,  'Bread', FALSE),
    (2,  'Cake', FALSE),
    (3,  'Pastry', FALSE),
    (4,  'Cookie', FALSE),
    (5,  'Gluten-Free', TRUE),
    (6,  'Dairy-Free', TRUE),
    (7,  'Seasonal', FALSE),
    (8,  'Vegan', TRUE),
    (9,  'Breakfast', FALSE),
    (10, 'Dessert', FALSE),
    (11, 'Whole Grain', TRUE);
SELECT setval(pg_get_serial_sequence('tag', 'tag_id'), 11);

-- ---------------------------------------------------------------------------
-- user — short emails; no profile photos; john.doe admin (V15 alignment)
-- ---------------------------------------------------------------------------
INSERT INTO "user" (user_id, uuid, username, user_email, user_password_hash, user_role, is_active, user_created_at, profile_photo_path, photo_approval_pending) VALUES
    ('10000000-0000-4000-8000-000000000001'::uuid, '10000000-0000-4000-8000-000000000001'::uuid, 'john.doe',         'john.doe@bakery.ca',         '$2b$10$R92cP1dmtXyTDQYq6U53V.fRED4Kb9JHcAscibP8XAc7W1Zzm2hXm', 'admin'::user_role,    TRUE, '2025-08-22 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000002'::uuid, '10000000-0000-4000-8000-000000000002'::uuid, 'mason.clark',      'mason.clark@bakery.ca',      '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-16 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000003'::uuid, '10000000-0000-4000-8000-000000000003'::uuid, 'sophia.patel',     'sophia.patel@bakery.ca',     '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-21 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000004'::uuid, '10000000-0000-4000-8000-000000000004'::uuid, 'ethan.wright',     'ethan.wright@bakery.ca',     '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-09-29 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000005'::uuid, '10000000-0000-4000-8000-000000000005'::uuid, 'isabella.chen',    'isabella.chen@bakery.ca',    '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-01 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000006'::uuid, '10000000-0000-4000-8000-000000000006'::uuid, 'noah.martin',      'noah.martin@bakery.ca',      '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-05 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000007'::uuid, '10000000-0000-4000-8000-000000000007'::uuid, 'ava.roberts',      'ava.roberts@bakery.ca',      '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-11 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000008'::uuid, '10000000-0000-4000-8000-000000000008'::uuid, 'logan.scott',      'logan.scott@bakery.ca',      '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-15 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000009'::uuid, '10000000-0000-4000-8000-000000000009'::uuid, 'mia.kim',          'mia.kim@bakery.ca',          '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-19 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000010'::uuid, '10000000-0000-4000-8000-000000000010'::uuid, 'jackson.hall',     'jackson.hall@bakery.ca',     '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2025-10-21 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000011'::uuid, '10000000-0000-4000-8000-000000000011'::uuid, 'olivia.brown',     'olivia.brown@bakery.ca',     '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-23 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000012'::uuid, '10000000-0000-4000-8000-000000000012'::uuid, 'liam.thompson',    'liam.thompson@bakery.ca',    '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-26 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000013'::uuid, '10000000-0000-4000-8000-000000000013'::uuid, 'emma.wilson',      'emma.wilson@bakery.ca',      '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-10-31 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000014'::uuid, '10000000-0000-4000-8000-000000000014'::uuid, 'benjamin.lee',     'benjamin.lee@bakery.ca',     '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-05 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000015'::uuid, '10000000-0000-4000-8000-000000000015'::uuid, 'amelia.johnson',   'amelia.johnson@bakery.ca',   '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-08 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000016'::uuid, '10000000-0000-4000-8000-000000000016'::uuid, 'lucas.anderson',   'lucas.anderson@bakery.ca',   '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-10 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000017'::uuid, '10000000-0000-4000-8000-000000000017'::uuid, 'charlotte.miller', 'charlotte.miller@bakery.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-12 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000018'::uuid, '10000000-0000-4000-8000-000000000018'::uuid, 'henry.davis',      'henry.davis@bakery.ca',      '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-15 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000019'::uuid, '10000000-0000-4000-8000-000000000019'::uuid, 'evelyn.moore',     'evelyn.moore@bakery.ca',     '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-20 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000020'::uuid, '10000000-0000-4000-8000-000000000020'::uuid, 'daniel.taylor',    'daniel.taylor@bakery.ca',    '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-22 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000021'::uuid, '10000000-0000-4000-8000-000000000021'::uuid, 'harper.jackson',   'harper.jackson@bakery.ca',   '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-26 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000022'::uuid, '10000000-0000-4000-8000-000000000022'::uuid, 'sebastian.white',  'sebastian.white@bakery.ca',  '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-28 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000023'::uuid, '10000000-0000-4000-8000-000000000023'::uuid, 'nora.harris',      'nora.harris@bakery.ca',      '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-11-30 12:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000024'::uuid, '10000000-0000-4000-8000-000000000024'::uuid, 'wyatt.martinez',   'wyatt.martinez@bakery.ca',   '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2025-12-02 12:00:00+00', NULL, FALSE);

-- Test accounts — easy-to-remember credentials for manual testing
-- admin_test / Admin123!    employee_test / Emp123!    customer_test / Cust123!
INSERT INTO "user" (user_id, uuid, username, user_email, user_password_hash, user_role, is_active, user_created_at, profile_photo_path, photo_approval_pending) VALUES
    ('10000000-0000-4000-8000-000000000025'::uuid, '10000000-0000-4000-8000-000000000025'::uuid, 'admin_test',    'admin_test@bakery.ca',    '$2b$10$R92cP1dmtXyTDQYq6U53V.fRED4Kb9JHcAscibP8XAc7W1Zzm2hXm', 'admin'::user_role,    TRUE, '2026-01-01 00:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000026'::uuid, '10000000-0000-4000-8000-000000000026'::uuid, 'employee_test', 'employee_test@bakery.ca', '$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy', 'employee'::user_role, TRUE, '2026-01-01 00:00:00+00', NULL, FALSE),
    ('10000000-0000-4000-8000-000000000027'::uuid, '10000000-0000-4000-8000-000000000027'::uuid, 'customer_test', 'customer_test@bakery.ca', '$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe', 'customer'::user_role, TRUE, '2026-01-01 00:00:00+00', NULL, FALSE);

-- Bakery hero URLs (V12 canonical /bakery/ paths)
INSERT INTO bakery (bakery_id, address_id, bakery_name, bakery_phone, bakery_email, status, latitude, longitude, bakery_image_url)
OVERRIDING SYSTEM VALUE VALUES
    (1, 1,  'Copper Whisk Beltline',                   '(403) 555-2101', 'beltline@bakery.ca',               'open'::bakery_status, 51.038100, -114.075900,
     'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-downtown.jpg'),
    (2, 11, 'Strathcona Rye House',                    '(780) 555-4302', 'strathcona@bakery.ca',             'open'::bakery_status, 53.518900, -113.501800,
     'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-edmonton-central.jpg'),
    (3, 20, 'Front Street Proof',                      '(416) 555-9012', 'frontst@bakery.ca',                'open'::bakery_status, 43.648900, -79.371300,
     'https://peelin-good-storage.tor1.digitaloceanspaces.com/bakery/north-harbour-bakery-toronto-financial.jpg');
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

-- ---------------------------------------------------------------------------
-- product — V3 catalog; image URLs = merged V12 + V13 (DigitalOcean Spaces)
-- ---------------------------------------------------------------------------
INSERT INTO product (product_id, product_name, product_description, product_base_price, product_image_url)
OVERRIDING SYSTEM VALUE VALUES
    (1,  'Sourdough Loaf',            'Naturally leavened sourdough bread',                    6.49,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/sourdough.jpg'),
    (2,  'Multigrain Sandwich Bread',  'Whole grain sandwich loaf',                            5.99,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/multigrain-sandwich-bread.jpg'),
    (3,  'Baguette',                   'Classic French-style baguette',                        3.49,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/baguette.png'),
    (4,  'Cinnamon Roll',             'Soft roll with cinnamon filling and glaze',             4.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/cinnamon-roll.jpg'),
    (5,  'Butter Croissant',          'Flaky butter croissant',                                3.95,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/butter-croissant.jpg'),
    (6,  'Blueberry Muffin',          'Muffin with blueberries',                               3.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/blueberry-muffin.jpg'),
    (7,  'Banana Bread Slice',        'Moist banana bread slice',                              2.95,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/banana-bread-slice.jpg'),
    (8,  'Chocolate Chip Cookie',     'Cookie with chocolate chips',                           2.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-chip-cookie.jpg'),
    (9,  'Oatmeal Raisin Cookie',     'Oatmeal cookie with raisins',                          2.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/oatmeal-raisin-cookie.jpg'),
    (10, 'Vanilla Cupcake',           'Vanilla cupcake with buttercream',                      3.50,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/vanilla-cupcake.jpg'),
    (11, 'Chocolate Cupcake',         'Chocolate cupcake with buttercream',                    3.50,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-cupcake.jpg'),
    (12, 'Carrot Cake Slice',         'Carrot cake slice with cream cheese icing',             6.95,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/carrot-cake-slice.jpg'),
    (13, 'Chocolate Layer Cake',      'Chocolate cake with ganache',                           29.99, 'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-layer-cake.jpg'),
    (14, 'Cheesecake Slice',          'Classic cheesecake slice',                              7.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/cheesecake-slice.jpg'),
    (15, 'Apple Turnover',            'Puff pastry turnover with apple filling',               4.10,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/apple-turnover.jpg'),
    (16, 'Spinach Feta Danish',       'Danish pastry with spinach and feta',                   4.75,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/spinach-feta-danish.jpg'),
    (17, 'Lemon Tart',               'Tart with lemon curd filling',                          6.50,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/lemon-tart.jpg'),
    (18, 'Brownie',                   'Fudgy chocolate brownie',                               3.75,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/brownie.jpg'),
    (19, 'Vegan Chocolate Brownie',   'Dairy-free brownie',                                    4.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/vegan-chocolate-brownie.jpg'),
    (20, 'Gluten-Free Banana Muffin', 'Gluten-free banana muffin',                            3.95,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/gluten-free-banana-muffin.jpg'),
    (21, 'Seasonal Pumpkin Muffin',   'Pumpkin spice muffin',                                  3.75,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/seasonal-pumpkin-muffin.jpg'),
    (22, 'Strawberry Shortcake Cup',  'Layered shortcake with strawberries',                   6.95,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/strawberry-shortcake-cup.jpg'),
    (23, 'Almond Biscotti',           'Twice-baked almond biscotti',                           2.75,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/almond-biscotti.jpg'),
    (24, 'Whole Wheat Scone',         'Scone made with whole wheat flour',                     3.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/whole-wheat-scone.jpg'),
    (25, 'Raspberry Danish',          'Danish pastry with raspberry filling',                   4.75,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/raspberry-danish.jpg'),
    (26, 'Chocolate Eclair',          'Choux pastry with cream and chocolate topping',         5.25,  'https://peelin-good-storage.tor1.digitaloceanspaces.com/products/chocolate-eclair.jpg');
SELECT setval(pg_get_serial_sequence('product', 'product_id'), 26);

INSERT INTO product_tag (product_id, tag_id) VALUES
    (1, 1), (2, 1), (2, 11), (3, 1), (4, 3), (4, 9), (5, 3), (5, 9), (6, 3), (6, 9), (7, 9),
    (8, 4), (8, 10), (9, 4), (9, 10), (10, 2), (10, 10), (11, 2), (11, 10), (12, 2), (12, 10), (13, 2), (13, 10),
    (14, 2), (14, 10), (15, 3), (15, 10), (16, 3), (17, 10), (18, 10), (19, 6), (19, 8), (20, 5), (21, 7), (22, 10),
    (23, 4), (24, 11), (25, 3), (26, 3);

INSERT INTO supplier (supplier_id, address_id, supplier_name, supplier_phone, supplier_email)
OVERRIDING SYSTEM VALUE VALUES
    (1, 29, 'Prairie Wholesale Ingredients',  '(403) 555-7001', 'prairie@bakery.ca'),
    (2, 30, 'Summit Packaging Supply',        '(403) 555-7002', 'summit@bakery.ca'),
    (3, 33, 'Riverbend Dairy Co.',            '(780) 555-7003', 'riverbend@bakery.ca'),
    (4, 16, 'Coastal Produce Distributors',   '(604) 555-7004', 'coastal@bakery.ca'),
    (5, 24, 'St. Lawrence Dry Goods',         '(514) 555-7005', 'stlawrence@bakery.ca');
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

-- Work email = same username@bakery.ca as "user".user_email for that employee
INSERT INTO employee (employee_id, uuid, user_id, address_id, bakery_id, employee_first_name, employee_middle_initial, employee_last_name, employee_position, employee_phone, employee_business_phone, employee_work_email) VALUES
    ('30000000-0000-4000-8000-000000000001'::uuid, '30000000-0000-4000-8000-000000000001'::uuid, '10000000-0000-4000-8000-000000000002'::uuid, 2,  1, 'Mason',   NULL,  'Clark',   'Baker',            '(403) 555-3101', '(403) 555-4101', 'mason.clark@bakery.ca'),
    ('30000000-0000-4000-8000-000000000002'::uuid, '30000000-0000-4000-8000-000000000002'::uuid, '10000000-0000-4000-8000-000000000003'::uuid, 3,  1, 'Sophia',  'R ',  'Patel',   'Baker',            '(403) 555-3102', '(403) 555-4102', 'sophia.patel@bakery.ca'),
    ('30000000-0000-4000-8000-000000000003'::uuid, '30000000-0000-4000-8000-000000000003'::uuid, '10000000-0000-4000-8000-000000000004'::uuid, 4,  1, 'Ethan',   NULL,  'Wright',  'Shift Lead',       '(403) 555-3103', '(403) 555-4103', 'ethan.wright@bakery.ca'),
    ('30000000-0000-4000-8000-000000000004'::uuid, '30000000-0000-4000-8000-000000000004'::uuid, '10000000-0000-4000-8000-000000000005'::uuid, 5,  1, 'Isabella','M ',  'Chen',    'Baker',            '(403) 555-3104', '(403) 555-4104', 'isabella.chen@bakery.ca'),
    ('30000000-0000-4000-8000-000000000005'::uuid, '30000000-0000-4000-8000-000000000005'::uuid, '10000000-0000-4000-8000-000000000006'::uuid, 6,  2, 'Noah',    NULL,  'Martin',  'Baker',            '(403) 555-3105', '(403) 555-4105', 'noah.martin@bakery.ca'),
    ('30000000-0000-4000-8000-000000000006'::uuid, '30000000-0000-4000-8000-000000000006'::uuid, '10000000-0000-4000-8000-000000000007'::uuid, 7,  2, 'Ava',     NULL,  'Roberts', 'Customer Support', '(403) 555-3106', '(403) 555-4106', 'ava.roberts@bakery.ca'),
    ('30000000-0000-4000-8000-000000000007'::uuid, '30000000-0000-4000-8000-000000000007'::uuid, '10000000-0000-4000-8000-000000000008'::uuid, 8,  2, 'Logan',   'J ',  'Scott',   'Quality Control',  '(403) 555-3107', '(403) 555-4107', 'logan.scott@bakery.ca'),
    ('30000000-0000-4000-8000-000000000008'::uuid, '30000000-0000-4000-8000-000000000008'::uuid, '10000000-0000-4000-8000-000000000009'::uuid, 9,  3, 'Mia',     NULL,  'Kim',     'Baker',            '(403) 555-3108', '(403) 555-4108', 'mia.kim@bakery.ca'),
    ('30000000-0000-4000-8000-000000000009'::uuid, '30000000-0000-4000-8000-000000000009'::uuid, '10000000-0000-4000-8000-000000000010'::uuid, 10, 3, 'Jackson', NULL,  'Hall',    'Baker',            '(403) 555-3109', '(403) 555-4109', 'jackson.hall@bakery.ca'),
    ('30000000-0000-4000-8000-000000000011'::uuid, '30000000-0000-4000-8000-000000000011'::uuid, '10000000-0000-4000-8000-000000000001'::uuid, 1,  1, 'John',    NULL,  'Doe',     'Admin',            '(403) 555-3110', '(403) 555-4110', 'john.doe@bakery.ca');

INSERT INTO employee (employee_id, uuid, user_id, address_id, bakery_id, employee_first_name, employee_middle_initial, employee_last_name, employee_position, employee_phone, employee_business_phone, employee_work_email) VALUES
    ('30000000-0000-4000-8000-000000000012'::uuid, '30000000-0000-4000-8000-000000000012'::uuid, '10000000-0000-4000-8000-000000000025'::uuid, 1, 1, 'Admin',    NULL, 'Test', 'Admin', '(403) 555-9901', '(403) 555-9911', 'admin_test@bakery.ca'),
    ('30000000-0000-4000-8000-000000000013'::uuid, '30000000-0000-4000-8000-000000000013'::uuid, '10000000-0000-4000-8000-000000000026'::uuid, 2, 1, 'Employee', NULL, 'Test', 'Baker', '(403) 555-9902', '(403) 555-9912', 'employee_test@bakery.ca');

-- reward_tier: 1 Bronze 0–99999, 2 Silver 100k–249999, 3 Gold 250k–499999, 4 Platinum 500k+
-- Balances = sum of reward rows below (0 if none); tier matches balance.
INSERT INTO customer (customer_id, uuid, user_id, address_id, reward_tier_id, customer_first_name, customer_middle_initial, customer_last_name, customer_phone, customer_business_phone, customer_email, customer_reward_balance, customer_tier_assigned_date, guest_expiry_date) VALUES
    ('20000000-0000-4000-8000-000000000001'::uuid, '20000000-0000-4000-8000-000000000001'::uuid, '10000000-0000-4000-8000-000000000011'::uuid, 21, 1, 'Olivia',    NULL,  'Brown',    '(416) 555-1201', NULL, 'olivia.brown@bakery.ca',     26950,   '2025-12-09', NULL),
    ('20000000-0000-4000-8000-000000000002'::uuid, '20000000-0000-4000-8000-000000000002'::uuid, '10000000-0000-4000-8000-000000000012'::uuid, 22, 1, 'Liam',      NULL,  'Thompson', '(416) 555-1202', NULL, 'liam.thompson@bakery.ca',  12980,   '2025-12-10', NULL),
    ('20000000-0000-4000-8000-000000000003'::uuid, '20000000-0000-4000-8000-000000000003'::uuid, '10000000-0000-4000-8000-000000000013'::uuid, 23, 1, 'Emma',      'J',   'Wilson',   '(514) 555-1203', NULL, 'emma.wilson@bakery.ca',    32200,   '2025-12-12', NULL),
    ('20000000-0000-4000-8000-000000000004'::uuid, '20000000-0000-4000-8000-000000000004'::uuid, '10000000-0000-4000-8000-000000000014'::uuid, 25, 1, 'Benjamin',  NULL,  'Lee',      '(613) 555-1204', NULL, 'benjamin.lee@bakery.ca',   9750,    '2025-12-12', NULL),
    ('20000000-0000-4000-8000-000000000005'::uuid, '20000000-0000-4000-8000-000000000005'::uuid, '10000000-0000-4000-8000-000000000015'::uuid, 26, 1, 'Amelia',    NULL,  'Johnson',  '(613) 555-1205', NULL, 'amelia.johnson@bakery.ca', 37900,   '2025-12-14', NULL),
    ('20000000-0000-4000-8000-000000000006'::uuid, '20000000-0000-4000-8000-000000000006'::uuid, '10000000-0000-4000-8000-000000000016'::uuid, 27, 1, 'Lucas',     'A',   'Anderson', '(613) 555-1206', NULL, 'lucas.anderson@bakery.ca', 20200,   '2025-12-14', NULL),
    ('20000000-0000-4000-8000-000000000007'::uuid, '20000000-0000-4000-8000-000000000007'::uuid, '10000000-0000-4000-8000-000000000017'::uuid, 31, 1, 'Charlotte', NULL,  'Miller',   '(403) 555-1207', NULL, 'charlotte.miller@bakery.ca', 0,     '2025-10-23', NULL),
    ('20000000-0000-4000-8000-000000000008'::uuid, '20000000-0000-4000-8000-000000000008'::uuid, '10000000-0000-4000-8000-000000000018'::uuid, 32, 1, 'Henry',     NULL,  'Davis',    '(403) 555-1208', NULL, 'henry.davis@bakery.ca',    7250,    '2025-12-15', NULL),
    ('20000000-0000-4000-8000-000000000009'::uuid, '20000000-0000-4000-8000-000000000009'::uuid, '10000000-0000-4000-8000-000000000019'::uuid, 34, 1, 'Evelyn',    NULL,  'Moore',    '(403) 555-1209', NULL, 'evelyn.moore@bakery.ca',   53480,   '2025-12-17', NULL),
    ('20000000-0000-4000-8000-000000000010'::uuid, '20000000-0000-4000-8000-000000000010'::uuid, '10000000-0000-4000-8000-000000000020'::uuid, 35, 1, 'Daniel',    NULL,  'Taylor',   '(403) 555-1210', NULL, 'daniel.taylor@bakery.ca',  0,       '2025-11-05', NULL),
    ('20000000-0000-4000-8000-000000000011'::uuid, '20000000-0000-4000-8000-000000000011'::uuid, '10000000-0000-4000-8000-000000000021'::uuid, 36, 1, 'Harper',    NULL,  'Jackson',  '(403) 555-1211', NULL, 'harper.jackson@bakery.ca', 27700,   '2025-12-18', NULL),
    ('20000000-0000-4000-8000-000000000012'::uuid, '20000000-0000-4000-8000-000000000012'::uuid, '10000000-0000-4000-8000-000000000022'::uuid, 37, 1, 'Sebastian', NULL,  'White',    '(403) 555-1212', NULL, 'sebastian.white@bakery.ca', 16500,   '2025-12-18', NULL),
    ('20000000-0000-4000-8000-000000000013'::uuid, '20000000-0000-4000-8000-000000000013'::uuid, '10000000-0000-4000-8000-000000000023'::uuid, 38, 1, 'Nora',      NULL,  'Harris',   '(403) 555-1213', NULL, 'nora.harris@bakery.ca',    0,       '2025-11-30', NULL),
    ('20000000-0000-4000-8000-000000000014'::uuid, '20000000-0000-4000-8000-000000000014'::uuid, '10000000-0000-4000-8000-000000000024'::uuid, 39, 1, 'Wyatt',     NULL,  'Martinez', '(403) 555-1214', NULL, 'wyatt.martinez@bakery.ca', 0,       '2025-12-02', NULL);

INSERT INTO customer (customer_id, uuid, user_id, address_id, reward_tier_id, customer_first_name, customer_middle_initial, customer_last_name, customer_phone, customer_business_phone, customer_email, customer_reward_balance, customer_tier_assigned_date, guest_expiry_date) VALUES
    ('20000000-0000-4000-8000-000000000015'::uuid, '20000000-0000-4000-8000-000000000015'::uuid, '10000000-0000-4000-8000-000000000027'::uuid, 21, 1, 'Customer', NULL, 'Test', '(403) 555-9903', NULL, 'customer_test@bakery.ca', 0, '2026-01-01', NULL);

INSERT INTO product_special (product_special_id, product_id, "date", discount_percent)
OVERRIDING SYSTEM VALUE VALUES
    (1, 1, DATE '2026-04-01', 10.00),
    (2, 1, DATE '2026-04-02', 10.00),
    (3, 4, DATE '2026-04-03', 15.00);
SELECT setval(pg_get_serial_sequence('product_special', 'product_special_id'), (SELECT MAX(product_special_id) FROM product_special));

INSERT INTO batch (batch_id, bakery_id, product_id, employee_id, batch_production_date, batch_expiry_date, batch_quantity_produced)
OVERRIDING SYSTEM VALUE VALUES
    (1,  1, 1,  '30000000-0000-4000-8000-000000000001'::uuid, '2025-12-14'::date, '2025-12-19'::date, 60),
    (2,  1, 3,  '30000000-0000-4000-8000-000000000002'::uuid, '2025-12-17'::date, '2025-12-22'::date, 90),
    (3,  1, 5,  '30000000-0000-4000-8000-000000000003'::uuid, '2025-12-18'::date, '2025-12-22'::date, 120),
    (4,  1, 8,  '30000000-0000-4000-8000-000000000004'::uuid, '2025-12-16'::date, '2025-12-26'::date, 200),
    (5,  1, 13, '30000000-0000-4000-8000-000000000003'::uuid, '2025-12-19'::date, '2025-12-23'::date, 12),
    (6,  1, 21, '30000000-0000-4000-8000-000000000002'::uuid, '2025-12-20'::date, '2025-12-25'::date, 80),
    (7,  2, 2,  '30000000-0000-4000-8000-000000000005'::uuid, '2025-12-15'::date, '2025-12-22'::date, 55),
    (8,  2, 6,  '30000000-0000-4000-8000-000000000006'::uuid, '2025-12-18'::date, '2025-12-23'::date, 140),
    (9,  2, 10, '30000000-0000-4000-8000-000000000007'::uuid, '2025-12-18'::date, '2025-12-24'::date, 110),
    (10, 2, 14, '30000000-0000-4000-8000-000000000007'::uuid, '2025-12-19'::date, '2025-12-23'::date, 40),
    (11, 2, 18, '30000000-0000-4000-8000-000000000005'::uuid, '2025-12-20'::date, '2025-12-26'::date, 90),
    (12, 3, 4,  '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-17'::date, '2025-12-22'::date, 70),
    (13, 3, 7,  '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-14'::date, '2025-12-21'::date, 120),
    (14, 3, 12, '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-18'::date, '2025-12-25'::date, 30),
    (15, 3, 15, '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-19'::date, '2025-12-23'::date, 75),
    (16, 3, 16, '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-20'::date, '2025-12-24'::date, 65),
    (17, 3, 17, '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-20'::date, '2025-12-24'::date, 40),
    (18, 3, 26, '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-19'::date, '2025-12-22'::date, 50),
    -- Extra batches so order_line batch_id matches order.bakery_id + product_id
    (19, 1, 6,  '30000000-0000-4000-8000-000000000001'::uuid, '2025-12-13'::date, '2025-12-20'::date, 100),
    (20, 1, 10, '30000000-0000-4000-8000-000000000002'::uuid, '2025-12-14'::date, '2025-12-21'::date, 95),
    (21, 1, 12, '30000000-0000-4000-8000-000000000003'::uuid, '2025-12-12'::date, '2025-12-19'::date, 45),
    (22, 1, 14, '30000000-0000-4000-8000-000000000004'::uuid, '2025-12-14'::date, '2025-12-22'::date, 55),
    (23, 1, 15, '30000000-0000-4000-8000-000000000001'::uuid, '2025-12-15'::date, '2025-12-22'::date, 80),
    (24, 1, 16, '30000000-0000-4000-8000-000000000002'::uuid, '2025-12-15'::date, '2025-12-22'::date, 70),
    (25, 1, 17, '30000000-0000-4000-8000-000000000003'::uuid, '2025-12-13'::date, '2025-12-20'::date, 50),
    (26, 1, 18, '30000000-0000-4000-8000-000000000004'::uuid, '2025-12-14'::date, '2025-12-23'::date, 85),
    (27, 1, 26, '30000000-0000-4000-8000-000000000002'::uuid, '2025-12-13'::date, '2025-12-21'::date, 60),
    (28, 2, 4,  '30000000-0000-4000-8000-000000000005'::uuid, '2025-12-16'::date, '2025-12-22'::date, 75),
    (29, 2, 5,  '30000000-0000-4000-8000-000000000006'::uuid, '2025-12-17'::date, '2025-12-22'::date, 130),
    (30, 2, 8,  '30000000-0000-4000-8000-000000000007'::uuid, '2025-12-16'::date, '2025-12-24'::date, 180),
    (31, 2, 9,  '30000000-0000-4000-8000-000000000005'::uuid, '2025-12-17'::date, '2025-12-23'::date, 160),
    (32, 2, 13, '30000000-0000-4000-8000-000000000006'::uuid, '2025-12-18'::date, '2025-12-23'::date, 20),
    (33, 2, 21, '30000000-0000-4000-8000-000000000007'::uuid, '2025-12-19'::date, '2025-12-24'::date, 75),
    (34, 2, 24, '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-17'::date, '2025-12-24'::date, 90),
    (35, 3, 1,  '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-09'::date, '2025-12-16'::date, 50),
    (36, 3, 5,  '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-07'::date, '2025-12-14'::date, 100),
    (37, 3, 8,  '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-06'::date, '2025-12-18'::date, 220),
    (38, 3, 13, '30000000-0000-4000-8000-000000000009'::uuid, '2025-12-10'::date, '2025-12-17'::date, 15),
    (39, 3, 14, '30000000-0000-4000-8000-000000000008'::uuid, '2025-12-08'::date, '2025-12-15'::date, 35);
SELECT setval(pg_get_serial_sequence('batch', 'batch_id'), 39);

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
    (18, 18, 2.200,  'kg',    '2025-12-19 12:00:00+00'),
    (19, 1,  10.000, 'kg',    '2025-12-13 11:00:00+00'),
    (20, 1,  9.500,  'kg',    '2025-12-14 11:00:00+00'),
    (21, 1,  8.000,  'kg',    '2025-12-12 11:00:00+00'),
    (22, 1,  9.200,  'kg',    '2025-12-14 11:00:00+00'),
    (23, 1,  7.800,  'kg',    '2025-12-15 11:00:00+00'),
    (24, 1,  7.200,  'kg',    '2025-12-15 11:00:00+00'),
    (25, 1,  6.500,  'kg',    '2025-12-13 11:00:00+00'),
    (26, 1,  8.800,  'kg',    '2025-12-14 11:00:00+00'),
    (27, 1,  5.800,  'kg',    '2025-12-13 11:00:00+00'),
    (28, 9,  8.500,  'kg',    '2025-12-16 11:00:00+00'),
    (29, 9,  11.000, 'kg',    '2025-12-17 11:00:00+00'),
    (30, 9,  10.200, 'kg',    '2025-12-16 11:00:00+00'),
    (31, 9,  7.500,  'kg',    '2025-12-17 11:00:00+00'),
    (32, 9,  12.000, 'kg',    '2025-12-18 11:00:00+00'),
    (33, 9,  8.000,  'kg',    '2025-12-19 11:00:00+00'),
    (34, 9,  6.200,  'kg',    '2025-12-17 11:00:00+00'),
    (35, 17, 14.000, 'kg',    '2025-12-09 11:00:00+00'),
    (36, 17, 11.500, 'kg',    '2025-12-07 11:00:00+00'),
    (37, 17, 12.800, 'kg',    '2025-12-06 11:00:00+00'),
    (38, 17, 13.500, 'kg',    '2025-12-10 11:00:00+00'),
    (39, 17, 9.800,  'kg',    '2025-12-08 11:00:00+00');

-- order: legacy order_discount preserved; V38 buckets (tier holds former flat discount); tax by delivery province
INSERT INTO "order" (
    order_id, uuid, order_number, customer_id, bakery_id, address_id,
    guest_name, guest_email, guest_phone,
    order_placed_datetime, order_scheduled_datetime, order_delivered_datetime,
    order_method, order_comment, order_total, order_discount,
    order_special_discount_amount, order_tier_discount_amount, order_employee_discount_amount,
    order_tax_rate, order_tax_amount, order_status
) VALUES
    ('40000000-0000-4000-8000-000000000001'::uuid, '40000000-0000-4000-8000-000000000001'::uuid, 'ORD-9F57A210', '20000000-0000-4000-8000-000000000001'::uuid, 3, 21, NULL, NULL, NULL, '2025-12-08 12:00:00+00', '2025-12-09 12:00:00+00', '2025-12-09 12:00:00+00', 'delivery'::order_method, 'Ring buzzer upon arrival',          26.95, 0.0, 0, 0, 0, 13.000, 3.50, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000002'::uuid, '40000000-0000-4000-8000-000000000002'::uuid, 'ORD-2E651949', '20000000-0000-4000-8000-000000000002'::uuid, 3, NULL, NULL, NULL, NULL, '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', 'pickup'::order_method,   NULL,                                12.98, 0.0, 0, 0, 0, 0,      0.00, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000003'::uuid, '40000000-0000-4000-8000-000000000003'::uuid, 'ORD-255EF439', '20000000-0000-4000-8000-000000000003'::uuid, 2, 23, NULL, NULL, NULL, '2025-12-11 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'delivery'::order_method, 'Leave with concierge',              34.2,  2.0, 0, 2.0, 0, 14.975, 5.12, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000004'::uuid, '40000000-0000-4000-8000-000000000004'::uuid, 'ORD-5B585631', '20000000-0000-4000-8000-000000000004'::uuid, 2, NULL, NULL, NULL, NULL, '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'pickup'::order_method,   NULL,                                9.75,  0.0, 0, 0, 0, 0,      0.00, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000005'::uuid, '40000000-0000-4000-8000-000000000005'::uuid, 'ORD-8991D636', '20000000-0000-4000-8000-000000000005'::uuid, 1, 26, NULL, NULL, NULL, '2025-12-13 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'delivery'::order_method, 'Call on arrival',                   41.9,  4.0, 0, 4.0, 0, 13.000, 5.45, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000006'::uuid, '40000000-0000-4000-8000-000000000006'::uuid, 'ORD-D8DD9FF9', '20000000-0000-4000-8000-000000000006'::uuid, 1, NULL, NULL, NULL, NULL, '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'pickup'::order_method,   NULL,                                20.2,  0.0, 0, 0, 0, 0,      0.00, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000007'::uuid, 'ORD-6B556FEA', '20000000-0000-4000-8000-000000000007'::uuid, 1, 31, NULL, NULL, NULL, '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', NULL,                        'delivery'::order_method, 'Please ensure items are sealed',    22.45, 0.0, 0, 0, 0, 5.000,  1.12, 'scheduled'::order_status),
    ('40000000-0000-4000-8000-000000000008'::uuid, '40000000-0000-4000-8000-000000000008'::uuid, 'ORD-E8F28437', '20000000-0000-4000-8000-000000000008'::uuid, 1, NULL, NULL, NULL, NULL, '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', 'pickup'::order_method,   NULL,                                7.25,  0.0, 0, 0, 0, 0,      0.00, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000009'::uuid, '40000000-0000-4000-8000-000000000009'::uuid, 'ORD-9F533009', '20000000-0000-4000-8000-000000000009'::uuid, 3, 34, NULL, NULL, NULL, '2025-12-16 12:00:00+00', '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', 'delivery'::order_method, NULL,                                58.48, 5.0, 0, 5.0, 0, 5.000,  2.92, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000010'::uuid, '40000000-0000-4000-8000-000000000010'::uuid, 'ORD-4D503C80', '20000000-0000-4000-8000-000000000010'::uuid, 3, NULL, NULL, NULL, NULL, '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', NULL,                        'pickup'::order_method,   NULL,                                6.49,  0.0, 0, 0, 0, 0,      0.00, 'placed'::order_status),
    ('40000000-0000-4000-8000-000000000011'::uuid, '40000000-0000-4000-8000-000000000011'::uuid, 'ORD-0E86775E', '20000000-0000-4000-8000-000000000011'::uuid, 2, 36, NULL, NULL, NULL, '2025-12-17 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'delivery'::order_method, 'Front desk drop-off',               27.7,  0.0, 0, 0, 0, 5.000,  1.39, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000012'::uuid, '40000000-0000-4000-8000-000000000012'::uuid, 'ORD-E2F567B5', '20000000-0000-4000-8000-000000000012'::uuid, 2, NULL, NULL, NULL, NULL, '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'pickup'::order_method,   NULL,                                16.5,  0.0, 0, 0, 0, 0,      0.00, 'completed'::order_status),
    ('40000000-0000-4000-8000-000000000013'::uuid, '40000000-0000-4000-8000-000000000013'::uuid, 'ORD-6025A59A', '20000000-0000-4000-8000-000000000013'::uuid, 1, 38, NULL, NULL, NULL, '2025-12-18 12:00:00+00', '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', 'delivery'::order_method, NULL,                                19.95, 0.0, 0, 0, 0, 5.000,  1.00, 'cancelled'::order_status),
    ('40000000-0000-4000-8000-000000000014'::uuid, '40000000-0000-4000-8000-000000000014'::uuid, 'ORD-FEF8D3EA', '20000000-0000-4000-8000-000000000014'::uuid, 1, NULL, NULL, NULL, NULL, '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', NULL,                        'pickup'::order_method,   NULL,                                29.99, 0.0, 0, 0, 0, 0,      0.00, 'cancelled'::order_status),
    ('40000000-0000-4000-8000-000000000015'::uuid, '40000000-0000-4000-8000-000000000015'::uuid, 'ORD-91128D33', '20000000-0000-4000-8000-000000000008'::uuid, 1, 41, NULL, NULL, NULL, '2026-03-25 15:31:54+00', '2026-04-03 18:00:00+00', NULL,                        'pickup'::order_method,   NULL,                                17.45, 0.0, 0, 0, 0, 5.000,  0.87, 'ready'::order_status);

INSERT INTO order_item (order_item_id, order_id, product_id, batch_id, order_item_quantity, order_item_unit_price_at_time, order_item_line_total)
OVERRIDING SYSTEM VALUE VALUES
    (1,  '40000000-0000-4000-8000-000000000001'::uuid, 14, 39, 1, 7.25,  7.25),
    (2,  '40000000-0000-4000-8000-000000000001'::uuid, 8,  37, 2, 2.25,  4.50),
    (3,  '40000000-0000-4000-8000-000000000001'::uuid, 5,  36, 2, 7.60,  15.20),
    (4,  '40000000-0000-4000-8000-000000000002'::uuid, 1,  35, 2, 6.49,  12.98),
    (5,  '40000000-0000-4000-8000-000000000003'::uuid, 13, 32, 1, 28.20, 28.20),
    (6,  '40000000-0000-4000-8000-000000000003'::uuid, 8,  30, 1, 2.25,  2.25),
    (7,  '40000000-0000-4000-8000-000000000003'::uuid, 21, 33, 1, 3.75,  3.75),
    (8,  '40000000-0000-4000-8000-000000000004'::uuid, 18, 11, 1, 3.75,  3.75),
    (9,  '40000000-0000-4000-8000-000000000004'::uuid, 6,  8,  2, 3.00,  6.00),
    (10, '40000000-0000-4000-8000-000000000005'::uuid, 12, 21, 2, 6.95,  13.90),
    (11, '40000000-0000-4000-8000-000000000005'::uuid, 26, 27, 1, 5.25,  5.25),
    (12, '40000000-0000-4000-8000-000000000005'::uuid, 5,  3,  2, 3.95,  7.90),
    (13, '40000000-0000-4000-8000-000000000005'::uuid, 8,  4,  1, 2.25,  2.25),
    (14, '40000000-0000-4000-8000-000000000005'::uuid, 17, 25, 1, 12.60, 12.60),
    (15, '40000000-0000-4000-8000-000000000006'::uuid, 15, 23, 1, 4.10,  4.10),
    (16, '40000000-0000-4000-8000-000000000006'::uuid, 16, 24, 1, 4.75,  4.75),
    (17, '40000000-0000-4000-8000-000000000006'::uuid, 18, 26, 1, 3.75,  3.75),
    (18, '40000000-0000-4000-8000-000000000006'::uuid, 8,  4,  2, 2.25,  4.50),
    (19, '40000000-0000-4000-8000-000000000006'::uuid, 6,  19, 1, 3.10,  3.10),
    (20, '40000000-0000-4000-8000-000000000007'::uuid, 10, 20, 2, 3.50,  7.00),
    (21, '40000000-0000-4000-8000-000000000007'::uuid, 5,  3,  1, 3.95,  3.95),
    (22, '40000000-0000-4000-8000-000000000007'::uuid, 1,  1,  1, 7.00,  7.00),
    (23, '40000000-0000-4000-8000-000000000007'::uuid, 8,  4,  2, 2.25,  4.50),
    (24, '40000000-0000-4000-8000-000000000008'::uuid, 14, 22, 1, 7.25,  7.25),
    (25, '40000000-0000-4000-8000-000000000009'::uuid, 13, 38, 1, 28.78, 28.78),
    (26, '40000000-0000-4000-8000-000000000009'::uuid, 12, 14, 1, 6.95,  6.95),
    (27, '40000000-0000-4000-8000-000000000009'::uuid, 17, 17, 2, 6.50,  13.00),
    (28, '40000000-0000-4000-8000-000000000009'::uuid, 26, 18, 1, 5.25,  5.25),
    (29, '40000000-0000-4000-8000-000000000009'::uuid, 8,  37, 2, 2.25,  4.50),
    (30, '40000000-0000-4000-8000-000000000010'::uuid, 1,  35, 1, 6.49,  6.49),
    (31, '40000000-0000-4000-8000-000000000011'::uuid, 4,  28, 1, 4.50,  4.50),
    (32, '40000000-0000-4000-8000-000000000011'::uuid, 5,  29, 1, 3.95,  3.95),
    (33, '40000000-0000-4000-8000-000000000011'::uuid, 18, 11, 2, 3.75,  7.50),
    (34, '40000000-0000-4000-8000-000000000011'::uuid, 14, 10, 1, 7.25,  7.25),
    (35, '40000000-0000-4000-8000-000000000011'::uuid, 8,  30, 2, 2.25,  4.50),
    (36, '40000000-0000-4000-8000-000000000012'::uuid, 6,  8,  2, 3.25,  6.50),
    (37, '40000000-0000-4000-8000-000000000012'::uuid, 8,  30, 2, 2.25,  4.50),
    (38, '40000000-0000-4000-8000-000000000012'::uuid, 9,  31, 1, 2.25,  2.25),
    (39, '40000000-0000-4000-8000-000000000012'::uuid, 24, 34, 1, 3.25,  3.25),
    (40, '40000000-0000-4000-8000-000000000013'::uuid, 21, 6,  2, 3.75,  7.50),
    (41, '40000000-0000-4000-8000-000000000013'::uuid, 5,  3,  1, 3.95,  3.95),
    (42, '40000000-0000-4000-8000-000000000013'::uuid, 16, 24, 1, 4.75,  4.75),
    (43, '40000000-0000-4000-8000-000000000013'::uuid, 8,  4,  1, 2.25,  2.25),
    (44, '40000000-0000-4000-8000-000000000013'::uuid, 6,  19, 1, 1.50,  1.50),
    (45, '40000000-0000-4000-8000-000000000014'::uuid, 13, 5,  1, 29.99, 29.99),
    (46, '40000000-0000-4000-8000-000000000015'::uuid, 3,  2,  5, 3.49,  17.45);
SELECT setval(pg_get_serial_sequence('order_item', 'order_item_id'), 46);

INSERT INTO payment (payment_id, uuid, order_id, payment_amount, payment_method, payment_transaction_id, payment_status, payment_paid_at, stripe_session_id) VALUES
    ('50000000-0000-4000-8000-000000000001'::uuid, '50000000-0000-4000-8000-000000000001'::uuid, '40000000-0000-4000-8000-000000000001'::uuid, 26.95, 'credit_card'::payment_method, 'TRX-98314501', 'completed'::payment_status,  '2025-12-09 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000002'::uuid, '50000000-0000-4000-8000-000000000002'::uuid, '40000000-0000-4000-8000-000000000002'::uuid, 12.98, 'debit_card'::payment_method,  'TRX-98314502', 'completed'::payment_status,  '2025-12-10 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000003'::uuid, '50000000-0000-4000-8000-000000000003'::uuid, '40000000-0000-4000-8000-000000000003'::uuid, 32.20, 'credit_card'::payment_method, 'TRX-98314503', 'completed'::payment_status,  '2025-12-12 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000004'::uuid, '50000000-0000-4000-8000-000000000004'::uuid, '40000000-0000-4000-8000-000000000004'::uuid, 9.75,  'credit_card'::payment_method, 'TRX-98314504', 'completed'::payment_status,  '2025-12-12 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000005'::uuid, '50000000-0000-4000-8000-000000000005'::uuid, '40000000-0000-4000-8000-000000000005'::uuid, 37.90, 'credit_card'::payment_method, 'TRX-98314505', 'completed'::payment_status,  '2025-12-14 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000006'::uuid, '50000000-0000-4000-8000-000000000006'::uuid, '40000000-0000-4000-8000-000000000006'::uuid, 20.20, 'debit_card'::payment_method,  'TRX-98314506', 'completed'::payment_status,  '2025-12-14 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000007'::uuid, '50000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000007'::uuid, 22.45, 'credit_card'::payment_method, 'TRX-98314507', 'authorized'::payment_status,  NULL, NULL),
    ('50000000-0000-4000-8000-000000000008'::uuid, '50000000-0000-4000-8000-000000000008'::uuid, '40000000-0000-4000-8000-000000000008'::uuid, 7.25,  'credit_card'::payment_method, 'TRX-98314508', 'completed'::payment_status,  '2025-12-15 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000009'::uuid, '50000000-0000-4000-8000-000000000009'::uuid, '40000000-0000-4000-8000-000000000009'::uuid, 53.48, 'credit_card'::payment_method, 'TRX-98314509', 'completed'::payment_status,  '2025-12-17 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000010'::uuid, '50000000-0000-4000-8000-000000000010'::uuid, '40000000-0000-4000-8000-000000000010'::uuid, 6.49,  'debit_card'::payment_method,  'TRX-98314510', 'pending'::payment_status,    NULL, NULL),
    ('50000000-0000-4000-8000-000000000011'::uuid, '50000000-0000-4000-8000-000000000011'::uuid, '40000000-0000-4000-8000-000000000011'::uuid, 27.70, 'credit_card'::payment_method, 'TRX-98314511', 'completed'::payment_status,  '2025-12-18 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000012'::uuid, '50000000-0000-4000-8000-000000000012'::uuid, '40000000-0000-4000-8000-000000000012'::uuid, 16.50, 'credit_card'::payment_method, 'TRX-98314512', 'completed'::payment_status,  '2025-12-18 12:00:00+00', NULL),
    ('50000000-0000-4000-8000-000000000013'::uuid, '50000000-0000-4000-8000-000000000013'::uuid, '40000000-0000-4000-8000-000000000013'::uuid, 19.95, 'debit_card'::payment_method,  'TRX-98314513', 'refunded'::payment_status,   NULL, NULL),
    ('50000000-0000-4000-8000-000000000014'::uuid, '50000000-0000-4000-8000-000000000014'::uuid, '40000000-0000-4000-8000-000000000014'::uuid, 29.99, 'credit_card'::payment_method, 'TRX-98314514', 'pending'::payment_status,    NULL, NULL),
    ('50000000-0000-4000-8000-000000000015'::uuid, '50000000-0000-4000-8000-000000000015'::uuid, '40000000-0000-4000-8000-000000000015'::uuid, 17.45, 'credit_card'::payment_method, 'TRX-98314515', 'authorized'::payment_status, NULL, NULL);

INSERT INTO reward (reward_id, uuid, customer_id, order_id, reward_points_earned, reward_transaction_date) VALUES
    ('60000000-0000-4000-8000-000000000001'::uuid, '60000000-0000-4000-8000-000000000001'::uuid, '20000000-0000-4000-8000-000000000001'::uuid, '40000000-0000-4000-8000-000000000001'::uuid, 26950, '2025-12-09 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000002'::uuid, '60000000-0000-4000-8000-000000000002'::uuid, '20000000-0000-4000-8000-000000000002'::uuid, '40000000-0000-4000-8000-000000000002'::uuid, 12980, '2025-12-10 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000003'::uuid, '60000000-0000-4000-8000-000000000003'::uuid, '20000000-0000-4000-8000-000000000003'::uuid, '40000000-0000-4000-8000-000000000003'::uuid, 32200, '2025-12-12 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000004'::uuid, '60000000-0000-4000-8000-000000000004'::uuid, '20000000-0000-4000-8000-000000000004'::uuid, '40000000-0000-4000-8000-000000000004'::uuid, 9750,  '2025-12-12 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000005'::uuid, '60000000-0000-4000-8000-000000000005'::uuid, '20000000-0000-4000-8000-000000000005'::uuid, '40000000-0000-4000-8000-000000000005'::uuid, 37900, '2025-12-14 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000006'::uuid, '60000000-0000-4000-8000-000000000006'::uuid, '20000000-0000-4000-8000-000000000006'::uuid, '40000000-0000-4000-8000-000000000006'::uuid, 20200, '2025-12-14 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000007'::uuid, '60000000-0000-4000-8000-000000000007'::uuid, '20000000-0000-4000-8000-000000000008'::uuid, '40000000-0000-4000-8000-000000000008'::uuid, 7250,  '2025-12-15 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000008'::uuid, '60000000-0000-4000-8000-000000000008'::uuid, '20000000-0000-4000-8000-000000000009'::uuid, '40000000-0000-4000-8000-000000000009'::uuid, 53480, '2025-12-17 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000009'::uuid, '60000000-0000-4000-8000-000000000009'::uuid, '20000000-0000-4000-8000-000000000011'::uuid, '40000000-0000-4000-8000-000000000011'::uuid, 27700, '2025-12-18 12:00:00+00'),
    ('60000000-0000-4000-8000-000000000010'::uuid, '60000000-0000-4000-8000-000000000010'::uuid, '20000000-0000-4000-8000-000000000012'::uuid, '40000000-0000-4000-8000-000000000012'::uuid, 16500, '2025-12-18 12:00:00+00');

INSERT INTO employee_customer_link (employee_id, customer_id) VALUES
    ('30000000-0000-4000-8000-000000000001'::uuid, '20000000-0000-4000-8000-000000000001'::uuid),
    ('30000000-0000-4000-8000-000000000003'::uuid, '20000000-0000-4000-8000-000000000003'::uuid);

-- V27-shaped reviews + moderation_rejection_reason (NULL unless rejected)
INSERT INTO review (
    review_id, uuid, customer_id, product_id, employee_id, order_id, bakery_id,
    review_rating, review_comment, review_submitted_date, review_status, review_approval_date, moderation_rejection_reason
) VALUES
    ('71000000-0000-4000-8000-000000000001'::uuid, '71000000-0000-4000-8000-000000000001'::uuid,
     '20000000-0000-4000-8000-000000000001'::uuid, 2, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'House favourite for sandwiches — stays soft for days.',
     '2026-04-01 10:00:00+00', 'approved'::review_status, '2026-04-02 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000002'::uuid, '71000000-0000-4000-8000-000000000002'::uuid,
     '20000000-0000-4000-8000-000000000002'::uuid, 2, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     4, 'Nutty and hearty; great toasted.',
     '2026-04-01 11:00:00+00', 'approved'::review_status, '2026-04-02 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000003'::uuid, '71000000-0000-4000-8000-000000000003'::uuid,
     '20000000-0000-4000-8000-000000000003'::uuid, 2, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'Best multigrain in the city, in my opinion.',
     '2026-04-02 14:00:00+00', 'approved'::review_status, '2026-04-03 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000004'::uuid, '71000000-0000-4000-8000-000000000004'::uuid,
     '20000000-0000-4000-8000-000000000004'::uuid, 6, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'Loaded with blueberries; moist and not too sweet.',
     '2026-04-02 15:00:00+00', 'approved'::review_status, '2026-04-03 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000005'::uuid, '71000000-0000-4000-8000-000000000005'::uuid,
     '20000000-0000-4000-8000-000000000005'::uuid, 6, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     4, 'Nice dome and even distribution of fruit.',
     '2026-04-03 09:00:00+00', 'approved'::review_status, '2026-04-04 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000006'::uuid, '71000000-0000-4000-8000-000000000006'::uuid,
     '20000000-0000-4000-8000-000000000006'::uuid, 4, '30000000-0000-4000-8000-000000000008'::uuid, NULL, 3,
     5, 'Sticky, glossy glaze — worth the trip.',
     '2026-04-03 12:00:00+00', 'approved'::review_status, '2026-04-04 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000007'::uuid, '71000000-0000-4000-8000-000000000007'::uuid,
     '20000000-0000-4000-8000-000000000007'::uuid, 4, '30000000-0000-4000-8000-000000000008'::uuid, NULL, 3,
     4, 'A bit sweet for breakfast but perfect with coffee.',
     '2026-04-04 12:00:00+00', 'approved'::review_status, '2026-04-05 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000008'::uuid, '71000000-0000-4000-8000-000000000008'::uuid,
     '20000000-0000-4000-8000-000000000008'::uuid, 9, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'Chewy centers, crisp edges — classic done right.',
     '2026-04-04 09:00:00+00', 'approved'::review_status, '2026-04-05 10:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000009'::uuid, '71000000-0000-4000-8000-000000000009'::uuid,
     '20000000-0000-4000-8000-000000000010'::uuid, 11, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'Frosting is silky, cake is moist.',
     '2026-04-05 10:00:00+00', 'approved'::review_status, '2026-04-06 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000010'::uuid, '71000000-0000-4000-8000-000000000010'::uuid,
     '20000000-0000-4000-8000-000000000011'::uuid, 11, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     4, 'Rich chocolate, not too bitter.',
     '2026-04-05 11:00:00+00', 'approved'::review_status, '2026-04-06 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000011'::uuid, '71000000-0000-4000-8000-000000000011'::uuid,
     '20000000-0000-4000-8000-000000000012'::uuid, 18, '30000000-0000-4000-8000-000000000007'::uuid, NULL, 2,
     5, 'Perfect brownie, very fudgy.',
     '2026-04-06 12:00:00+00', 'approved'::review_status, '2026-04-07 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000012'::uuid, '71000000-0000-4000-8000-000000000012'::uuid,
     '20000000-0000-4000-8000-000000000001'::uuid, 26, '30000000-0000-4000-8000-000000000008'::uuid, NULL, 3,
     5, 'Choux held up, chocolate ganache was smooth.',
     '2026-04-06 13:00:00+00', 'approved'::review_status, '2026-04-07 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000013'::uuid, '71000000-0000-4000-8000-000000000013'::uuid,
     '20000000-0000-4000-8000-000000000013'::uuid, 15, '30000000-0000-4000-8000-000000000008'::uuid, NULL, 3,
     4, 'Flaky layers and tart apples — lovely afternoon treat.',
     '2026-04-07 11:00:00+00', 'approved'::review_status, '2026-04-08 09:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000101'::uuid, '71000000-0000-4000-8000-000000000101'::uuid,
     '20000000-0000-4000-8000-000000000003'::uuid, 13, '30000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000003'::uuid, 2,
     5, 'Delivery arrived on time; driver handled the bags carefully.',
     '2026-04-01 16:00:00+00', 'approved'::review_status, '2026-04-02 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000102'::uuid, '71000000-0000-4000-8000-000000000102'::uuid,
     '20000000-0000-4000-8000-000000000004'::uuid, 18, '30000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000004'::uuid, 2,
     4, 'Pickup was quick and staff confirmed everything before I left.',
     '2026-04-02 10:00:00+00', 'approved'::review_status, '2026-04-03 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000103'::uuid, '71000000-0000-4000-8000-000000000103'::uuid,
     '20000000-0000-4000-8000-000000000011'::uuid, 4, '30000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000011'::uuid, 2,
     5, 'Friendly counter service; order matched the app exactly.',
     '2026-04-03 14:00:00+00', 'approved'::review_status, '2026-04-04 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000104'::uuid, '71000000-0000-4000-8000-000000000104'::uuid,
     '20000000-0000-4000-8000-000000000012'::uuid, 6, '30000000-0000-4000-8000-000000000007'::uuid, '40000000-0000-4000-8000-000000000012'::uuid, 2,
     4, 'Packaging kept pastries intact for the ride home.',
     '2026-04-04 09:00:00+00', 'approved'::review_status, '2026-04-05 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000105'::uuid, '71000000-0000-4000-8000-000000000105'::uuid,
     '20000000-0000-4000-8000-000000000005'::uuid, 12, '30000000-0000-4000-8000-000000000001'::uuid, '40000000-0000-4000-8000-000000000005'::uuid, 1,
     5, 'Delivery updates were clear and the box was well sealed.',
     '2026-04-05 08:00:00+00', 'approved'::review_status, '2026-04-06 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000106'::uuid, '71000000-0000-4000-8000-000000000106'::uuid,
     '20000000-0000-4000-8000-000000000001'::uuid, 14, '30000000-0000-4000-8000-000000000008'::uuid, '40000000-0000-4000-8000-000000000001'::uuid, 3,
     5, 'Courier communicated well; chilled items were still cold.',
     '2026-04-05 17:00:00+00', 'approved'::review_status, '2026-04-06 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000107'::uuid, '71000000-0000-4000-8000-000000000107'::uuid,
     '20000000-0000-4000-8000-000000000002'::uuid, 1, '30000000-0000-4000-8000-000000000008'::uuid, '40000000-0000-4000-8000-000000000002'::uuid, 3,
     4, 'Smooth pickup experience at the Front Street Proof counter.',
     '2026-04-06 11:00:00+00', 'approved'::review_status, '2026-04-07 11:00:00+00', NULL),
    ('71000000-0000-4000-8000-000000000108'::uuid, '71000000-0000-4000-8000-000000000108'::uuid,
     '20000000-0000-4000-8000-000000000006'::uuid, 15, '30000000-0000-4000-8000-000000000001'::uuid, '40000000-0000-4000-8000-000000000006'::uuid, 1,
     5, 'Order was ready right on schedule for pickup.',
     '2026-04-07 09:00:00+00', 'approved'::review_status, '2026-04-08 11:00:00+00', NULL);

INSERT INTO customer_preference (customer_id, tag_id, preference_type, preference_strength) VALUES
    ('20000000-0000-4000-8000-000000000001'::uuid, 1,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000001'::uuid, 10, 'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000002'::uuid, 5,  'dislike'::preference_type,  4::smallint),
    ('20000000-0000-4000-8000-000000000002'::uuid, 9,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000003'::uuid, 2,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000003'::uuid, 10, 'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000004'::uuid, 4,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000005'::uuid, 3,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000005'::uuid, 7,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000006'::uuid, 6,  'dislike'::preference_type,  4::smallint),
    ('20000000-0000-4000-8000-000000000006'::uuid, 8,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000007'::uuid, 5,  'allergic'::preference_type, 5::smallint),
    ('20000000-0000-4000-8000-000000000007'::uuid, 9,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000008'::uuid, 2,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000008'::uuid, 10, 'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000009'::uuid, 7,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000009'::uuid, 9,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000010'::uuid, 4,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000011'::uuid, 3,  'dislike'::preference_type,  2::smallint),
    ('20000000-0000-4000-8000-000000000011'::uuid, 9,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000012'::uuid, 6,  'dislike'::preference_type,  4::smallint),
    ('20000000-0000-4000-8000-000000000012'::uuid, 10, 'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000013'::uuid, 5,  'allergic'::preference_type, 5::smallint),
    ('20000000-0000-4000-8000-000000000013'::uuid, 7,  'like'::preference_type,     3::smallint),
    ('20000000-0000-4000-8000-000000000014'::uuid, 2,  'like'::preference_type,     4::smallint),
    ('20000000-0000-4000-8000-000000000014'::uuid, 10, 'like'::preference_type,     3::smallint);

COMMIT;
