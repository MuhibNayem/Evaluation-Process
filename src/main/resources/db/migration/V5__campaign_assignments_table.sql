-- ============================================================================
-- V5: First-class campaign assignments table (dual-write foundation)
-- ============================================================================

CREATE TABLE IF NOT EXISTS campaign_assignments (
    id              VARCHAR(64)     PRIMARY KEY,
    campaign_id     VARCHAR(36)     NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    evaluator_id    VARCHAR(255)    NOT NULL,
    evaluatee_id    VARCHAR(255)    NOT NULL,
    evaluator_role  VARCHAR(30)     NOT NULL,
    completed       BOOLEAN         NOT NULL DEFAULT FALSE,
    evaluation_id   VARCHAR(36),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_campaign_assignment_tuple UNIQUE (campaign_id, evaluator_id, evaluatee_id, evaluator_role)
);

CREATE INDEX IF NOT EXISTS idx_campaign_assignments_campaign ON campaign_assignments(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_evaluator ON campaign_assignments(evaluator_id);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_evaluatee ON campaign_assignments(evaluatee_id);
CREATE INDEX IF NOT EXISTS idx_campaign_assignments_completed ON campaign_assignments(completed);

-- Backfill from legacy campaigns.assignments_json for compatibility during transition.
INSERT INTO campaign_assignments (
    id,
    campaign_id,
    evaluator_id,
    evaluatee_id,
    evaluator_role,
    completed,
    evaluation_id,
    created_at,
    updated_at
)
SELECT
    a->>'id'                                         AS id,
    c.id                                             AS campaign_id,
    a->>'evaluatorId'                                AS evaluator_id,
    a->>'evaluateeId'                                AS evaluatee_id,
    a->>'evaluatorRole'                              AS evaluator_role,
    COALESCE((a->>'completed')::boolean, FALSE)      AS completed,
    NULLIF(a->>'evaluationId', '')                   AS evaluation_id,
    c.created_at                                     AS created_at,
    c.updated_at                                     AS updated_at
FROM campaigns c
CROSS JOIN LATERAL jsonb_array_elements(COALESCE(c.assignments_json, '[]')::jsonb) AS a
WHERE COALESCE(a->>'id', '') <> ''
  AND COALESCE(a->>'evaluatorId', '') <> ''
  AND COALESCE(a->>'evaluateeId', '') <> ''
  AND COALESCE(a->>'evaluatorRole', '') <> ''
ON CONFLICT (id) DO NOTHING;
