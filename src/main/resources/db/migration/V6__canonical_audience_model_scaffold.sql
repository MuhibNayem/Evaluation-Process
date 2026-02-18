-- ============================================================================
-- V6: Canonical audience model scaffolding (Phase 3 foundation)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tenants (
    id              VARCHAR(64)     PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(100)    NOT NULL UNIQUE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audience_persons (
    id              VARCHAR(128)    PRIMARY KEY,
    tenant_id       VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    external_ref    VARCHAR(255),
    display_name    VARCHAR(255),
    email           VARCHAR(320),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audience_persons_tenant ON audience_persons(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_persons_external_ref ON audience_persons(external_ref);

CREATE TABLE IF NOT EXISTS audience_groups (
    id              VARCHAR(128)    PRIMARY KEY,
    tenant_id       VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    group_type      VARCHAR(100)    NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    external_ref    VARCHAR(255),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audience_groups_tenant ON audience_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_groups_type ON audience_groups(group_type);

CREATE TABLE IF NOT EXISTS audience_memberships (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    person_id           VARCHAR(128)    NOT NULL REFERENCES audience_persons(id) ON DELETE CASCADE,
    group_id            VARCHAR(128)    NOT NULL REFERENCES audience_groups(id) ON DELETE CASCADE,
    membership_role     VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from          TIMESTAMPTZ,
    valid_to            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audience_membership UNIQUE (tenant_id, person_id, group_id, membership_role)
);

CREATE INDEX IF NOT EXISTS idx_audience_memberships_tenant ON audience_memberships(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_memberships_person ON audience_memberships(person_id);
CREATE INDEX IF NOT EXISTS idx_audience_memberships_group ON audience_memberships(group_id);

CREATE TABLE IF NOT EXISTS audience_relations (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    source_person_id    VARCHAR(128)    NOT NULL REFERENCES audience_persons(id) ON DELETE CASCADE,
    target_person_id    VARCHAR(128)    NOT NULL REFERENCES audience_persons(id) ON DELETE CASCADE,
    relation_type       VARCHAR(100)    NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from          TIMESTAMPTZ,
    valid_to            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audience_relation UNIQUE (tenant_id, source_person_id, target_person_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_audience_relations_tenant ON audience_relations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_relations_source ON audience_relations(source_person_id);
CREATE INDEX IF NOT EXISTS idx_audience_relations_target ON audience_relations(target_person_id);
CREATE INDEX IF NOT EXISTS idx_audience_relations_type ON audience_relations(relation_type);

CREATE TABLE IF NOT EXISTS audience_attributes (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subject_type        VARCHAR(30)     NOT NULL, -- PERSON | GROUP
    subject_id          VARCHAR(128)    NOT NULL,
    attribute_key       VARCHAR(150)    NOT NULL,
    attribute_value     TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audience_attribute UNIQUE (tenant_id, subject_type, subject_id, attribute_key)
);

CREATE INDEX IF NOT EXISTS idx_audience_attributes_tenant ON audience_attributes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audience_attributes_subject ON audience_attributes(subject_type, subject_id);
CREATE INDEX IF NOT EXISTS idx_audience_attributes_key ON audience_attributes(attribute_key);
