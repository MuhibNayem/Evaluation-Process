-- ============================================================================
-- V10: Audience mapping profile lifecycle audit events
-- ============================================================================

CREATE TABLE IF NOT EXISTS audience_mapping_profile_events (
    id                  BIGSERIAL       PRIMARY KEY,
    profile_id          BIGINT          NOT NULL REFERENCES audience_mapping_profiles(id) ON DELETE CASCADE,
    tenant_id           VARCHAR(64)     NOT NULL,
    event_type          VARCHAR(40)     NOT NULL,
    actor               VARCHAR(120)    NOT NULL,
    event_payload_json  TEXT            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_amp_events_profile ON audience_mapping_profile_events(profile_id);
CREATE INDEX IF NOT EXISTS idx_amp_events_tenant ON audience_mapping_profile_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_amp_events_type ON audience_mapping_profile_events(event_type);
CREATE INDEX IF NOT EXISTS idx_amp_events_created ON audience_mapping_profile_events(created_at);
