-- ============================================================================
-- V4: Dynamic audience source and assignment rule config for campaigns
-- ============================================================================

ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS audience_source_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS audience_source_config_json TEXT,
    ADD COLUMN IF NOT EXISTS assignment_rule_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS assignment_rule_config_json TEXT;

CREATE INDEX IF NOT EXISTS idx_campaigns_audience_source_type ON campaigns(audience_source_type);
CREATE INDEX IF NOT EXISTS idx_campaigns_assignment_rule_type ON campaigns(assignment_rule_type);
