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
-- When this migration runs in order (before V14/V18), review has no order_id/bakery_id yet — use the legacy INSERT.
-- When it runs out-of-order on a cloud DB already at a high version, bakery_id is NOT NULL and we must supply it (see V27 bakery mapping).
-- Skip rows that would violate later unique (customer_id, product_id) for product reviews (order_id IS NULL).
DO $v11_reviews$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'review'
          AND column_name = 'bakery_id'
    ) THEN
        INSERT INTO review (
            review_id,
            uuid,
            customer_id,
            product_id,
            employee_id,
            order_id,
            bakery_id,
            review_rating,
            review_comment,
            review_submitted_date,
            review_status,
            review_approval_date
        )
        SELECT v.review_id,
               v.uuid,
               v.customer_id,
               v.product_id,
               v.employee_id,
               v.order_id,
               v.bakery_id,
               v.review_rating,
               v.review_comment,
               v.review_submitted_date,
               v.review_status,
               v.review_approval_date
        FROM (
                 VALUES
                     ('70000000-0000-4000-8000-000000000015'::uuid, '70000000-0000-4000-8000-000000000015'::uuid,
                      '20000000-0000-4000-8000-000000000001'::uuid, 2,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'House favourite for sandwiches — stays soft for days.'::varchar,
                      '2026-03-01 10:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-02 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000016'::uuid, '70000000-0000-4000-8000-000000000016'::uuid,
                      '20000000-0000-4000-8000-000000000002'::uuid, 2,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      4::smallint, 'Nutty and hearty; great toasted.'::varchar,
                      '2026-03-03 11:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-04 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000017'::uuid, '70000000-0000-4000-8000-000000000017'::uuid,
                      '20000000-0000-4000-8000-000000000003'::uuid, 2,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'Best multigrain in the city, in my opinion.'::varchar,
                      '2026-03-05 14:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-06 10:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000018'::uuid, '70000000-0000-4000-8000-000000000018'::uuid,
                      '20000000-0000-4000-8000-000000000004'::uuid, 2,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      4::smallint, 'Dense in a good way — fills you up.'::varchar,
                      '2026-03-07 08:30:00+00'::timestamptz, 'approved'::review_status, '2026-03-08 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000019'::uuid, '70000000-0000-4000-8000-000000000019'::uuid,
                      '20000000-0000-4000-8000-000000000005'::uuid, 2,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'Kids ask for this loaf every week.'::varchar,
                      '2026-03-09 16:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-10 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000020'::uuid, '70000000-0000-4000-8000-000000000020'::uuid,
                      '20000000-0000-4000-8000-000000000006'::uuid, 4,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 3,
                      5::smallint, 'Sticky, glossy glaze — worth the trip.'::varchar,
                      '2026-02-20 12:00:00+00'::timestamptz, 'approved'::review_status, '2026-02-21 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000021'::uuid, '70000000-0000-4000-8000-000000000021'::uuid,
                      '20000000-0000-4000-8000-000000000007'::uuid, 4,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 3,
                      4::smallint, 'A bit sweet for breakfast but perfect with coffee.'::varchar,
                      '2026-02-22 12:00:00+00'::timestamptz, 'approved'::review_status, '2026-02-23 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000022'::uuid, '70000000-0000-4000-8000-000000000022'::uuid,
                      '20000000-0000-4000-8000-000000000008'::uuid, 9,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'Chewy centers, crisp edges — classic done right.'::varchar,
                      '2026-03-04 09:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-05 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000023'::uuid, '70000000-0000-4000-8000-000000000023'::uuid,
                      '20000000-0000-4000-8000-000000000009'::uuid, 9,
                      NULL::uuid, NULL::uuid, 2,
                      3::smallint, 'Raisins were plump; hoping for a bit more spice next time.'::varchar,
                      '2026-03-11 15:00:00+00'::timestamptz, 'pending'::review_status, NULL::timestamptz),
                     ('70000000-0000-4000-8000-000000000024'::uuid, '70000000-0000-4000-8000-000000000024'::uuid,
                      '20000000-0000-4000-8000-000000000010'::uuid, 11,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'Frosting is silky, cake is moist.'::varchar,
                      '2026-01-28 10:00:00+00'::timestamptz, 'approved'::review_status, '2026-01-29 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000025'::uuid, '70000000-0000-4000-8000-000000000025'::uuid,
                      '20000000-0000-4000-8000-000000000011'::uuid, 11,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      4::smallint, 'Rich chocolate, not too bitter.'::varchar,
                      '2026-01-30 10:00:00+00'::timestamptz, 'approved'::review_status, '2026-01-31 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000026'::uuid, '70000000-0000-4000-8000-000000000026'::uuid,
                      '20000000-0000-4000-8000-000000000012'::uuid, 11,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 2,
                      5::smallint, 'Ordered a dozen for the office — gone in an hour.'::varchar,
                      '2026-02-01 10:00:00+00'::timestamptz, 'approved'::review_status, '2026-02-02 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000027'::uuid, '70000000-0000-4000-8000-000000000027'::uuid,
                      '20000000-0000-4000-8000-000000000013'::uuid, 15,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 3,
                      4::smallint, 'Flaky layers and tart apples — lovely afternoon treat.'::varchar,
                      '2026-02-10 11:00:00+00'::timestamptz, 'approved'::review_status, '2026-02-11 09:00:00+00'::timestamptz),
                     ('70000000-0000-4000-8000-000000000028'::uuid, '70000000-0000-4000-8000-000000000028'::uuid,
                      '20000000-0000-4000-8000-000000000014'::uuid, 15,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 3,
                      2::smallint, 'Mine arrived under-baked; sharing in case it was a one-off.'::varchar,
                      '2026-02-12 11:00:00+00'::timestamptz, 'rejected'::review_status, NULL::timestamptz),
                     ('70000000-0000-4000-8000-000000000029'::uuid, '70000000-0000-4000-8000-000000000029'::uuid,
                      '20000000-0000-4000-8000-000000000001'::uuid, 26,
                      '30000000-0000-4000-8000-000000000007'::uuid, NULL::uuid, 3,
                      5::smallint, 'Choux held up, chocolate ganache was smooth.'::varchar,
                      '2026-03-08 13:00:00+00'::timestamptz, 'approved'::review_status, '2026-03-09 09:00:00+00'::timestamptz)
             ) AS v(review_id, uuid, customer_id, product_id, employee_id, order_id, bakery_id,
                    review_rating, review_comment, review_submitted_date, review_status, review_approval_date)
        WHERE NOT EXISTS (
            SELECT 1
            FROM review r
            WHERE r.customer_id = v.customer_id
              AND r.product_id = v.product_id
              AND r.order_id IS NULL
        );
    ELSE
        INSERT INTO review (review_id, uuid, customer_id, product_id, employee_id, review_rating, review_comment,
                            review_submitted_date, review_status, review_approval_date)
        VALUES
            ('70000000-0000-4000-8000-000000000015'::uuid, '70000000-0000-4000-8000-000000000015'::uuid,
             '20000000-0000-4000-8000-000000000001'::uuid, 2,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'House favourite for sandwiches — stays soft for days.', '2026-03-01 10:00:00+00', 'approved'::review_status,
             '2026-03-02 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000016'::uuid, '70000000-0000-4000-8000-000000000016'::uuid,
             '20000000-0000-4000-8000-000000000002'::uuid, 2,
             '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Nutty and hearty; great toasted.', '2026-03-03 11:00:00+00',
             'approved'::review_status, '2026-03-04 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000017'::uuid, '70000000-0000-4000-8000-000000000017'::uuid,
             '20000000-0000-4000-8000-000000000003'::uuid, 2,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'Best multigrain in the city, in my opinion.', '2026-03-05 14:00:00+00', 'approved'::review_status,
             '2026-03-06 10:00:00+00'),
            ('70000000-0000-4000-8000-000000000018'::uuid, '70000000-0000-4000-8000-000000000018'::uuid,
             '20000000-0000-4000-8000-000000000004'::uuid, 2,
             '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Dense in a good way — fills you up.', '2026-03-07 08:30:00+00',
             'approved'::review_status, '2026-03-08 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000019'::uuid, '70000000-0000-4000-8000-000000000019'::uuid,
             '20000000-0000-4000-8000-000000000005'::uuid, 2,
             '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Kids ask for this loaf every week.', '2026-03-09 16:00:00+00',
             'approved'::review_status, '2026-03-10 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000020'::uuid, '70000000-0000-4000-8000-000000000020'::uuid,
             '20000000-0000-4000-8000-000000000006'::uuid, 4,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'Sticky, glossy glaze — worth the trip.', '2026-02-20 12:00:00+00', 'approved'::review_status,
             '2026-02-21 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000021'::uuid, '70000000-0000-4000-8000-000000000021'::uuid,
             '20000000-0000-4000-8000-000000000007'::uuid, 4,
             '30000000-0000-4000-8000-000000000007'::uuid, 4,
             'A bit sweet for breakfast but perfect with coffee.', '2026-02-22 12:00:00+00', 'approved'::review_status,
             '2026-02-23 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000022'::uuid, '70000000-0000-4000-8000-000000000022'::uuid,
             '20000000-0000-4000-8000-000000000008'::uuid, 9,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'Chewy centers, crisp edges — classic done right.', '2026-03-04 09:00:00+00', 'approved'::review_status,
             '2026-03-05 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000023'::uuid, '70000000-0000-4000-8000-000000000023'::uuid,
             '20000000-0000-4000-8000-000000000009'::uuid, 9, NULL, 3,
             'Raisins were plump; hoping for a bit more spice next time.', '2026-03-11 15:00:00+00', 'pending'::review_status,
             NULL),
            ('70000000-0000-4000-8000-000000000024'::uuid, '70000000-0000-4000-8000-000000000024'::uuid,
             '20000000-0000-4000-8000-000000000010'::uuid, 11,
             '30000000-0000-4000-8000-000000000007'::uuid, 5, 'Frosting is silky, cake is moist.', '2026-01-28 10:00:00+00',
             'approved'::review_status, '2026-01-29 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000025'::uuid, '70000000-0000-4000-8000-000000000025'::uuid,
             '20000000-0000-4000-8000-000000000011'::uuid, 11,
             '30000000-0000-4000-8000-000000000007'::uuid, 4, 'Rich chocolate, not too bitter.', '2026-01-30 10:00:00+00',
             'approved'::review_status, '2026-01-31 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000026'::uuid, '70000000-0000-4000-8000-000000000026'::uuid,
             '20000000-0000-4000-8000-000000000012'::uuid, 11,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'Ordered a dozen for the office — gone in an hour.', '2026-02-01 10:00:00+00', 'approved'::review_status,
             '2026-02-02 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000027'::uuid, '70000000-0000-4000-8000-000000000027'::uuid,
             '20000000-0000-4000-8000-000000000013'::uuid, 15,
             '30000000-0000-4000-8000-000000000007'::uuid, 4,
             'Flaky layers and tart apples — lovely afternoon treat.', '2026-02-10 11:00:00+00', 'approved'::review_status,
             '2026-02-11 09:00:00+00'),
            ('70000000-0000-4000-8000-000000000028'::uuid, '70000000-0000-4000-8000-000000000028'::uuid,
             '20000000-0000-4000-8000-000000000014'::uuid, 15,
             '30000000-0000-4000-8000-000000000007'::uuid, 2,
             'Mine arrived under-baked; sharing in case it was a one-off.', '2026-02-12 11:00:00+00', 'rejected'::review_status,
             NULL),
            ('70000000-0000-4000-8000-000000000029'::uuid, '70000000-0000-4000-8000-000000000029'::uuid,
             '20000000-0000-4000-8000-000000000001'::uuid, 26,
             '30000000-0000-4000-8000-000000000007'::uuid, 5,
             'Choux held up, chocolate ganache was smooth.', '2026-03-08 13:00:00+00', 'approved'::review_status,
             '2026-03-09 09:00:00+00');
    END IF;
END $v11_reviews$;
