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
