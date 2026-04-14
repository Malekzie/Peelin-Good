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