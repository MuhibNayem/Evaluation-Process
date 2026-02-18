-- ============================================================================
-- V13: Rule control plane (Phase 4 + Phase 5 foundation)
-- ============================================================================

CREATE TABLE IF NOT EXISTS assignment_rule_definitions (
    id                 BIGSERIAL       PRIMARY KEY,
    tenant_id          VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name               VARCHAR(255)    NOT NULL,
    description        TEXT,
    semantic_version   VARCHAR(20)     NOT NULL,
    status             VARCHAR(20)     NOT NULL, -- DRAFT, PUBLISHED, DEPRECATED
    rule_type          VARCHAR(60)     NOT NULL,
    rule_config_json   TEXT            NOT NULL,
    created_by         VARCHAR(255)    NOT NULL,
    created_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at       TIMESTAMPTZ,
    CONSTRAINT uq_rule_definition_semver UNIQUE (tenant_id, name, semantic_version)
);

CREATE INDEX IF NOT EXISTS idx_rule_definitions_tenant ON assignment_rule_definitions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_rule_definitions_status ON assignment_rule_definitions(status);
CREATE INDEX IF NOT EXISTS idx_rule_definitions_tenant_name ON assignment_rule_definitions(tenant_id, name);

CREATE TABLE IF NOT EXISTS assignment_rule_publish_requests (
    id                  BIGSERIAL       PRIMARY KEY,
    rule_definition_id  BIGINT          NOT NULL REFERENCES assignment_rule_definitions(id) ON DELETE CASCADE,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    status              VARCHAR(20)     NOT NULL, -- PENDING, APPROVED, REJECTED
    reason_code         VARCHAR(80),
    comment             TEXT,
    requested_by        VARCHAR(255)    NOT NULL,
    requested_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    decided_by          VARCHAR(255),
    decided_at          TIMESTAMPTZ,
    decision_comment    TEXT
);

CREATE INDEX IF NOT EXISTS idx_rule_publish_requests_rule ON assignment_rule_publish_requests(rule_definition_id);
CREATE INDEX IF NOT EXISTS idx_rule_publish_requests_tenant_status ON assignment_rule_publish_requests(tenant_id, status);

CREATE TABLE IF NOT EXISTS admin_action_audit_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor               VARCHAR(255)    NOT NULL,
    action              VARCHAR(120)    NOT NULL,
    aggregate_type      VARCHAR(80)     NOT NULL,
    aggregate_id        VARCHAR(120)    NOT NULL,
    reason_code         VARCHAR(80),
    comment             TEXT,
    payload_json        TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_tenant_created ON admin_action_audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_action ON admin_action_audit_logs(action);
