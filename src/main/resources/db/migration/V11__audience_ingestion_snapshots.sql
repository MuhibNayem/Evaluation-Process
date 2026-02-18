-- ============================================================================
-- V11: Audience ingestion snapshots for deterministic replay
-- ============================================================================

CREATE TABLE IF NOT EXISTS audience_ingestion_snapshots (
    run_id               VARCHAR(80)     PRIMARY KEY REFERENCES audience_ingestion_runs(id) ON DELETE CASCADE,
    tenant_id            VARCHAR(64)     NOT NULL,
    source_type          VARCHAR(80)     NOT NULL,
    mapping_profile_id   BIGINT,
    source_config_json   TEXT            NOT NULL,
    source_records_json  TEXT            NOT NULL,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audience_ing_snapshots_tenant ON audience_ingestion_snapshots(tenant_id);
