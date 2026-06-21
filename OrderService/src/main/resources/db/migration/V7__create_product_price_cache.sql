CREATE TABLE IF NOT EXISTS product_price_cache (
    product_id  VARCHAR(255)    PRIMARY KEY,
    name        VARCHAR(255),
    price       NUMERIC(19, 4),
    updated_at  TIMESTAMP
);
