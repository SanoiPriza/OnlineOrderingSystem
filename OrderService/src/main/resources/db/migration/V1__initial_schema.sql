CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL       PRIMARY KEY,
    customer_name           VARCHAR(255)    NOT NULL,
    product_id              VARCHAR(255)    NOT NULL,
    quantity                INTEGER         NOT NULL,
    total_price             NUMERIC(19, 4)  NOT NULL,
    status                  VARCHAR(50)     NOT NULL,
    status_message          VARCHAR(1000),
    payment_method          VARCHAR(100),
    amount                  NUMERIC(19, 4),
    currency                VARCHAR(10),
    payment_transaction_id  VARCHAR(255),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_name         ON orders (customer_name);
CREATE INDEX IF NOT EXISTS idx_orders_status                ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_product_id            ON orders (product_id);
CREATE INDEX IF NOT EXISTS idx_orders_payment_transaction_id ON orders (payment_transaction_id);
