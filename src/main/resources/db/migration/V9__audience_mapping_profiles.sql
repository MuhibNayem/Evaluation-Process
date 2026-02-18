-- ============================================================================
-- V9: Audience source-to-canonical mapping profiles
-- ============================================================================

CREATE TABLE IF NOT EXISTS audience_mapping_profiles (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(120)    NOT NULL,
    source_type     VARCHAR(40)     NOT NULL,
    mappings_json   TEXT            NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audience_mapping_profile UNIQUE (tenant_id, source_type, name)
);

CREATE INDEX IF NOT EXISTS idx_audience_mapping_profile_tenant ON audience_mapping_profiles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_mapping_profile_source ON audience_mapping_profiles(source_type);
CREATE INDEX IF NOT EXISTS idx_audience_mapping_profile_active ON audience_mapping_profiles(active);
