ALTER TABLE orders ADD COLUMN IF NOT EXISTS username VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_orders_username ON orders (username);