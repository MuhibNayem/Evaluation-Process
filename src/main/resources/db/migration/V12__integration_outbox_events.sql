-- ============================================================================
-- V12: Integration outbox for guaranteed external event delivery
-- ============================================================================

CREATE TABLE IF NOT EXISTS integration_outbox_events (
    id              BIGSERIAL       PRIMARY KEY,
    aggregate_type  VARCHAR(80)     NOT NULL,
    aggregate_id    VARCHAR(120)    NOT NULL,
    event_type      VARCHAR(120)    NOT NULL,
    payload_json    TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempt_count   INT             NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt ON integration_outbox_events(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON integration_outbox_events(aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_created ON integration_outbox_events(created_at);
