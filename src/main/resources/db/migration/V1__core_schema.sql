-- ============================================================================
-- V1: Core schema for evaluation service
-- Tables: templates, campaigns, evaluations
-- ============================================================================

-- Templates
CREATE TABLE IF NOT EXISTS templates (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    current_version INT             NOT NULL DEFAULT 1,
    scoring_method  VARCHAR(30)     DEFAULT 'WEIGHTED_AVERAGE',
    custom_formula  TEXT,
    sections_json   TEXT,
    created_by      VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_status ON templates(status);
CREATE INDEX idx_templates_category ON templates(category);
CREATE INDEX idx_templates_created_by ON templates(created_by);

-- Campaigns
CREATE TABLE IF NOT EXISTS campaigns (
    id                   VARCHAR(36)     PRIMARY KEY,
    name                 VARCHAR(255)    NOT NULL,
    description          TEXT,
    template_id          VARCHAR(36)     NOT NULL REFERENCES templates(id),
    template_version     INT             NOT NULL DEFAULT 1,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    start_date           TIMESTAMPTZ     NOT NULL,
    end_date             TIMESTAMPTZ     NOT NULL,
    scoring_method       VARCHAR(30)     DEFAULT 'WEIGHTED_AVERAGE',
    anonymous_mode       BOOLEAN         NOT NULL DEFAULT FALSE,
    anonymous_roles_json TEXT,
    minimum_respondents  INT             NOT NULL DEFAULT 1,
    assignments_json     TEXT,
    created_by           VARCHAR(255)    NOT NULL,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_campaign_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_template ON campaigns(template_id);
CREATE INDEX idx_campaigns_dates ON campaigns(start_date, end_date);
CREATE INDEX idx_campaigns_created_by ON campaigns(created_by);

-- Evaluations
CREATE TABLE IF NOT EXISTS evaluations (
    id                  VARCHAR(36)     PRIMARY KEY,
    campaign_id         VARCHAR(36)     NOT NULL REFERENCES campaigns(id),
    assignment_id       VARCHAR(36)     NOT NULL,
    evaluator_id        VARCHAR(255)    NOT NULL,
    evaluatee_id        VARCHAR(255)    NOT NULL,
    template_id         VARCHAR(36)     NOT NULL REFERENCES templates(id),
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    answers_json        TEXT,
    total_score         DOUBLE PRECISION,
    section_scores_json TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMPTZ,

    CONSTRAINT uk_evaluation_assignment UNIQUE (assignment_id)
);

CREATE INDEX idx_evaluations_campaign ON evaluations(campaign_id);
CREATE INDEX idx_evaluations_evaluator ON evaluations(evaluator_id);
CREATE INDEX idx_evaluations_evaluatee ON evaluations(evaluatee_id);
CREATE INDEX idx_evaluations_status ON evaluations(status);
CREATE INDEX idx_evaluations_submitted ON evaluations(submitted_at);
