-- ============================================================================
-- V8: Audience ingestion row-level rejection tracking
-- ============================================================================

CREATE TABLE IF NOT EXISTS audience_ingestion_rejections (
    id              BIGSERIAL       PRIMARY KEY,
    run_id          VARCHAR(80)     NOT NULL REFERENCES audience_ingestion_runs(id) ON DELETE CASCADE,
    tenant_id       VARCHAR(64)     NOT NULL,
    row_number      INT             NOT NULL,
    reason          TEXT            NOT NULL,
    row_data        TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audience_ing_rej_run ON audience_ingestion_rejections(run_id);
CREATE INDEX IF NOT EXISTS idx_audience_ing_rej_tenant ON audience_ingestion_rejections(tenant_id);
