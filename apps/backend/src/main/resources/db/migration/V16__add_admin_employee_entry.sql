-- V16: Ensure seeded ADMIN user also has an employee record.
-- This keeps /api/v1/employee/me functional for admins and allows employee-scoped UI to render.

INSERT INTO employee (
    employee_id,
    uuid,
    user_id,
    address_id,
    bakery_id,
    employee_first_name,
    employee_middle_initial,
    employee_last_name,
    employee_position,
    employee_phone,
    employee_business_phone,
    employee_work_email
)
SELECT
    '30000000-0000-4000-8000-000000000011'::uuid AS employee_id,
    '30000000-0000-4000-8000-000000000011'::uuid AS uuid,
    '10000000-0000-4000-8000-000000000001'::uuid AS user_id, -- seeded admin user
    1 AS address_id,  -- matches bakery_id=1 address
    1 AS bakery_id,
    'John' AS employee_first_name,
    NULL AS employee_middle_initial,
    'Doe' AS employee_last_name,
    'Admin' AS employee_position,
    '(403) 555-3110' AS employee_phone,
    '(403) 555-4110' AS employee_business_phone,
    'john.doe@northharbourmail.ca' AS employee_work_email
WHERE NOT EXISTS (
    SELECT 1
    FROM employee e
    WHERE e.user_id = '10000000-0000-4000-8000-000000000001'::uuid
);

