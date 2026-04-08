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
