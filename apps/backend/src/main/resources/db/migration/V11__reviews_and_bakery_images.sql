-- Hero images (DigitalOcean Spaces) for each seeded bakery row.
UPDATE bakery
SET bakery_image_url = 'https://peelin-good-storage.tor1.digitaloceanspaces.com/locations/north-harbour-bakery-downtown.jpg'
WHERE bakery_id = 1;
UPDATE bakery
SET bakery_image_url = 'https://peelin-good-storage.tor1.digitaloceanspaces.com/locations/north-harbour-bakery-edmonton-central.jpg'
WHERE bakery_id = 2;
UPDATE bakery
SET bakery_image_url = 'https://peelin-good-storage.tor1.digitaloceanspaces.com/locations/north-harbour-bakery-toronto-financial.jpg'
WHERE bakery_id = 3;

-- Extra test reviews (existing customers + products only). Leaves several products with no reviews (e.g. 7, 19, 20, 22, 23, 24, 25).
-- Variety: one product with many approved, some with two, three, pending-only mix, one rejected, etc.
INSERT INTO review (review_id, uuid, customer_id, product_id, employee_id, review_rating, review_comment, review_submitted_date, review_status, review_approval_date) VALUES
    -- Product 2 (Multigrain): five approved — good for "top 3 newest" carousel testing
    ('70000000-0000-4000-8000-000000000015'::uuid, '70000000-0000-4000-8000-000000000015'::uuid, '20000000-0000-4000-8000-000000000001'::uuid, 2,  '30000000-0000-4000-8000-000000000007'::uuid, 5, 'House favourite for sandwiches — stays soft for days.',           '2026-03-01 10:00:00+00', 'approved'::review_status,  '2026-03-02 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000016'::uuid, '70000000-0000-4000-8000-000000000016'::uuid, '20000000-0000-4000-8000-000000000002'::uuid, 2,  '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Nutty and hearty; great toasted.',                                '2026-03-03 11:00:00+00', 'approved'::review_status,  '2026-03-04 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000017'::uuid, '70000000-0000-4000-8000-000000000017'::uuid, '20000000-0000-4000-8000-000000000003'::uuid, 2,  '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Best multigrain in the city, in my opinion.',                     '2026-03-05 14:00:00+00', 'approved'::review_status,  '2026-03-06 10:00:00+00'),
    ('70000000-0000-4000-8000-000000000018'::uuid, '70000000-0000-4000-8000-000000000018'::uuid, '20000000-0000-4000-8000-000000000004'::uuid, 2,  '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Dense in a good way — fills you up.',                             '2026-03-07 08:30:00+00', 'approved'::review_status,  '2026-03-08 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000019'::uuid, '70000000-0000-4000-8000-000000000019'::uuid, '20000000-0000-4000-8000-000000000005'::uuid, 2,  '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Kids ask for this loaf every week.',                              '2026-03-09 16:00:00+00', 'approved'::review_status,  '2026-03-10 09:00:00+00'),
    -- Product 4 (Cinnamon Roll): exactly two approved
    ('70000000-0000-4000-8000-000000000020'::uuid, '70000000-0000-4000-8000-000000000020'::uuid, '20000000-0000-4000-8000-000000000006'::uuid, 4,  '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Sticky, glossy glaze — worth the trip.',                          '2026-02-20 12:00:00+00', 'approved'::review_status,  '2026-02-21 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000021'::uuid, '70000000-0000-4000-8000-000000000021'::uuid, '20000000-0000-4000-8000-000000000007'::uuid, 4,  '30000000-0000-4000-8000-000000000007'::uuid, 4, 'A bit sweet for breakfast but perfect with coffee.',               '2026-02-22 12:00:00+00', 'approved'::review_status,  '2026-02-23 09:00:00+00'),
    -- Product 9 (Oatmeal Raisin): one approved + one pending
    ('70000000-0000-4000-8000-000000000022'::uuid, '70000000-0000-4000-8000-000000000022'::uuid, '20000000-0000-4000-8000-000000000008'::uuid, 9,  '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Chewy centers, crisp edges — classic done right.',                 '2026-03-04 09:00:00+00', 'approved'::review_status,  '2026-03-05 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000023'::uuid, '70000000-0000-4000-8000-000000000023'::uuid, '20000000-0000-4000-8000-000000000009'::uuid, 9,  NULL,                                         3, 'Raisins were plump; hoping for a bit more spice next time.',      '2026-03-11 15:00:00+00', 'pending'::review_status,  NULL),
    -- Product 11 (Chocolate Cupcake): three approved
    ('70000000-0000-4000-8000-000000000024'::uuid, '70000000-0000-4000-8000-000000000024'::uuid, '20000000-0000-4000-8000-000000000010'::uuid, 11, '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Frosting is silky, cake is moist.',                               '2026-01-28 10:00:00+00', 'approved'::review_status,  '2026-01-29 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000025'::uuid, '70000000-0000-4000-8000-000000000025'::uuid, '20000000-0000-4000-8000-000000000011'::uuid, 11, '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Rich chocolate, not too bitter.',                                  '2026-01-30 10:00:00+00', 'approved'::review_status,  '2026-01-31 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000026'::uuid, '70000000-0000-4000-8000-000000000026'::uuid, '20000000-0000-4000-8000-000000000012'::uuid, 11, '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Ordered a dozen for the office — gone in an hour.',                '2026-02-01 10:00:00+00', 'approved'::review_status,  '2026-02-02 09:00:00+00'),
    -- Product 15 (Apple Turnover): one approved + one rejected
    ('70000000-0000-4000-8000-000000000027'::uuid, '70000000-0000-4000-8000-000000000027'::uuid, '20000000-0000-4000-8000-000000000013'::uuid, 15, '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Flaky layers and tart apples — lovely afternoon treat.',          '2026-02-10 11:00:00+00', 'approved'::review_status,  '2026-02-11 09:00:00+00'),
    ('70000000-0000-4000-8000-000000000028'::uuid, '70000000-0000-4000-8000-000000000028'::uuid, '20000000-0000-4000-8000-000000000014'::uuid, 15, '30000000-0000-4000-8000-000000000007'::uuid, 2, 'Mine arrived under-baked; sharing in case it was a one-off.',       '2026-02-12 11:00:00+00', 'rejected'::review_status,  NULL),
    -- Product 26 (Chocolate Eclair): single approved (several products still have zero reviews)
    ('70000000-0000-4000-8000-000000000029'::uuid, '70000000-0000-4000-8000-000000000029'::uuid, '20000000-0000-4000-8000-000000000001'::uuid, 26, '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Choux held up, chocolate ganache was smooth.',                      '2026-03-08 13:00:00+00', 'approved'::review_status,  '2026-03-09 09:00:00+00');
