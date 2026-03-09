ALTER TABLE customer
DROP
COLUMN customer_middle_initial;

ALTER TABLE customer
    ADD customer_middle_initial CHAR(2(2);

CREATE INDEX idx_order_number ON "order" (order_number);