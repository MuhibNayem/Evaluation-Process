-- ============================================================================
-- V15: FES parity schema scaffold (Phase 1, schema-only)
-- Adds additive tables/columns required by PDF parity modules.
-- No runtime behavior changes are introduced by this migration.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Campaign lifecycle additive metadata
-- ----------------------------------------------------------------------------
ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS results_published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_campaigns_locked ON campaigns(locked);

-- ----------------------------------------------------------------------------
-- Campaign lifecycle events (auditable transitions)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_lifecycle_events (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    from_status         VARCHAR(40),
    to_status           VARCHAR(40)     NOT NULL,
    action              VARCHAR(60)     NOT NULL,
    actor               VARCHAR(255)    NOT NULL,
    reason              TEXT,
    metadata_json       TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_campaign_lifecycle_events_campaign ON campaign_lifecycle_events(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_lifecycle_events_created ON campaign_lifecycle_events(created_at DESC);

-- ----------------------------------------------------------------------------
-- Campaign steps and windows
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_steps (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    step_type           VARCHAR(30)     NOT NULL, -- STUDENT | PEER | SELF | DEPARTMENT
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    display_order       INT             NOT NULL,
    open_at             TIMESTAMPTZ,
    close_at            TIMESTAMPTZ,
    late_allowed        BOOLEAN         NOT NULL DEFAULT FALSE,
    late_days           INT             NOT NULL DEFAULT 0,
    instructions        TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_campaign_steps_unique_step UNIQUE (campaign_id, step_type),
    CONSTRAINT chk_campaign_steps_late_days_non_negative CHECK (late_days >= 0),
    CONSTRAINT chk_campaign_steps_window_valid CHECK (open_at IS NULL OR close_at IS NULL OR close_at > open_at)
);

CREATE INDEX IF NOT EXISTS idx_campaign_steps_campaign ON campaign_steps(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_steps_enabled ON campaign_steps(enabled);

-- ----------------------------------------------------------------------------
-- Categories and default campaign weights
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_categories (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name                VARCHAR(255)    NOT NULL,
    weight_percent      NUMERIC(6,2)    NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_campaign_categories_name UNIQUE (campaign_id, name),
    CONSTRAINT chk_campaign_categories_weight_percent CHECK (weight_percent >= 0 AND weight_percent <= 100)
);

CREATE INDEX IF NOT EXISTS idx_campaign_categories_campaign ON campaign_categories(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_categories_active ON campaign_categories(active);

-- ----------------------------------------------------------------------------
-- Designation-specific weight overrides
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS designation_weight_mappings (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    designation         VARCHAR(120)    NOT NULL,
    category_id         BIGINT          NOT NULL REFERENCES campaign_categories(id) ON DELETE CASCADE,
    weight_percent      NUMERIC(6,2)    NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_designation_category_weight UNIQUE (campaign_id, designation, category_id),
    CONSTRAINT chk_designation_weight_percent CHECK (weight_percent >= 0 AND weight_percent <= 100)
);

CREATE INDEX IF NOT EXISTS idx_designation_weight_campaign ON designation_weight_mappings(campaign_id);
CREATE INDEX IF NOT EXISTS idx_designation_weight_designation ON designation_weight_mappings(designation);

-- ----------------------------------------------------------------------------
-- Campaign rating scales
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_rating_scales (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name                VARCHAR(120)    NOT NULL DEFAULT 'Default',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_campaign_rating_scale_name UNIQUE (campaign_id, name)
);

CREATE TABLE IF NOT EXISTS campaign_rating_scale_items (
    id                  BIGSERIAL       PRIMARY KEY,
    rating_scale_id     BIGINT          NOT NULL REFERENCES campaign_rating_scales(id) ON DELETE CASCADE,
    label               VARCHAR(120)    NOT NULL,
    numeric_value       NUMERIC(10,2)   NOT NULL,
    display_order       INT             NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rating_item_value UNIQUE (rating_scale_id, numeric_value),
    CONSTRAINT uq_rating_item_order UNIQUE (rating_scale_id, display_order)
);

CREATE INDEX IF NOT EXISTS idx_rating_scales_campaign ON campaign_rating_scales(campaign_id);
CREATE INDEX IF NOT EXISTS idx_rating_scale_items_scale ON campaign_rating_scale_items(rating_scale_id);

-- ----------------------------------------------------------------------------
-- Question bank and versions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS question_bank_sets (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(64)     REFERENCES tenants(id) ON DELETE SET NULL,
    name                VARCHAR(255)    NOT NULL,
    version_tag         VARCHAR(60),
    owner               VARCHAR(255),
    status              VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_question_bank_sets_tenant ON question_bank_sets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_sets_status ON question_bank_sets(status);

CREATE TABLE IF NOT EXISTS question_bank_items (
    id                  BIGSERIAL       PRIMARY KEY,
    set_id              BIGINT          NOT NULL REFERENCES question_bank_sets(id) ON DELETE CASCADE,
    stable_key          VARCHAR(120)    NOT NULL, -- logical question key across versions
    context_type        VARCHAR(80),
    category_name       VARCHAR(255),
    default_type        VARCHAR(40)     NOT NULL,
    default_marks       NUMERIC(10,2)   NOT NULL DEFAULT 0,
    active_version_no   INT             NOT NULL DEFAULT 1,
    status              VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_question_bank_item_key UNIQUE (set_id, stable_key)
);

CREATE INDEX IF NOT EXISTS idx_question_bank_items_set ON question_bank_items(set_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_items_status ON question_bank_items(status);

CREATE TABLE IF NOT EXISTS question_bank_item_versions (
    id                  BIGSERIAL       PRIMARY KEY,
    question_item_id    BIGINT          NOT NULL REFERENCES question_bank_items(id) ON DELETE CASCADE,
    version_no          INT             NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT', -- DRAFT | ACTIVE | RETIRED
    change_summary      TEXT,
    question_text       TEXT            NOT NULL,
    question_type       VARCHAR(40)     NOT NULL,
    marks               NUMERIC(10,2)   NOT NULL DEFAULT 0,
    remarks_mandatory   BOOLEAN         NOT NULL DEFAULT FALSE,
    metadata_json       TEXT,
    effective_from      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_question_item_version UNIQUE (question_item_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_question_versions_item ON question_bank_item_versions(question_item_id);
CREATE INDEX IF NOT EXISTS idx_question_versions_status ON question_bank_item_versions(status);

-- ----------------------------------------------------------------------------
-- Assignment metadata extension for FES context
-- ----------------------------------------------------------------------------
ALTER TABLE campaign_assignments
    ADD COLUMN IF NOT EXISTS step_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS section_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS faculty_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS anonymity_mode VARCHAR(30),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_campaign_assignments_step_type ON campaign_assignments(step_type);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_section_id ON campaign_assignments(section_id);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_faculty_id ON campaign_assignments(faculty_id);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_status ON campaign_assignments(status);

-- ----------------------------------------------------------------------------
-- Notification rule/template/delivery scaffolding
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_rules (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    rule_code           VARCHAR(80)     NOT NULL,
    trigger_type        VARCHAR(80)     NOT NULL,
    audience            VARCHAR(120)    NOT NULL,
    channel             VARCHAR(40)     NOT NULL,
    schedule_expr       VARCHAR(255),
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    config_json         TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_rule_code UNIQUE (campaign_id, rule_code)
);

CREATE INDEX IF NOT EXISTS idx_notification_rules_campaign ON notification_rules(campaign_id);
CREATE INDEX IF NOT EXISTS idx_notification_rules_enabled ON notification_rules(enabled);

CREATE TABLE IF NOT EXISTS notification_templates (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     REFERENCES campaigns(id) ON DELETE CASCADE,
    template_code       VARCHAR(120)    NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    channel             VARCHAR(40)     NOT NULL,
    subject             TEXT,
    body                TEXT            NOT NULL,
    required_variables_json TEXT,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_template_code UNIQUE (campaign_id, template_code)
);

CREATE INDEX IF NOT EXISTS idx_notification_templates_campaign ON notification_templates(campaign_id);
CREATE INDEX IF NOT EXISTS idx_notification_templates_status ON notification_templates(status);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id                  BIGSERIAL       PRIMARY KEY,
    campaign_id         VARCHAR(36)     REFERENCES campaigns(id) ON DELETE SET NULL,
    rule_id             BIGINT          REFERENCES notification_rules(id) ON DELETE SET NULL,
    template_id         BIGINT          REFERENCES notification_templates(id) ON DELETE SET NULL,
    recipient           VARCHAR(255)    NOT NULL,
    channel             VARCHAR(40)     NOT NULL,
    status              VARCHAR(30)     NOT NULL, -- SENT | FAILED | DELIVERED
    error_code          VARCHAR(120),
    error_message       TEXT,
    message_payload_json TEXT,
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_campaign ON notification_deliveries(campaign_id);
CREATE INDEX IF NOT EXISTS idx_notification_deliveries_rule ON notification_deliveries(rule_id);
CREATE INDEX IF NOT EXISTS idx_notification_deliveries_status ON notification_deliveries(status);
CREATE INDEX IF NOT EXISTS idx_notification_deliveries_created ON notification_deliveries(created_at DESC);

