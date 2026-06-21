CREATE TABLE IF NOT EXISTS outbox_events (
    id            BIGSERIAL       PRIMARY KEY,
    exchange      VARCHAR(255)    NOT NULL,
    routing_key   VARCHAR(255)    NOT NULL,
    status        VARCHAR(50)     NOT NULL,
    payload       TEXT            NOT NULL,
    retry_count   INTEGER         NOT NULL DEFAULT 0,
    created_at    TIMESTAMP       NOT NULL,
    processed_at  TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status     ON outbox_events (status);
