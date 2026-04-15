-- Chat auto-routing: staff specialties + reserved system user
-- Spec: Workshop06/docs/superpowers/specs/2026-04-15-chat-routing-design.md

-- 1. Specialty table
CREATE TABLE employee_specialty (
    user_id  UUID         NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    category VARCHAR(64)  NOT NULL,
    PRIMARY KEY (user_id, category)
);

CREATE INDEX idx_employee_specialty_category ON employee_specialty(category);

-- 2. Seed every existing admin/employee with all four categories.
INSERT INTO employee_specialty (user_id, category)
SELECT u.user_id, cat
FROM "user" u
CROSS JOIN (VALUES
    ('general'),
    ('order_issue'),
    ('account_help'),
    ('feedback')
) AS c(cat)
WHERE u.user_role IN ('employee', 'admin')
ON CONFLICT DO NOTHING;

-- 3. Reserved "system" user for assignment / no-staff-online system messages.
INSERT INTO "user" (user_id, username, user_email, user_password_hash, user_role, is_active, user_created_at, photo_approval_pending)
VALUES (
    '00000000-0000-0000-0000-000000000001'::uuid,
    '__system__',
    'system@peelin.internal',
    '!DISABLED!',
    'employee',
    false,
    now(),
    false
)
ON CONFLICT (user_id) DO NOTHING;
