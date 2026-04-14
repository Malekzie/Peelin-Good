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
