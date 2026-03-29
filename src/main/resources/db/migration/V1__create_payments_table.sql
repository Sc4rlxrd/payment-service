CREATE TABLE payments
(
    id         UUID PRIMARY KEY,
    order_id   UUID           NOT NULL,
    amount     NUMERIC(10, 2) NOT NULL,
    status     VARCHAR(50)    NOT NULL,
    created_at TIMESTAMP      NOT NULL
);