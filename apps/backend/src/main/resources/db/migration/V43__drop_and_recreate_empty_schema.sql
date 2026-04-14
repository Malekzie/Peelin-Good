-- =============================================================================
-- V43__drop_and_recreate_empty_schema.sql (Flyway) — THREADED-PROJECT repo root (not Workshop-7 classpath).
--
-- Versioned migration (runs once): drops application tables in FK-safe order, then recreates an empty
-- schema merged from legacy V0,V1,V2,V4..V41 (same body as flyway-migrations/V42; excludes data-only
-- migrations; skips duplicate tax_rate DDL from V24). Keeps tax_rate rows and flyway_schema_history.
--
-- Pipeline with sibling files here: V43__drop_and_recreate_empty_schema.sql → R__reference_seed_v0.sql
-- =============================================================================

DROP TABLE IF EXISTS chat_message CASCADE;
DROP TABLE IF EXISTS chat_thread CASCADE;
DROP TABLE IF EXISTS message CASCADE;
DROP TABLE IF EXISTS review CASCADE;
DROP TABLE IF EXISTS reward CASCADE;
DROP TABLE IF EXISTS payment CASCADE;
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS "order" CASCADE;
DROP TABLE IF EXISTS employee_customer_link CASCADE;
DROP TABLE IF EXISTS batch_inventory CASCADE;
DROP TABLE IF EXISTS batch CASCADE;
DROP TABLE IF EXISTS customer_preference CASCADE;
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS employee CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS product_tag CASCADE;
DROP TABLE IF EXISTS product_special CASCADE;
DROP TABLE IF EXISTS supplier CASCADE;
DROP TABLE IF EXISTS bakery_hours CASCADE;
DROP TABLE IF EXISTS bakery CASCADE;
DROP TABLE IF EXISTS product CASCADE;
DROP TABLE IF EXISTS tag CASCADE;
DROP TABLE IF EXISTS reward_tier CASCADE;
DROP TABLE IF EXISTS password_reset_token CASCADE;
DROP TABLE IF EXISTS address CASCADE;
DROP TABLE IF EXISTS staff_message CASCADE;
DROP TABLE IF EXISTS staff_conversation CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

-- ---- V0__extensions.sql ----
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---- V1__baseline.sql ----
-- =============================================
-- V1__baseline.sql
-- Flyway baseline migration for Bakery Management System
-- =============================================

-- ===================
-- EXTENSION
-- ===================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ===================
-- ENUM TYPES
-- ===================

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('admin', 'employee', 'customer');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE order_status AS ENUM ('pending_payment', 'paid', 'preparing', 'ready', 'picked_up', 'cancelled');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_method') THEN
        CREATE TYPE order_method AS ENUM ('pickup', 'delivery');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'preference_type') THEN
        CREATE TYPE preference_type AS ENUM ('like', 'dislike', 'avoid', 'allergy');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM ('pending', 'completed', 'failed', 'refunded');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method') THEN
        CREATE TYPE payment_method AS ENUM ('cash', 'credit_card', 'debit_card', 'online');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'review_status') THEN
        CREATE TYPE review_status AS ENUM ('pending', 'approved', 'rejected');
    END IF;
END $$;

-- ===================
-- TABLES
-- ===================

CREATE TABLE IF NOT EXISTS address (
    address_id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address_line1       VARCHAR(120) NOT NULL,
    address_line2       VARCHAR(120),
    address_city        VARCHAR(120) NOT NULL,
    address_province    VARCHAR(80)  NOT NULL,
    address_postal_code VARCHAR(10)  NOT NULL
);

CREATE TABLE IF NOT EXISTS "user" (
                                      user_id            UUID         DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    user_email         VARCHAR(254) NOT NULL UNIQUE,
    user_password_hash VARCHAR(255) NOT NULL,
    user_role          user_role    NOT NULL,
    user_created_at    TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS bakery (
    bakery_id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      address_id   INTEGER      NOT NULL REFERENCES address,
                                      bakery_name  VARCHAR(100) NOT NULL,
                                      bakery_phone VARCHAR(20)  NOT NULL,
                                      bakery_email VARCHAR(254) NOT NULL
);

CREATE TABLE IF NOT EXISTS bakery_hours (
    bakery_hours_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bakery_id       INTEGER               NOT NULL REFERENCES bakery,
    day_of_week     SMALLINT              NOT NULL
                                                CONSTRAINT bakery_hours_day_of_week_check CHECK (day_of_week >= 0 AND day_of_week <= 6),
    open_time       TIME,
    close_time      TIME,
    is_closed       BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS reward_tier (
                                           reward_tier_id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                           reward_tier_name          VARCHAR(30)   NOT NULL,
                                           reward_tier_min_points    INTEGER       NOT NULL,
                                           reward_tier_max_points    INTEGER,
                                           reward_tier_discount_rate NUMERIC(5, 2)
);

CREATE TABLE IF NOT EXISTS customer (
                                        customer_id                 UUID    DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
                                        user_id                     UUID    REFERENCES "user",
    address_id                  INTEGER NOT NULL REFERENCES address,
    reward_tier_id              INTEGER NOT NULL REFERENCES reward_tier,
    customer_first_name         VARCHAR(50)  NOT NULL,
    customer_middle_initial     VARCHAR(2),
    customer_last_name          VARCHAR(50)  NOT NULL,
    customer_phone              VARCHAR(20)  NOT NULL,
    customer_business_phone     VARCHAR(20),
    customer_email              VARCHAR(254) NOT NULL,
    customer_reward_balance     INTEGER DEFAULT 0 NOT NULL,
    customer_tier_assigned_date DATE,
    guest_expiry_date           DATE,
                                        CONSTRAINT chk_customer_email CHECK (user_id IS NOT NULL OR customer_email IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_customer_user ON customer (user_id);
CREATE INDEX IF NOT EXISTS idx_customer_guest_expiry ON customer (guest_expiry_date) WHERE guest_expiry_date IS NOT NULL;

CREATE TABLE IF NOT EXISTS employee (
                                        employee_id             UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
                                        user_id                 UUID    NOT NULL REFERENCES "user",
    address_id              INTEGER NOT NULL REFERENCES address,
    bakery_id               INTEGER NOT NULL REFERENCES bakery,
    employee_first_name     VARCHAR(50)  NOT NULL,
    employee_middle_initial CHAR(2),
    employee_last_name      VARCHAR(50)  NOT NULL,
    employee_position       VARCHAR(40)  NOT NULL,
    employee_phone          VARCHAR(20)  NOT NULL,
    employee_business_phone VARCHAR(20),
                                        employee_work_email     VARCHAR(254) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address_id     INTEGER      NOT NULL REFERENCES address,
    supplier_name  VARCHAR(120) NOT NULL,
    supplier_phone VARCHAR(20),
    supplier_email VARCHAR(254)
);

CREATE TABLE IF NOT EXISTS inventory (
    inventory_id               INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bakery_id                  INTEGER        NOT NULL REFERENCES bakery,
    supplier_id                INTEGER        NOT NULL REFERENCES supplier,
    inventory_item_name        VARCHAR(120)   NOT NULL,
    inventory_item_type        VARCHAR(40)    NOT NULL,
    inventory_quantity_on_hand NUMERIC(12, 3) NOT NULL,
    inventory_unit_of_measure  VARCHAR(20)    NOT NULL
);

CREATE TABLE IF NOT EXISTS tag (
    tag_id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product (
    product_id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name        VARCHAR(120)   NOT NULL,
    product_description VARCHAR(1000),
    product_base_price  NUMERIC(10, 2) NOT NULL,
    product_image_url   VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS product_tag (
    product_id INTEGER NOT NULL REFERENCES product,
    tag_id     INTEGER NOT NULL REFERENCES tag,
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE IF NOT EXISTS batch (
    batch_id                INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bakery_id               INTEGER NOT NULL REFERENCES bakery,
    product_id              INTEGER NOT NULL REFERENCES product,
                                     employee_id             UUID    NOT NULL REFERENCES employee,
                                     batch_production_date   DATE    NOT NULL,
                                     batch_expiry_date       DATE,
    batch_quantity_produced INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_batch_product ON batch (product_id);
CREATE INDEX IF NOT EXISTS idx_batch_bakery ON batch (bakery_id);

CREATE TABLE IF NOT EXISTS batch_inventory (
    batch_id                INTEGER        NOT NULL REFERENCES batch,
    inventory_id            INTEGER        NOT NULL REFERENCES inventory,
    quantity_used           NUMERIC(12, 3) NOT NULL,
    unit_of_measure_at_time VARCHAR(20)    NOT NULL,
    usage_recorded_date     TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (batch_id, inventory_id)
);

CREATE TABLE IF NOT EXISTS "order" (
                                       order_id                 UUID           DEFAULT gen_random_uuid()               NOT NULL PRIMARY KEY,
    order_number             VARCHAR(20)    NOT NULL UNIQUE,
                                       customer_id              UUID           REFERENCES customer,
    bakery_id                INTEGER        NOT NULL REFERENCES bakery,
    address_id               INTEGER        REFERENCES address,
    guest_name               VARCHAR(100),
    guest_email              VARCHAR(254),
    guest_phone              VARCHAR(20),
                                       order_placed_datetime    TIMESTAMP WITH TIME ZONE DEFAULT now()                 NOT NULL,
    order_scheduled_datetime TIMESTAMP WITH TIME ZONE,
    order_delivered_datetime TIMESTAMP WITH TIME ZONE,
    order_method             order_method   NOT NULL,
    order_comment            VARCHAR(500),
    order_total              NUMERIC(10, 2) NOT NULL,
    order_discount           NUMERIC(10, 2) DEFAULT 0 NOT NULL,
                                       order_status             order_status   DEFAULT 'pending_payment' NOT NULL,
    CONSTRAINT chk_order_customer_or_guest
        CHECK (customer_id IS NOT NULL OR (guest_name IS NOT NULL AND guest_email IS NOT NULL)),
    CONSTRAINT chk_order_delivery_address
        CHECK (order_method = 'pickup' OR address_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_order_customer ON "order" (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_bakery ON "order" (bakery_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON "order" (order_status);
CREATE INDEX IF NOT EXISTS idx_order_number ON "order" (order_number);

CREATE TABLE IF NOT EXISTS order_item (
    order_item_id                 INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          order_id                      UUID           NOT NULL REFERENCES "order",
    product_id                    INTEGER        NOT NULL REFERENCES product,
    batch_id                      INTEGER        REFERENCES batch,
    order_item_quantity           INTEGER        NOT NULL,
    order_item_unit_price_at_time NUMERIC(10, 2) NOT NULL,
    order_item_line_total         NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment (
                                       payment_id             UUID           DEFAULT gen_random_uuid()  NOT NULL PRIMARY KEY,
                                       order_id               UUID           NOT NULL REFERENCES "order",
    payment_amount         NUMERIC(10, 2) NOT NULL,
    payment_method         payment_method NOT NULL,
    payment_transaction_id VARCHAR(100),
    payment_status         payment_status DEFAULT 'pending' NOT NULL,
    payment_paid_at        TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS reward (
                                      reward_id               UUID    DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
                                      customer_id             UUID    NOT NULL REFERENCES customer,
                                      order_id                UUID    NOT NULL REFERENCES "order",
    reward_points_earned    INTEGER NOT NULL,
    reward_transaction_date TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS review (
                                      review_id             UUID         DEFAULT gen_random_uuid()  NOT NULL PRIMARY KEY,
                                      customer_id           UUID         NOT NULL REFERENCES customer,
                                      product_id            INTEGER      NOT NULL REFERENCES product,
                                      employee_id           UUID         REFERENCES employee,
                                      review_rating         SMALLINT     NOT NULL
                                          CONSTRAINT review_review_rating_check CHECK (review_rating >= 1 AND review_rating <= 5),
    review_comment        VARCHAR(2000),
                                      review_submitted_date TIMESTAMP WITH TIME ZONE DEFAULT now()  NOT NULL,
    review_status         review_status DEFAULT 'pending' NOT NULL,
    review_approval_date  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_review_product ON review (product_id);

CREATE TABLE IF NOT EXISTS customer_preference (
                                                   customer_id         UUID            NOT NULL REFERENCES customer,
    tag_id              INTEGER         NOT NULL REFERENCES tag,
    preference_type     preference_type NOT NULL,
    preference_strength SMALLINT
                                                       CONSTRAINT customer_preference_preference_strength_check CHECK (preference_strength >= 1 AND preference_strength <= 5),
    PRIMARY KEY (customer_id, tag_id)
);

CREATE TABLE IF NOT EXISTS message (
                                       message_id            UUID         DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
                                       sender_id             UUID         NOT NULL REFERENCES "user",
                                       receiver_id           UUID         NOT NULL REFERENCES "user",
    message_subject       VARCHAR(255)  NOT NULL,
    message_content       VARCHAR(2000) NOT NULL,
    message_sent_datetime TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
                                       message_is_read       BOOLEAN      DEFAULT FALSE NOT NULL
);

-- ---- V2__unified_schema.sql ----
-- =============================================
-- V2__unified_schema.sql (Flyway)
-- Incremental migration from existing V1 baseline.
-- Safe for environments that already applied V1.
-- =============================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Add new enum values used by current code/unified model.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'order_status'::regtype AND enumlabel = 'placed') THEN
            ALTER TYPE order_status ADD VALUE 'placed';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'order_status'::regtype AND enumlabel = 'scheduled') THEN
            ALTER TYPE order_status ADD VALUE 'scheduled';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'order_status'::regtype AND enumlabel = 'delivered') THEN
            ALTER TYPE order_status ADD VALUE 'delivered';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'order_status'::regtype AND enumlabel = 'completed') THEN
            ALTER TYPE order_status ADD VALUE 'completed';
        END IF;
    END IF;
END $$;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'payment_status'::regtype AND enumlabel = 'authorized') THEN
            ALTER TYPE payment_status ADD VALUE 'authorized';
        END IF;
    END IF;
END $$;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'preference_type') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumtypid = 'preference_type'::regtype AND enumlabel = 'allergic') THEN
            ALTER TYPE preference_type ADD VALUE 'allergic';
        END IF;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'bakery_status') THEN
        CREATE TYPE bakery_status AS ENUM ('open', 'closed', 'maintenance');
    END IF;
END $$;

-- Add columns introduced in unified schema/entities while preserving V1 key types.
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_uuid_key') THEN
        ALTER TABLE "user" ADD CONSTRAINT user_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE bakery ADD COLUMN IF NOT EXISTS status bakery_status NOT NULL DEFAULT 'open';
ALTER TABLE bakery ADD COLUMN IF NOT EXISTS latitude NUMERIC(9, 6);
ALTER TABLE bakery ADD COLUMN IF NOT EXISTS longitude NUMERIC(9, 6);

ALTER TABLE customer ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE customer ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS photo_approval_pending BOOLEAN NOT NULL DEFAULT FALSE;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'customer_uuid_key') THEN
        ALTER TABLE customer ADD CONSTRAINT customer_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE employee ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE employee ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS photo_approval_pending BOOLEAN NOT NULL DEFAULT FALSE;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'employee_uuid_key') THEN
        ALTER TABLE employee ADD CONSTRAINT employee_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'order_uuid_key') THEN
        ALTER TABLE "order" ADD CONSTRAINT order_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE payment ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'payment_uuid_key') THEN
        ALTER TABLE payment ADD CONSTRAINT payment_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE reward ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'reward_uuid_key') THEN
        ALTER TABLE reward ADD CONSTRAINT reward_uuid_key UNIQUE (uuid);
    END IF;
END $$;

ALTER TABLE review ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'review_uuid_key') THEN
        ALTER TABLE review ADD CONSTRAINT review_uuid_key UNIQUE (uuid);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_review_customer ON review (customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_order ON payment (order_id);
CREATE INDEX IF NOT EXISTS idx_reward_customer ON reward (customer_id);

-- Add chat tables with UUID FKs to match existing V1 user.user_id type.
CREATE TABLE IF NOT EXISTS chat_thread (
    thread_id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_user_id  UUID NOT NULL REFERENCES "user",
    employee_user_id  UUID REFERENCES "user",
    status            VARCHAR(20) DEFAULT 'open' NOT NULL,
    category          VARCHAR(30) NOT NULL DEFAULT 'general',
    closed_at         TIMESTAMPTZ,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_thread_customer ON chat_thread (customer_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_thread_employee ON chat_thread (employee_user_id);

CREATE TABLE IF NOT EXISTS chat_message (
    message_id     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    thread_id      INTEGER NOT NULL REFERENCES chat_thread ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES "user",
    message_text   VARCHAR(2000),
    sent_at        TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    is_read        BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_message_thread ON chat_message (thread_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender ON chat_message (sender_user_id);


-- ---- V4__make_customer_address_nullable.sql ----
ALTER TABLE customer ALTER COLUMN address_id DROP NOT NULL;


-- ---- V5__move_profile_photo_to_user.sql ----
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);

UPDATE "user" u
SET profile_photo_path = c.profile_photo_path
FROM customer c
WHERE c.user_id = u.user_id
  AND c.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');

UPDATE "user" u
SET profile_photo_path = e.profile_photo_path
FROM employee e
WHERE e.user_id = u.user_id
  AND e.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');


-- ---- V6__photo_fields_only_on_user.sql ----
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS photo_approval_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);

UPDATE "user" u
SET photo_approval_pending = c.photo_approval_pending
FROM customer c
WHERE c.user_id = u.user_id
  AND c.photo_approval_pending IS NOT NULL;

UPDATE "user" u
SET photo_approval_pending = e.photo_approval_pending
FROM employee e
WHERE e.user_id = u.user_id
  AND e.photo_approval_pending IS NOT NULL;

UPDATE "user" u
SET profile_photo_path = c.profile_photo_path
FROM customer c
WHERE c.user_id = u.user_id
  AND c.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');

UPDATE "user" u
SET profile_photo_path = e.profile_photo_path
FROM employee e
WHERE e.user_id = u.user_id
  AND e.profile_photo_path IS NOT NULL
  AND (u.profile_photo_path IS NULL OR u.profile_photo_path = '');

ALTER TABLE customer DROP COLUMN IF EXISTS profile_photo_path;
ALTER TABLE customer DROP COLUMN IF EXISTS photo_approval_pending;
ALTER TABLE employee DROP COLUMN IF EXISTS profile_photo_path;
ALTER TABLE employee DROP COLUMN IF EXISTS photo_approval_pending;


-- ---- V7__normalize_profile_photos_and_product_image_urls.sql ----
-- Original migration cleared legacy user profile paths and normalized product image URLs.
-- Canonical product/bakery URLs and NULL profile photos live in R__reference_seed_v0.sql (no duplicate DML here).

-- ---- V8__product_special.sql ----
-- product_special: calendar-based featured product for the storefront.
-- Lookup: match "date" to current date (local/app timezone). If multiple rows share a date, use the first row returned (ORDER BY product_special_id LIMIT 1).

CREATE TABLE IF NOT EXISTS product_special (
    product_special_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id         INTEGER NOT NULL REFERENCES product (product_id) ON DELETE CASCADE,
    "date"             DATE    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_special_date ON product_special ("date");

-- product_special rows reference product_id; seed data is loaded by repeatable migration R__reference_seed_v0.sql.

-- ---- V9__product_special_discount_percent.sql ----
-- Daily special discount shown in the storefront (e.g. percent off the featured product for that calendar day).

ALTER TABLE product_special
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0;

-- discount_percent for calendar specials is seeded in R__reference_seed_v0.sql.

-- ---- V10__bakery_image_url.sql ----
-- Nullable hero/list image per bakery; full HTTPS URL in Spaces under /bakery/ (aligned with V12 + R__ seed).
ALTER TABLE bakery
    ADD COLUMN IF NOT EXISTS bakery_image_url VARCHAR(2048);


-- ---- V11__reviews_and_bakery_images.sql ----
-- Legacy V11 review INSERTs omitted (empty schema). Bakery hero URLs: R__reference_seed_v0.sql.

-- ---- V12__align_image_urls_with_spaces_rename.sql ----
-- Original migration updated bakery and product Spaces URLs; same literals are in R__ seed INSERTs.

-- ---- V13__add_product_images_for_renamed_products.sql ----
-- Remaining product image URLs merged into R__ product INSERTs.

-- ---- V14__link_reviews_to_orders.sql ----
-- Allow review moderation to complete the exact delivered order that generated the review.
ALTER TABLE review
    ADD COLUMN IF NOT EXISTS order_id UUID REFERENCES "order"(order_id);

CREATE INDEX IF NOT EXISTS idx_review_order ON review (order_id);



-- ---- V18__add_non_null_bakery_id_to_reviews.sql ----
-- V18: Make reviews bakery-owned with non-null bakery_id.
-- Backfill bakery_id from order first, then from customer's most recent matching order.

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS bakery_id INTEGER;

-- Primary backfill: where order_id exists, copy bakery directly.
UPDATE review r
SET bakery_id = o.bakery_id
FROM "order" o
WHERE r.order_id = o.order_id
  AND r.bakery_id IS NULL;

-- Secondary backfill: infer from customer+product to most recent matching order.
-- Safe when re-run / out-of-order: avoid duplicate (customer_id, order_id) (see V36 unique index).
WITH matched AS (
    SELECT r.review_id,
           r.customer_id,
           x.order_id,
           x.bakery_id,
           ROW_NUMBER() OVER (
               PARTITION BY r.customer_id, x.order_id
               ORDER BY r.review_submitted_date NULLS LAST, r.review_id
           ) AS rn
    FROM review r
             JOIN LATERAL (
        SELECT o.order_id, o.bakery_id
        FROM "order" o
                 JOIN order_item oi ON oi.order_id = o.order_id
        WHERE o.customer_id = r.customer_id
          AND oi.product_id = r.product_id
        ORDER BY o.order_placed_datetime DESC
        LIMIT 1
        ) x ON TRUE
    WHERE r.bakery_id IS NULL
),
     picked AS (
         SELECT review_id, customer_id, order_id, bakery_id
         FROM matched
         WHERE rn = 1
     )
UPDATE review r
SET order_id = COALESCE(r.order_id, p.order_id),
    bakery_id = p.bakery_id
FROM picked p
WHERE r.review_id = p.review_id
  AND r.bakery_id IS NULL
  AND (
    COALESCE(r.order_id, p.order_id) IS NULL
        OR NOT EXISTS (SELECT 1
                       FROM review other
                       WHERE other.customer_id = p.customer_id
                         AND other.order_id = COALESCE(r.order_id, p.order_id)
                         AND other.review_id <> r.review_id)
    );

-- Final safety fallback: copy bakery from customer's most recent order only (do not mass-assign
-- order_id — that would duplicate (customer_id, order_id) for multiple product reviews per customer).
WITH customer_latest AS (
    SELECT DISTINCT ON (o.customer_id) o.customer_id,
                                        o.bakery_id
    FROM "order" o
    ORDER BY o.customer_id, o.order_placed_datetime DESC
)
UPDATE review r
SET bakery_id = cl.bakery_id
FROM customer_latest cl
WHERE r.customer_id = cl.customer_id
  AND r.bakery_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM review WHERE bakery_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot enforce NOT NULL on review.bakery_id; unresolved rows remain.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'review_bakery_id_fkey'
    ) THEN
        ALTER TABLE review
            ADD CONSTRAINT review_bakery_id_fkey
            FOREIGN KEY (bakery_id) REFERENCES bakery(bakery_id);
    END IF;
END $$;

ALTER TABLE review
    ALTER COLUMN bakery_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_review_bakery ON review (bakery_id);



-- ---- V19__tax_rates_and_order_tax_columns.sql ----
CREATE TABLE IF NOT EXISTS tax_rate (
    province_name VARCHAR(80) PRIMARY KEY,
    tax_percent NUMERIC(5,3) NOT NULL
);

INSERT INTO tax_rate (province_name, tax_percent) VALUES
    ('Alberta', 5.000),
    ('British Columbia', 12.000),
    ('Manitoba', 12.000),
    ('New Brunswick', 15.000),
    ('Newfoundland and Labrador', 15.000),
    ('Northwest Territories', 5.000),
    ('Nova Scotia', 14.000),
    ('Nunavut', 5.000),
    ('Ontario', 13.000),
    ('Prince Edward Island', 15.000),
    ('Quebec', 14.975),
    ('Saskatchewan', 11.000),
    ('Yukon', 5.000)
ON CONFLICT (province_name) DO UPDATE
SET tax_percent = EXCLUDED.tax_percent;

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS order_tax_rate NUMERIC(5,3) NOT NULL DEFAULT 0;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS order_tax_amount NUMERIC(10,2) NOT NULL DEFAULT 0;

-- V19 backfill from address + tax_rate omitted: seeded orders set order_tax_rate / order_tax_amount in R__reference_seed_v0.sql.

-- ---- V20__unique_contact_identifiers.sql ----
-- Normalize optional business-phone blanks so uniqueness ignores "not provided" values.
UPDATE customer
SET customer_business_phone = NULL
WHERE customer_business_phone IS NOT NULL
  AND BTRIM(customer_business_phone) = '';

UPDATE employee
SET employee_business_phone = NULL
WHERE employee_business_phone IS NOT NULL
  AND BTRIM(employee_business_phone) = '';

-- Fail early with clear messages if existing data would violate the new uniqueness rules.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM "user"
        GROUP BY LOWER(BTRIM(username))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique username rule: duplicate usernames exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM "user"
        GROUP BY LOWER(BTRIM(user_email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique user email rule: duplicate user emails exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM customer
        GROUP BY LOWER(BTRIM(customer_email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique customer email rule: duplicate customer emails exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM employee
        GROUP BY LOWER(BTRIM(employee_work_email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique employee email rule: duplicate employee work emails exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM customer
        GROUP BY REGEXP_REPLACE(BTRIM(customer_phone), '\D', '', 'g')
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique customer phone rule: duplicate customer phone numbers exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM employee
        GROUP BY REGEXP_REPLACE(BTRIM(employee_phone), '\D', '', 'g')
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique employee phone rule: duplicate employee phone numbers exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM customer
        WHERE customer_business_phone IS NOT NULL
          AND BTRIM(customer_business_phone) <> ''
        GROUP BY REGEXP_REPLACE(BTRIM(customer_business_phone), '\D', '', 'g')
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique customer business phone rule: duplicate customer business phone numbers exist after normalization';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM employee
        WHERE employee_business_phone IS NOT NULL
          AND BTRIM(employee_business_phone) <> ''
        GROUP BY REGEXP_REPLACE(BTRIM(employee_business_phone), '\D', '', 'g')
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique employee business phone rule: duplicate employee business phone numbers exist after normalization';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_username_normalized
    ON "user" (LOWER(BTRIM(username)));

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_email_normalized
    ON "user" (LOWER(BTRIM(user_email)));

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_email_normalized
    ON customer (LOWER(BTRIM(customer_email)));

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_work_email_normalized
    ON employee (LOWER(BTRIM(employee_work_email)));

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_phone_normalized
    ON customer (REGEXP_REPLACE(BTRIM(customer_phone), '\D', '', 'g'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_phone_normalized
    ON employee (REGEXP_REPLACE(BTRIM(employee_phone), '\D', '', 'g'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_business_phone_normalized
    ON customer (REGEXP_REPLACE(BTRIM(customer_business_phone), '\D', '', 'g'))
    WHERE customer_business_phone IS NOT NULL
      AND BTRIM(customer_business_phone) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_business_phone_normalized
    ON employee (REGEXP_REPLACE(BTRIM(employee_business_phone), '\D', '', 'g'))
    WHERE employee_business_phone IS NOT NULL
      AND BTRIM(employee_business_phone) <> '';


-- ---- V21__add_password_reset_token.sql ----
CREATE TABLE password_reset_token (
                                      id          BIGSERIAL PRIMARY KEY,
                                      user_id     UUID NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
                                      token       VARCHAR(255) NOT NULL UNIQUE,
                                      expires_at  TIMESTAMPTZ NOT NULL,
                                      used        BOOLEAN NOT NULL DEFAULT FALSE,
                                      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token_token ON password_reset_token(token);
CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);

-- ---- V22__stripe_session_id.sql ----
ALTER TABLE payment ADD COLUMN IF NOT EXISTS stripe_session_id VARCHAR(255);


-- ---- V23__customer_nullable_guest_names.sql ----
-- Partial guest customers may omit legal name until they complete registration.
ALTER TABLE customer ALTER COLUMN customer_first_name DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN customer_last_name DROP NOT NULL;


-- ---- V25__order_drop_guest_contact_columns.sql ----
-- Guest contact is stored on customer; orders always reference customer_id.
ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_or_guest;

ALTER TABLE "order" DROP COLUMN IF EXISTS guest_name;
ALTER TABLE "order" DROP COLUMN IF EXISTS guest_email;
ALTER TABLE "order" DROP COLUMN IF EXISTS guest_phone;

ALTER TABLE "order" ADD CONSTRAINT chk_order_customer_id_not_null CHECK (customer_id IS NOT NULL);


-- ---- V26__restore_order_guest_contact_columns.sql ----
-- Undo V25: keep guest snapshot columns for clients (e.g. Svelte) and legacy constraint shape.
ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_id_not_null;

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_name VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_email VARCHAR(254);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(20);

ALTER TABLE "order" DROP CONSTRAINT IF EXISTS chk_order_customer_or_guest;
ALTER TABLE "order" ADD CONSTRAINT chk_order_customer_or_guest
    CHECK (customer_id IS NOT NULL OR (guest_name IS NOT NULL AND guest_email IS NOT NULL));


-- ---- V29__add_is_dietary_to_tag.sql ----
ALTER TABLE tag ADD COLUMN is_dietary BOOLEAN NOT NULL DEFAULT FALSE;

-- is_dietary flags for tags are set in R__reference_seed_v0.sql (INSERT tag …).

-- ---- V30__add_provider_columns_to_user.sql ----
ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- ---- V32__user_oauth_provider_unique.sql ----
-- One OAuth identity per provider (password accounts keep provider / provider_id null)
CREATE UNIQUE INDEX IF NOT EXISTS user_provider_provider_id_key
    ON "user" (provider, provider_id)
    WHERE provider IS NOT NULL AND provider_id IS NOT NULL;


-- ---- V33__review_moderation_rejection_reason.sql ----
ALTER TABLE review
    ADD COLUMN IF NOT EXISTS moderation_rejection_reason VARCHAR(500);


-- ---- V34__drop_review_approval_date.sql ----
-- Approval timestamp is redundant for this workflow (auto-approved path + staff status only).
-- Lifecycle remains on review_status (pending / approved / rejected).
ALTER TABLE review DROP COLUMN IF EXISTS review_approval_date;


-- ---- V35__restore_review_approval_date.sql ----
-- Restores staff / workflow approval timestamp (V34 dropped it; product still uses review_status).
ALTER TABLE review ADD COLUMN IF NOT EXISTS review_approval_date TIMESTAMP WITH TIME ZONE;

-- review_approval_date for approved rows is populated in R__reference_seed_v0.sql.

-- ---- V36__review_one_attempt_unique.sql ----
-- Enforce one review attempt per customer/product and per customer/order (race-safe).
-- Clean duplicates produced by concurrent submissions before creating unique indexes.
WITH ranked_product AS (
    SELECT review_id,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id, product_id
               ORDER BY review_submitted_date, review_id
           ) AS rn
    FROM review
    WHERE order_id IS NULL
),
ranked_order AS (
    SELECT review_id,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id, order_id
               ORDER BY review_submitted_date, review_id
           ) AS rn
    FROM review
    WHERE order_id IS NOT NULL
)
DELETE FROM review r
WHERE r.review_id IN (
    SELECT review_id FROM ranked_product WHERE rn > 1
    UNION ALL
    SELECT review_id FROM ranked_order WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_customer_product_attempt
    ON review (customer_id, product_id)
    WHERE order_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_review_customer_order_attempt
    ON review (customer_id, order_id)
    WHERE order_id IS NOT NULL;


-- ---- V37__employee_customer_link.sql ----
-- One-to-one link between an employee profile and a customer profile (stable ids; discount eligibility when both users active).
CREATE TABLE employee_customer_link (
    link_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES employee (employee_id) ON DELETE CASCADE,
    customer_id     UUID NOT NULL REFERENCES customer (customer_id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ecl_employee UNIQUE (employee_id),
    CONSTRAINT uk_ecl_customer UNIQUE (customer_id)
);

CREATE INDEX idx_ecl_employee ON employee_customer_link (employee_id);
CREATE INDEX idx_ecl_customer ON employee_customer_link (customer_id);


-- ---- V38__order_discount_breakdown.sql ----
ALTER TABLE "order"
    ADD COLUMN IF NOT EXISTS order_special_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS order_tier_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS order_employee_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

-- V38 backfill from order_discount omitted: R__ seed sets discount breakdown columns explicitly.

-- ---- V39__allow_null_bakeries_on_reviews.sql ----
ALTER TABLE review ALTER COLUMN bakery_id DROP NOT NULL;

-- ---- V40__allow_null_product_on_review.sql ----
ALTER TABLE review ALTER COLUMN product_id DROP NOT NULL;

-- ---- V41__drop_review_one_attempt_unique_indexes.sql ----
-- Allow multiple review submissions per customer/product and per customer/order
-- (e.g. retries after moderation, anonymous flows). Replaces V36 partial unique indexes.
DROP INDEX IF EXISTS uq_review_customer_product_attempt;
DROP INDEX IF EXISTS uq_review_customer_order_attempt;


-- ---- V42__websocket_chat.sql ----
CREATE TABLE IF NOT EXISTS staff_conversation (
    conversation_id  SERIAL PRIMARY KEY,
    user_a_id        UUID NOT NULL REFERENCES "user"(user_id),
    user_b_id        UUID NOT NULL REFERENCES "user"(user_id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);

CREATE TABLE IF NOT EXISTS staff_message (
    message_id       SERIAL PRIMARY KEY,
    conversation_id  INTEGER NOT NULL REFERENCES staff_conversation(conversation_id),
    sender_id        UUID NOT NULL REFERENCES "user"(user_id),
    message_text     VARCHAR(2000),
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read          BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_staff_message_convo ON staff_message(conversation_id, sent_at);


