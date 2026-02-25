CREATE TABLE IF NOT EXISTS payments (
    id                      BIGSERIAL       PRIMARY KEY,
    order_id                VARCHAR(255)    NOT NULL,
    amount                  NUMERIC(19, 4),
    currency                VARCHAR(10),
    payment_method          VARCHAR(100),
    status                  VARCHAR(50)     NOT NULL,
    transaction_id          VARCHAR(255)    UNIQUE,
    gateway_transaction_id  VARCHAR(255),
    error_message           TEXT,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    completed_at            TIMESTAMP,
    refunded_at             TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id        ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status          ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id  ON payments (transaction_id);
