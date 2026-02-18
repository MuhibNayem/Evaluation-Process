-- ============================================================================
-- V7: Audience ingestion run tracking
-- ============================================================================

CREATE TABLE IF NOT EXISTS audience_ingestion_runs (
    id                  VARCHAR(80)     PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    source_type         VARCHAR(80)     NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    dry_run             BOOLEAN         NOT NULL DEFAULT FALSE,
    processed_records   INT             NOT NULL DEFAULT 0,
    rejected_records    INT             NOT NULL DEFAULT 0,
    error_message       TEXT,
    started_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    ended_at            TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_audience_ingestion_runs_tenant ON audience_ingestion_runs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_ingestion_runs_status ON audience_ingestion_runs(status);
CREATE INDEX IF NOT EXISTS idx_audience_ingestion_runs_started ON audience_ingestion_runs(started_at);
