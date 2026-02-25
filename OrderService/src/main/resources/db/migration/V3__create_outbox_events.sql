CREATE TABLE IF NOT EXISTS outbox_events (
    id            BIGSERIAL       PRIMARY KEY,
    event_type    VARCHAR(50)     NOT NULL,
    status        VARCHAR(50)     NOT NULL,
    payload       TEXT            NOT NULL,
    order_id      BIGINT          NOT NULL,
    retry_count   INTEGER         NOT NULL DEFAULT 0,
    created_at    TIMESTAMP       NOT NULL,
    processed_at  TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status     ON outbox_events (status);
CREATE INDEX IF NOT EXISTS idx_outbox_events_order_id   ON outbox_events (order_id);
