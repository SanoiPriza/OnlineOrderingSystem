CREATE TABLE IF NOT EXISTS products (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    price           NUMERIC(19, 4)  NOT NULL,
    stock_quantity  INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
