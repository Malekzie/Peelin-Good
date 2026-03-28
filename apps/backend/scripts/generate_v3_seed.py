"""Generate V3__seed_data.sql — run from repo root: python scripts/generate_v3_seed.py"""
from pathlib import Path

def make_uuid(prefix: str, n: int) -> str:
    return f"{prefix}-0000-4000-8000-{n:012d}"

H_ADMIN = "$2b$10$R92cP1dmtXyTDQYq6U53V.fRED4Kb9JHcAscibP8XAc7W1Zzm2hXm"
H_EMP = "$2b$10$wxtEMcmy1odu09UvDcA1jOIZrh8Pyzf.sfmwpMuiApWlVYbrpETWy"
H_CUST = "$2b$10$QBNjm.PVIpb/6dZjjcbEHuTJdSjcQ9SLirABycucuHbHTw3DFvINe"

def uuser(n): return make_uuid("10000000", n)
def ucustomer(n): return make_uuid("20000000", n)
def uemployee(n): return make_uuid("30000000", n)
def uorder(n): return make_uuid("40000000", n)
def upayment(n): return make_uuid("50000000", n)
def ureward(n): return make_uuid("60000000", n)
def ureview(n): return make_uuid("70000000", n)

# Map unified day 1..7 (Mon..Sun) to V1 0..6 (Sun..Sat)
def dow(u):
    return 0 if u == 7 else u

lines = []
lines.append("-- V3: Seed data adapted from unified_postgres.sql for Flyway V1/V2 schema.")
lines.append("-- Surrogate PKs (user_id, employee_id, customer_id, order_id, …) are INTEGER (matches JPA).")
lines.append("-- Stable public identifiers remain in uuid columns where present.")
lines.append("-- Passwords: Admin123! (admin), Emp123! (employees), Cust123! (customers).")
lines.append("-- Run on an empty DB after V2 (or reset). Flyway wraps this in a transaction.")
lines.append("")

# address through product_tag — copy from unified (integer PKs) lines 433-646
lines.append("-- address")
lines.append("""INSERT INTO address (address_id, address_line1, address_line2, address_city, address_province, address_postal_code)
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
    (41, '5800 2 St SW',               'Unit 8',     'Calgary',    'AB', 'T2H 0H2');""")
lines.append("SELECT setval(pg_get_serial_sequence('address', 'address_id'), 41);")
lines.append("")

# reward_tier
lines.append("INSERT INTO reward_tier (reward_tier_id, reward_tier_name, reward_tier_min_points, reward_tier_max_points, reward_tier_discount_rate)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("    (1, 'Bronze',   0,      99999,  0.00),")
lines.append("    (2, 'Silver',   100000, 249999, 5.00),")
lines.append("    (3, 'Gold',     250000, 499999, 10.00),")
lines.append("    (4, 'Platinum', 500000, NULL,   15.00);")
lines.append("SELECT setval(pg_get_serial_sequence('reward_tier', 'reward_tier_id'), 4);")
lines.append("")

# users
users = [
    (1,  'alicia.nguyen',    'alicia.nguyen@northharbourmail.ca',    'admin',    True, '2025-08-22 12:00:00+00'),
    (2,  'mason.clark',      'mason.clark@northharbourmail.ca',      'employee', True, '2025-09-16 12:00:00+00'),
    (3,  'sophia.patel',     'sophia.patel@northharbourmail.ca',     'employee', True, '2025-09-21 12:00:00+00'),
    (4,  'ethan.wright',     'ethan.wright@northharbourmail.ca',     'employee', True, '2025-09-29 12:00:00+00'),
    (5,  'isabella.chen',    'isabella.chen@northharbourmail.ca',    'employee', True, '2025-10-01 12:00:00+00'),
    (6,  'noah.martin',      'noah.martin@northharbourmail.ca',      'employee', True, '2025-10-05 12:00:00+00'),
    (7,  'ava.roberts',      'ava.roberts@northharbourmail.ca',      'employee', True, '2025-10-11 12:00:00+00'),
    (8,  'logan.scott',      'logan.scott@northharbourmail.ca',      'employee', True, '2025-10-15 12:00:00+00'),
    (9,  'mia.kim',          'mia.kim@northharbourmail.ca',          'employee', True, '2025-10-19 12:00:00+00'),
    (10, 'jackson.hall',     'jackson.hall@northharbourmail.ca',     'employee', True, '2025-10-21 12:00:00+00'),
    (11, 'olivia.brown',     'olivia.brown@northharbourmail.ca',     'customer', True, '2025-10-23 12:00:00+00'),
    (12, 'liam.thompson',    'liam.thompson@northharbourmail.ca',    'customer', True, '2025-10-26 12:00:00+00'),
    (13, 'emma.wilson',      'emma.wilson@northharbourmail.ca',      'customer', True, '2025-10-31 12:00:00+00'),
    (14, 'benjamin.lee',     'benjamin.lee@northharbourmail.ca',     'customer', True, '2025-11-05 12:00:00+00'),
    (15, 'amelia.johnson',   'amelia.johnson@northharbourmail.ca',   'customer', True, '2025-11-08 12:00:00+00'),
    (16, 'lucas.anderson',   'lucas.anderson@northharbourmail.ca',   'customer', True, '2025-11-10 12:00:00+00'),
    (17, 'charlotte.miller', 'charlotte.miller@northharbourmail.ca', 'customer', True, '2025-11-12 12:00:00+00'),
    (18, 'henry.davis',      'henry.davis@northharbourmail.ca',      'customer', True, '2025-11-15 12:00:00+00'),
    (19, 'evelyn.moore',     'evelyn.moore@northharbourmail.ca',     'customer', True, '2025-11-20 12:00:00+00'),
    (20, 'daniel.taylor',    'daniel.taylor@northharbourmail.ca',    'customer', True, '2025-11-22 12:00:00+00'),
    (21, 'harper.jackson',   'harper.jackson@northharbourmail.ca',   'customer', True, '2025-11-26 12:00:00+00'),
    (22, 'sebastian.white',  'sebastian.white@northharbourmail.ca',  'customer', True, '2025-11-28 12:00:00+00'),
    (23, 'nora.harris',      'nora.harris@northharbourmail.ca',      'customer', True, '2025-11-30 12:00:00+00'),
    (24, 'wyatt.martinez',   'wyatt.martinez@northharbourmail.ca',   'customer', True, '2025-12-02 12:00:00+00'),
]
lines.append("INSERT INTO \"user\" (user_id, uuid, username, user_email, user_password_hash, user_role, is_active, user_created_at)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
for i, (uid, uname, email, role, active, ts) in enumerate(users):
    h = H_ADMIN if role == "admin" else (H_EMP if role == "employee" else H_CUST)
    comma = "," if i < len(users) - 1 else ";"
    lines.append(f"    ({uid}, '{uuser(uid)}'::uuid, '{uname}', '{email}', '{h}', '{role}'::user_role, {str(active).upper()}, '{ts}'){comma}")
lines.append("SELECT setval(pg_get_serial_sequence('\"user\"', 'user_id'), (SELECT MAX(user_id) FROM \"user\"));")
lines.append("")

# bakery
lines.append("INSERT INTO bakery (bakery_id, address_id, bakery_name, bakery_phone, bakery_email, status, latitude, longitude)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("    (1, 1,  'North Harbour Bakery - Downtown',         '(403) 555-2101', 'downtown@northharbourbakery.ca',  'open'::bakery_status, 51.044700, -114.071900),")
lines.append("    (2, 11, 'North Harbour Bakery - Edmonton Central',  '(780) 555-4302', 'edmonton@northharbourbakery.ca', 'open'::bakery_status, 53.546100, -113.493800),")
lines.append("    (3, 20, 'North Harbour Bakery - Toronto Financial', '(416) 555-9012', 'toronto@northharbourbakery.ca',  'open'::bakery_status, 43.653200, -79.383200);")
lines.append("SELECT setval(pg_get_serial_sequence('bakery', 'bakery_id'), 3);")
lines.append("")

# bakery_hours — map unified 1..7 to V1 0..6
bh = [
    (1,  1, 1, '07:30', '18:00', False),
    (2,  1, 2, '07:30', '18:00', False),
    (3,  1, 3, '07:30', '18:00', False),
    (4,  1, 4, '07:30', '18:00', False),
    (5,  1, 5, '07:30', '18:00', False),
    (6,  1, 6, '08:30', '16:30', False),
    (7,  1, 7, None, None, True),
    (8,  2, 1, '08:00', '17:30', False),
    (9,  2, 2, '08:00', '17:30', False),
    (10, 2, 3, '08:00', '17:30', False),
    (11, 2, 4, '08:00', '17:30', False),
    (12, 2, 5, '08:00', '17:30', False),
    (13, 2, 6, '09:00', '16:00', False),
    (14, 2, 7, None, None, True),
    (15, 3, 1, '07:00', '18:30', False),
    (16, 3, 2, '07:00', '18:30', False),
    (17, 3, 3, '07:00', '18:30', False),
    (18, 3, 4, '07:00', '18:30', False),
    (19, 3, 5, '07:00', '18:30', False),
    (20, 3, 6, '08:00', '17:00', False),
    (21, 3, 7, '09:00', '14:00', False),
]
lines.append("INSERT INTO bakery_hours (bakery_hours_id, bakery_id, day_of_week, open_time, close_time, is_closed)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
for i, (hid, bid, ud, ot, ct, closed) in enumerate(bh):
    d = dow(ud)
    ot_sql = "NULL" if ot is None else f"'{ot}'::time"
    ct_sql = "NULL" if ct is None else f"'{ct}'::time"
    comma = "," if i < len(bh) - 1 else ";"
    lines.append(f"    ({hid}, {bid}, {d}::smallint, {ot_sql}, {ct_sql}, {str(closed).upper()}){comma}")
lines.append("SELECT setval(pg_get_serial_sequence('bakery_hours', 'bakery_hours_id'), 21);")
lines.append("")

# tags + products + product_tag
lines.append("INSERT INTO tag (tag_id, tag_name) OVERRIDING SYSTEM VALUE VALUES")
lines.append("    (1,  'Bread'), (2,  'Cake'), (3,  'Pastry'), (4,  'Cookie'), (5,  'Gluten-Free'), (6,  'Dairy-Free'),")
lines.append("    (7,  'Seasonal'), (8,  'Vegan'), (9,  'Breakfast'), (10, 'Dessert'), (11, 'Nut-Free'), (12, 'Whole Grain');")
lines.append("SELECT setval(pg_get_serial_sequence('tag', 'tag_id'), 12);")
lines.append("")

# products — truncated list from unified (same 26 products)
lines.append("INSERT INTO product (product_id, product_name, product_description, product_base_price, product_image_url)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("""    (1,  'Sourdough Loaf',            'Naturally leavened sourdough bread',                    6.49,  NULL),
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
    (26, 'Chocolate Eclair',          'Choux pastry with cream and chocolate topping',         5.25,  NULL);""")
lines.append("SELECT setval(pg_get_serial_sequence('product', 'product_id'), 26);")
lines.append("")

lines.append("INSERT INTO product_tag (product_id, tag_id) VALUES")
lines.append("    (1, 1), (2, 1), (2, 12), (3, 1), (4, 3), (4, 9), (5, 3), (5, 9), (6, 3), (6, 9), (7, 9),")
lines.append("    (8, 4), (8, 10), (9, 4), (9, 10), (10, 2), (10, 10), (11, 2), (11, 10), (12, 2), (12, 10), (13, 2), (13, 10),")
lines.append("    (14, 2), (14, 10), (15, 3), (15, 10), (16, 3), (17, 10), (18, 10), (19, 6), (19, 8), (20, 5), (21, 7), (22, 10),")
lines.append("    (23, 4), (24, 12), (25, 3), (26, 3);")
lines.append("")

# supplier + inventory
lines.append("INSERT INTO supplier (supplier_id, address_id, supplier_name, supplier_phone, supplier_email)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("    (1, 29, 'Prairie Wholesale Ingredients',  '(403) 555-7001', 'orders@prairiewholesale.ca'),")
lines.append("    (2, 30, 'Summit Packaging Supply',        '(403) 555-7002', 'support@summitpackaging.ca'),")
lines.append("    (3, 33, 'Riverbend Dairy Co.',            '(780) 555-7003', 'sales@riverbenddairy.ca'),")
lines.append("    (4, 16, 'Coastal Produce Distributors',   '(604) 555-7004', 'info@coastalproduce.ca'),")
lines.append("    (5, 24, 'St. Lawrence Dry Goods',         '(514) 555-7005', 'service@stlawrencedrygoods.ca');")
lines.append("SELECT setval(pg_get_serial_sequence('supplier', 'supplier_id'), 5);")
lines.append("")

lines.append("INSERT INTO inventory (inventory_id, bakery_id, supplier_id, inventory_item_name, inventory_item_type, inventory_quantity_on_hand, inventory_unit_of_measure)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("""    (1,  1, 1, 'All-purpose flour',       'Ingredient', 450.000, 'kg'),
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
    (24, 3, 5, 'Baking powder',           'Ingredient', 65.000,  'kg');""")
lines.append("SELECT setval(pg_get_serial_sequence('inventory', 'inventory_id'), 24);")
lines.append("")

# employee — middle_initial CHAR(2): pad single char with space
lines.append("INSERT INTO employee (employee_id, uuid, user_id, address_id, bakery_id, employee_first_name, employee_middle_initial, employee_last_name, employee_position, employee_phone, employee_business_phone, employee_work_email, photo_approval_pending)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append(f"    (1, '{uemployee(1)}'::uuid, 2, 2,  1, 'Mason',   NULL, 'Clark',   'Baker',            '(403) 555-3101', '(403) 555-4101', 'mason.clark@northharbourbakery.ca', FALSE),")
lines.append(f"    (2, '{uemployee(2)}'::uuid, 3, 3,  1, 'Sophia',  'R ', 'Patel',   'Baker',            '(403) 555-3102', '(403) 555-4102', 'sophia.patel@northharbourbakery.ca', FALSE),")
lines.append(f"    (3, '{uemployee(3)}'::uuid, 4, 4,  1, 'Ethan',   NULL, 'Wright',  'Shift Lead',       '(403) 555-3103', '(403) 555-4103', 'ethan.wright@northharbourbakery.ca', FALSE),")
lines.append(f"    (4, '{uemployee(4)}'::uuid, 5, 5,  1, 'Isabella','M ', 'Chen',    'Baker',            '(403) 555-3104', '(403) 555-4104', 'isabella.chen@northharbourbakery.ca', FALSE),")
lines.append(f"    (5, '{uemployee(5)}'::uuid, 6, 6,  2, 'Noah',    NULL, 'Martin',  'Baker',            '(403) 555-3105', '(403) 555-4105', 'noah.martin@northharbourbakery.ca', FALSE),")
lines.append(f"    (6, '{uemployee(6)}'::uuid, 7, 7,  2, 'Ava',     NULL, 'Roberts', 'Customer Support', '(403) 555-3106', '(403) 555-4106', 'ava.roberts@northharbourbakery.ca', FALSE),")
lines.append(f"    (7, '{uemployee(7)}'::uuid, 8, 8,  2, 'Logan',   'J ', 'Scott',   'Quality Control',  '(403) 555-3107', '(403) 555-4107', 'logan.scott@northharbourbakery.ca', FALSE),")
lines.append(f"    (8, '{uemployee(8)}'::uuid, 9, 9,  3, 'Mia',     NULL, 'Kim',     'Baker',            '(403) 555-3108', '(403) 555-4108', 'mia.kim@northharbourbakery.ca', FALSE),")
lines.append(f"    (9, '{uemployee(9)}'::uuid, 10, 10, 3, 'Jackson', NULL, 'Hall',    'Baker',            '(403) 555-3109', '(403) 555-4109', 'jackson.hall@northharbourbakery.ca', FALSE);")
lines.append("SELECT setval(pg_get_serial_sequence('employee', 'employee_id'), (SELECT MAX(employee_id) FROM employee));")
lines.append("")

# customer — unified customer_id c maps to ucustomer(c), user_id u maps to uuser(u)
cust_rows = [
    (1,  11, 21, 1, 'Olivia',    None, 'Brown',    '(416) 555-1201', None, 'olivia.brown@northharbourmail.ca',     120000,  '2025-11-20'),
    (2,  12, 22, 1, 'Liam',      None, 'Thompson', '(416) 555-1202', None, 'liam.thompson@northharbourmail.ca',    240000,  '2025-11-22'),
    (3,  13, 23, 2, 'Emma',      'J',  'Wilson',   '(514) 555-1203', None, 'emma.wilson@northharbourmail.ca',      520000,  '2025-11-24'),
    (4,  14, 25, 1, 'Benjamin',  None, 'Lee',      '(613) 555-1204', None, 'benjamin.lee@northharbourmail.ca',     80000,   '2025-11-26'),
    (5,  15, 26, 2, 'Amelia',    None, 'Johnson',  '(613) 555-1205', None, 'amelia.johnson@northharbourmail.ca',   740000,  '2025-11-28'),
    (6,  16, 27, 1, 'Lucas',     'A',  'Anderson', '(613) 555-1206', None, 'lucas.anderson@northharbourmail.ca',   60000,   '2025-11-30'),
    (7,  17, 31, 1, 'Charlotte', None, 'Miller',   '(403) 555-1207', None, 'charlotte.miller@northharbourmail.ca', 210000,  '2025-12-02'),
    (8,  18, 32, 3, 'Henry',     None, 'Davis',    '(403) 555-1208', None, 'henry.davis@northharbourmail.ca',      1120000, '2025-12-04'),
    (9,  19, 34, 2, 'Evelyn',    None, 'Moore',    '(403) 555-1209', None, 'evelyn.moore@northharbourmail.ca',     680000,  '2025-12-06'),
    (10, 20, 35, 1, 'Daniel',    None, 'Taylor',   '(403) 555-1210', None, 'daniel.taylor@northharbourmail.ca',    140000,  '2025-12-08'),
    (11, 21, 36, 2, 'Harper',    None, 'Jackson',  '(403) 555-1211', None, 'harper.jackson@northharbourmail.ca',   810000,  '2025-12-10'),
    (12, 22, 37, 1, 'Sebastian', None, 'White',    '(403) 555-1212', None, 'sebastian.white@northharbourmail.ca',  95000,   '2025-12-11'),
    (13, 23, 38, 1, 'Nora',      None, 'Harris',   '(403) 555-1213', None, 'nora.harris@northharbourmail.ca',      260000,  '2025-12-12'),
    (14, 24, 39, 1, 'Wyatt',     None, 'Martinez', '(403) 555-1214', None, 'wyatt.martinez@northharbourmail.ca',   180000,  '2025-12-13'),
]
lines.append("INSERT INTO customer (customer_id, uuid, user_id, address_id, reward_tier_id, customer_first_name, customer_middle_initial, customer_last_name, customer_phone, customer_business_phone, customer_email, customer_reward_balance, customer_tier_assigned_date, photo_approval_pending)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
for i, (cid, uid, aid, tid, fn, mi, ln, ph, bph, em, bal, td) in enumerate(cust_rows):
    mi_sql = "NULL" if mi is None else f"'{mi}'"
    bph_sql = "NULL" if bph is None else f"'{bph}'"
    comma = "," if i < len(cust_rows) - 1 else ";"
    lines.append(f"    ({cid}, '{ucustomer(cid)}'::uuid, {uid}, {aid}, {tid}, '{fn}', {mi_sql}, '{ln}', '{ph}', {bph_sql}, '{em}', {bal}, '{td}', FALSE){comma}")
lines.append("SELECT setval(pg_get_serial_sequence('customer', 'customer_id'), (SELECT MAX(customer_id) FROM customer));")
lines.append("")

# batch — DATE casts
lines.append("INSERT INTO batch (batch_id, bakery_id, product_id, employee_id, batch_production_date, batch_expiry_date, batch_quantity_produced)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("""    (1,  1, 1,  1, '2025-12-14'::date, '2025-12-19'::date, 60),
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
    (18, 3, 26, 2, '2025-12-19'::date, '2025-12-22'::date, 50);""")
lines.append("SELECT setval(pg_get_serial_sequence('batch', 'batch_id'), 18);")
lines.append("")

# batch_inventory
lines.append("INSERT INTO batch_inventory (batch_id, inventory_id, quantity_used, unit_of_measure_at_time, usage_recorded_date) VALUES")
lines.append("""    (1,  1,  18.500, 'kg',    '2025-12-14 12:00:00+00'),
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
    (18, 18, 2.200,  'kg',    '2025-12-19 12:00:00+00');""")
lines.append("")

# orders — customer_id uses ucustomer(n), order uorder(n)
# unified order k -> uorder(k)
orders = [
    (1,  'ORD-0001', 1,  3, 21,   '2025-12-08 12:00:00+00', '2025-12-09 12:00:00+00', '2025-12-09 12:00:00+00', 'delivery', 'Ring buzzer upon arrival',       26.95,  0.00, 'completed'),
    (2,  'ORD-0002', 2,  3, None, '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', '2025-12-10 12:00:00+00', 'pickup',   None,                              12.98,  0.00, 'completed'),
    (3,  'ORD-0003', 3,  2, 23,   '2025-12-11 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'delivery', 'Leave with concierge',           34.20,  2.00, 'completed'),
    (4,  'ORD-0004', 4,  2, None, '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', '2025-12-12 12:00:00+00', 'pickup',   None,                              9.75,   0.00, 'completed'),
    (5,  'ORD-0005', 5,  1, 26,   '2025-12-13 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'delivery', 'Call on arrival',                41.90,  4.00, 'completed'),
    (6,  'ORD-0006', 6,  1, None, '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', '2025-12-14 12:00:00+00', 'pickup',   None,                              18.20,  0.00, 'completed'),
    (7,  'ORD-0007', 7,  1, 31,   '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', None,                     'delivery', 'Please ensure items are sealed', 22.45,  0.00, 'scheduled'),
    (8,  'ORD-0008', 8,  1, None, '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', '2025-12-15 12:00:00+00', 'pickup',   None,                              7.25,   0.00, 'completed'),
    (9,  'ORD-0009', 9,  3, 34,   '2025-12-16 12:00:00+00', '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', 'delivery', None,                              58.48,  5.00, 'completed'),
    (10, 'ORD-0010', 10, 3, None, '2025-12-17 12:00:00+00', '2025-12-17 12:00:00+00', None,                     'pickup',   None,                              6.49,   0.00, 'placed'),
    (11, 'ORD-0011', 11, 2, 36,   '2025-12-17 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'delivery', 'Front desk drop-off',            27.70,  0.00, 'completed'),
    (12, 'ORD-0012', 12, 2, None, '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', '2025-12-18 12:00:00+00', 'pickup',   None,                              14.50,  0.00, 'completed'),
    (13, 'ORD-0013', 13, 1, 38,   '2025-12-18 12:00:00+00', '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', 'delivery', None,                              19.95,  0.00, 'cancelled'),
    (14, 'ORD-0014', 14, 1, None, '2025-12-19 12:00:00+00', '2025-12-19 12:00:00+00', None,                     'pickup',   None,                              29.99,  0.00, 'cancelled'),
    (15, 'ORD-0015', 8,  1, 41,   '2026-03-25 15:31:54+00', '2026-04-03 18:00:00+00', None,                     'pickup',   None,                              17.45,  0.00, 'ready'),
]
lines.append("INSERT INTO \"order\" (order_id, uuid, order_number, customer_id, bakery_id, address_id, order_placed_datetime, order_scheduled_datetime, order_delivered_datetime, order_method, order_comment, order_total, order_discount, order_status)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
for i, row in enumerate(orders):
    oid, onum, cid, bid, aid, op, os, od, ometh, oc, tot, disc, ostat = row
    aid_sql = "NULL" if aid is None else str(aid)
    os_sql = "NULL" if os is None else f"'{os}'"
    od_sql = "NULL" if od is None else f"'{od}'"
    oc_sql = "NULL" if oc is None else f"'{oc.replace(chr(39), chr(39)+chr(39))}'"
    comma = "," if i < len(orders) - 1 else ";"
    lines.append(f"    ({oid}, '{uorder(oid)}'::uuid, '{onum}', {cid}, {bid}, {aid_sql}, '{op}', {os_sql}, {od_sql}, '{ometh}'::order_method, {oc_sql}, {tot}, {disc}, '{ostat}'::order_status){comma}")
lines.append("SELECT setval(pg_get_serial_sequence('\"order\"', 'order_id'), (SELECT MAX(order_id) FROM \"order\"));")
lines.append("")

# order_item — order_id UUID, batch_id nullable
lines.append("INSERT INTO order_item (order_item_id, order_id, product_id, batch_id, order_item_quantity, order_item_unit_price_at_time, order_item_line_total)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append("""    (1,  1,  14, 10, 1, 7.25,  7.25),
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
    (46, 15,  3,  NULL,5, 3.49,  17.45);""")
lines.append("SELECT setval(pg_get_serial_sequence('order_item', 'order_item_id'), 46);")
lines.append("")

# payment — paid -> completed
lines.append("INSERT INTO payment (payment_id, uuid, order_id, payment_amount, payment_method, payment_transaction_id, payment_status, payment_paid_at)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append(f"    (1, '{upayment(1)}'::uuid, 1,  26.95, 'credit_card'::payment_method, 'TRX-98314501', 'completed'::payment_status, '2025-12-09 12:00:00+00'),")
lines.append(f"    (2, '{upayment(2)}'::uuid, 2,  12.98, 'debit_card'::payment_method,  'TRX-98314502', 'completed'::payment_status, '2025-12-10 12:00:00+00'),")
lines.append(f"    (3, '{upayment(3)}'::uuid, 3,  32.20, 'credit_card'::payment_method, 'TRX-98314503', 'completed'::payment_status, '2025-12-12 12:00:00+00'),")
lines.append(f"    (4, '{upayment(4)}'::uuid, 4,  9.75,  'credit_card'::payment_method, 'TRX-98314504', 'completed'::payment_status, '2025-12-12 12:00:00+00'),")
lines.append(f"    (5, '{upayment(5)}'::uuid, 5,  37.90, 'credit_card'::payment_method, 'TRX-98314505', 'completed'::payment_status, '2025-12-14 12:00:00+00'),")
lines.append(f"    (6, '{upayment(6)}'::uuid, 6,  18.20, 'debit_card'::payment_method,  'TRX-98314506', 'completed'::payment_status, '2025-12-14 12:00:00+00'),")
lines.append(f"    (7, '{upayment(7)}'::uuid, 7,  22.45, 'credit_card'::payment_method, 'TRX-98314507', 'authorized'::payment_status, NULL),")
lines.append(f"    (8, '{upayment(8)}'::uuid, 8,  7.25,  'credit_card'::payment_method, 'TRX-98314508', 'completed'::payment_status, '2025-12-15 12:00:00+00'),")
lines.append(f"    (9, '{upayment(9)}'::uuid, 9,  53.48, 'credit_card'::payment_method, 'TRX-98314509', 'completed'::payment_status, '2025-12-17 12:00:00+00'),")
lines.append(f"    (10, '{upayment(10)}'::uuid, 10, 6.49,  'debit_card'::payment_method,  'TRX-98314510', 'pending'::payment_status, NULL),")
lines.append(f"    (11, '{upayment(11)}'::uuid, 11, 27.70, 'credit_card'::payment_method, 'TRX-98314511', 'completed'::payment_status, '2025-12-18 12:00:00+00'),")
lines.append(f"    (12, '{upayment(12)}'::uuid, 12, 14.50, 'credit_card'::payment_method, 'TRX-98314512', 'completed'::payment_status, '2025-12-18 12:00:00+00'),")
lines.append(f"    (13, '{upayment(13)}'::uuid, 13, 19.95, 'debit_card'::payment_method,  'TRX-98314513', 'completed'::payment_status, '2025-12-19 12:00:00+00'),")
lines.append(f"    (14, '{upayment(14)}'::uuid, 14, 29.99, 'credit_card'::payment_method, 'TRX-98314514', 'pending'::payment_status, NULL);")
lines.append("SELECT setval(pg_get_serial_sequence('payment', 'payment_id'), (SELECT MAX(payment_id) FROM payment));")
lines.append("")

# reward
lines.append("INSERT INTO reward (reward_id, uuid, customer_id, order_id, reward_points_earned, reward_transaction_date)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append(f"    (1, '{ureward(1)}'::uuid, 1, 1,  26950, '2025-12-09 12:00:00+00'),")
lines.append(f"    (2, '{ureward(2)}'::uuid, 2, 2,  12980, '2025-12-10 12:00:00+00'),")
lines.append(f"    (3, '{ureward(3)}'::uuid, 3, 3,  32200, '2025-12-12 12:00:00+00'),")
lines.append(f"    (4, '{ureward(4)}'::uuid, 4, 4,  9750,  '2025-12-12 12:00:00+00'),")
lines.append(f"    (5, '{ureward(5)}'::uuid, 5, 5,  37900, '2025-12-14 12:00:00+00'),")
lines.append(f"    (6, '{ureward(6)}'::uuid, 6, 6,  18200, '2025-12-14 12:00:00+00'),")
lines.append(f"    (7, '{ureward(7)}'::uuid, 8, 8,  7250,  '2025-12-15 12:00:00+00'),")
lines.append(f"    (8, '{ureward(8)}'::uuid, 9, 9,  53480, '2025-12-17 12:00:00+00'),")
lines.append(f"    (9, '{ureward(9)}'::uuid, 11, 11, 27700, '2025-12-18 12:00:00+00'),")
lines.append(f"    (10, '{ureward(10)}'::uuid, 12, 12, 14500, '2025-12-18 12:00:00+00'),")
lines.append(f"    (11, '{ureward(11)}'::uuid, 13, 13, 19950, '2025-12-19 12:00:00+00');")
lines.append("SELECT setval(pg_get_serial_sequence('reward', 'reward_id'), (SELECT MAX(reward_id) FROM reward));")
lines.append("")

# review — employee_id NULL or integer FK
lines.append("INSERT INTO review (review_id, uuid, customer_id, product_id, employee_id, review_rating, review_comment, review_submitted_date, review_status, review_approval_date)")
lines.append("OVERRIDING SYSTEM VALUE VALUES")
lines.append(f"    (1, '{ureview(1)}'::uuid, 1,  5,  7, 5, 'Fresh and flaky, exactly what I hoped for.',     '2025-12-11 12:00:00+00', 'approved'::review_status, '2025-12-12 12:00:00+00'),")
lines.append(f"    (2, '{ureview(2)}'::uuid, 2,  1,  7, 4, 'Good loaf with a nice crust.',                  '2025-12-12 12:00:00+00', 'approved'::review_status, '2025-12-13 12:00:00+00'),")
lines.append(f"    (3, '{ureview(3)}'::uuid, 3,  13, 7, 5, 'Excellent cake, rich and not overly sweet.',     '2025-12-13 12:00:00+00', 'approved'::review_status, '2025-12-14 12:00:00+00'),")
lines.append(f"    (4, '{ureview(4)}'::uuid, 4,  6,  NULL, 4, 'Muffin was soft and well-balanced.',             '2025-12-13 12:00:00+00', 'pending'::review_status, NULL),")
lines.append(f"    (5, '{ureview(5)}'::uuid, 5,  12, 7, 5, 'Great flavour and texture.',                     '2025-12-14 12:00:00+00', 'approved'::review_status, '2025-12-15 12:00:00+00'),")
lines.append(f"    (6, '{ureview(6)}'::uuid, 6,  16, 7, 3, 'Filling was good, pastry slightly dry.',         '2025-12-14 12:00:00+00', 'approved'::review_status, '2025-12-15 12:00:00+00'),")
lines.append(f"    (7, '{ureview(7)}'::uuid, 7,  10, NULL, 4, 'Cupcake was moist and frosting was smooth.',     '2025-12-15 12:00:00+00', 'pending'::review_status, NULL),")
lines.append(f"    (8, '{ureview(8)}'::uuid, 8,  14, 7, 5, 'Very creamy slice and good crust.',              '2025-12-15 12:00:00+00', 'approved'::review_status, '2025-12-16 12:00:00+00'),")
lines.append(f"    (9, '{ureview(9)}'::uuid, 9,  17, 7, 4, 'Bright flavour and a nice finish.',              '2025-12-16 12:00:00+00', 'approved'::review_status, '2025-12-17 12:00:00+00'),")
lines.append(f"    (10, '{ureview(10)}'::uuid, 10, 3,  NULL, 4, 'Crisp outside and soft inside.',                 '2025-12-17 12:00:00+00', 'pending'::review_status, NULL),")
lines.append(f"    (11, '{ureview(11)}'::uuid, 11, 18, 7, 5, 'Perfect brownie, very fudgy.',                   '2025-12-18 12:00:00+00', 'approved'::review_status, '2025-12-18 12:00:00+00'),")
lines.append(f"    (12, '{ureview(12)}'::uuid, 12, 8,  7, 4, 'Classic cookie, good texture.',                  '2025-12-18 12:00:00+00', 'approved'::review_status, '2025-12-19 12:00:00+00'),")
lines.append(f"    (13, '{ureview(13)}'::uuid, 13, 21, NULL, 4, 'Nice seasonal option, would buy again.',         '2025-12-19 12:00:00+00', 'pending'::review_status, NULL),")
lines.append(f"    (14, '{ureview(14)}'::uuid, 14, 13, NULL, 5, 'Great for an occasion, everyone enjoyed it.',    '2025-12-19 12:00:00+00', 'pending'::review_status, NULL);")
lines.append("SELECT setval(pg_get_serial_sequence('review', 'review_id'), (SELECT MAX(review_id) FROM review));")
lines.append("")

# customer_preference — strength 1..5, map allergic
def pref_strength(s):
    m = {10: 5, 8: 4, 7: 4, 6: 3, 4: 2, 3: 2}
    return m.get(s, min(5, max(1, s // 2)))

prefs = [
    (1,  1,  'like',     7), (1,  10, 'like',     6),
    (2,  5,  'dislike',  8), (2,  9,  'like',     6),
    (3,  2,  'like',     8), (3,  10, 'like',     7),
    (4,  4,  'like',     6), (4,  11, 'allergic', 10),
    (5,  3,  'like',     7), (5,  7,  'like',     6),
    (6,  6,  'dislike',  8), (6,  8,  'like',     6),
    (7,  5,  'allergic', 10), (7,  9,  'like',     6),
    (8,  2,  'like',     7), (8,  10, 'like',     8),
    (9,  7,  'like',     7), (9,  9,  'like',     6),
    (10, 4,  'like',     6), (10, 11, 'allergic', 10),
    (11, 3,  'dislike',  4), (11, 9,  'like',     6),
    (12, 6,  'dislike',  8), (12, 10, 'like',     7),
    (13, 5,  'allergic', 10), (13, 7,  'like',     6),
    (14, 2,  'like',     7), (14, 10, 'like',     6),
]
lines.append("INSERT INTO customer_preference (customer_id, tag_id, preference_type, preference_strength) VALUES")
for i, (cid, tid, pt, strn) in enumerate(prefs):
    ps = pref_strength(strn)
    comma = "," if i < len(prefs) - 1 else ";"
    lines.append(f"    ({cid}, {tid}, '{pt}'::preference_type, {ps}::smallint){comma}")
lines.append("")

out = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "db" / "migration" / "V3__seed_data.sql"
out.write_text("\n".join(lines), encoding="utf-8")
print(f"Wrote {out}")
