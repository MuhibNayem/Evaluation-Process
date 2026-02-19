-- Dev seed: section-based student->instructor evaluation scenario.
-- Safe to re-run (uses deterministic IDs and upserts).

BEGIN;

-- Ensure tenant exists.
INSERT INTO tenants (id, name, code, active, created_at, updated_at)
VALUES ('tenant-001', 'Default Tenant', 'TENANT001', TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    code = EXCLUDED.code,
    active = EXCLUDED.active,
    updated_at = NOW();

-- Ensure there is at least one published template.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM templates WHERE status = 'PUBLISHED') THEN
        INSERT INTO templates (
            id, name, description, category, status, current_version,
            scoring_method, custom_formula, sections_json,
            created_by, created_at, updated_at
        ) VALUES (
            'b4dd7d3f-20ac-4be0-b5bf-0d76f0ef1001',
            'Seeded Instructor Evaluation Template',
            'Auto-seeded for local development testing',
            'Engineering',
            'PUBLISHED',
            1,
            'WEIGHTED_AVERAGE',
            NULL,
            '[]',
            'seed-script',
            NOW(),
            NOW()
        );
    END IF;
END $$;

-- Pick one published template for seeded campaign.
WITH t AS (
    SELECT id, current_version
    FROM templates
    WHERE status = 'PUBLISHED'
    ORDER BY created_at DESC
    LIMIT 1
)
INSERT INTO campaigns (
    id,
    name,
    description,
    template_id,
    template_version,
    status,
    start_date,
    end_date,
    scoring_method,
    anonymous_mode,
    anonymous_roles_json,
    minimum_respondents,
    assignments_json,
    created_by,
    created_at,
    updated_at,
    audience_source_type,
    audience_source_config_json,
    assignment_rule_type,
    assignment_rule_config_json
)
SELECT
    '11111111-1111-1111-1111-111111111111',
    'Spring 2026 - Student Instructor Evaluation',
    'Seeded campaign for section-based testing',
    t.id,
    t.current_version,
    'ACTIVE',
    NOW() - INTERVAL '14 days',
    NOW() + INTERVAL '60 days',
    'WEIGHTED_AVERAGE',
    FALSE,
    '["EXTERNAL"]',
    1,
    '[]',
    'seed-script',
    NOW(),
    NOW(),
    'DIRECTORY_SNAPSHOT',
    '{"scope":"SPRING_2026"}',
    'ATTRIBUTE_MATCH',
    '{"matchAttribute":"section"}'
FROM t
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    template_id = EXCLUDED.template_id,
    template_version = EXCLUDED.template_version,
    status = EXCLUDED.status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    scoring_method = EXCLUDED.scoring_method,
    anonymous_mode = EXCLUDED.anonymous_mode,
    anonymous_roles_json = EXCLUDED.anonymous_roles_json,
    minimum_respondents = EXCLUDED.minimum_respondents,
    updated_at = NOW(),
    audience_source_type = EXCLUDED.audience_source_type,
    audience_source_config_json = EXCLUDED.audience_source_config_json,
    assignment_rule_type = EXCLUDED.assignment_rule_type,
    assignment_rule_config_json = EXCLUDED.assignment_rule_config_json;

-- People: 4 instructors + 13 students (one takes 4 courses).
INSERT INTO audience_persons (id, tenant_id, external_ref, display_name, email, active, created_at, updated_at)
VALUES
    ('ins-001', 'tenant-001', 'I001', 'Instructor 1', 'ins1@example.edu', TRUE, NOW(), NOW()),
    ('ins-002', 'tenant-001', 'I002', 'Instructor 2', 'ins2@example.edu', TRUE, NOW(), NOW()),
    ('ins-003', 'tenant-001', 'I003', 'Instructor 3', 'ins3@example.edu', TRUE, NOW(), NOW()),
    ('ins-004', 'tenant-001', 'I004', 'Instructor 4', 'ins4@example.edu', TRUE, NOW(), NOW()),
    ('stu-001', 'tenant-001', 'S001', 'Student 1', 'stu1@example.edu', TRUE, NOW(), NOW()),
    ('stu-002', 'tenant-001', 'S002', 'Student 2', 'stu2@example.edu', TRUE, NOW(), NOW()),
    ('stu-003', 'tenant-001', 'S003', 'Student 3', 'stu3@example.edu', TRUE, NOW(), NOW()),
    ('stu-004', 'tenant-001', 'S004', 'Student 4', 'stu4@example.edu', TRUE, NOW(), NOW()),
    ('stu-005', 'tenant-001', 'S005', 'Student 5', 'stu5@example.edu', TRUE, NOW(), NOW()),
    ('stu-006', 'tenant-001', 'S006', 'Student 6', 'stu6@example.edu', TRUE, NOW(), NOW()),
    ('stu-007', 'tenant-001', 'S007', 'Student 7', 'stu7@example.edu', TRUE, NOW(), NOW()),
    ('stu-008', 'tenant-001', 'S008', 'Student 8', 'stu8@example.edu', TRUE, NOW(), NOW()),
    ('stu-009', 'tenant-001', 'S009', 'Student 9', 'stu9@example.edu', TRUE, NOW(), NOW()),
    ('stu-010', 'tenant-001', 'S010', 'Student 10', 'stu10@example.edu', TRUE, NOW(), NOW()),
    ('stu-011', 'tenant-001', 'S011', 'Student 11', 'stu11@example.edu', TRUE, NOW(), NOW()),
    ('stu-012', 'tenant-001', 'S012', 'Student 12', 'stu12@example.edu', TRUE, NOW(), NOW()),
    ('stu-013', 'tenant-001', 'S013', 'Student 13', 'stu13@example.edu', TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET tenant_id = EXCLUDED.tenant_id,
    external_ref = EXCLUDED.external_ref,
    display_name = EXCLUDED.display_name,
    email = EXCLUDED.email,
    active = EXCLUDED.active,
    updated_at = NOW();

-- 9 sections across multiple courses.
INSERT INTO audience_groups (id, tenant_id, group_type, name, external_ref, active, created_at, updated_at)
VALUES
    ('CSE101-SEC01-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE101 Section 01 (SP26)', 'SEC-101-01-SP26', TRUE, NOW(), NOW()),
    ('CSE101-SEC02-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE101 Section 02 (SP26)', 'SEC-101-02-SP26', TRUE, NOW(), NOW()),
    ('CSE101-SEC03-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE101 Section 03 (SP26)', 'SEC-101-03-SP26', TRUE, NOW(), NOW()),
    ('CSE102-SEC01-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE102 Section 01 (SP26)', 'SEC-102-01-SP26', TRUE, NOW(), NOW()),
    ('CSE102-SEC02-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE102 Section 02 (SP26)', 'SEC-102-02-SP26', TRUE, NOW(), NOW()),
    ('CSE103-SEC01-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE103 Section 01 (SP26)', 'SEC-103-01-SP26', TRUE, NOW(), NOW()),
    ('CSE103-SEC02-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE103 Section 02 (SP26)', 'SEC-103-02-SP26', TRUE, NOW(), NOW()),
    ('CSE104-SEC01-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE104 Section 01 (SP26)', 'SEC-104-01-SP26', TRUE, NOW(), NOW()),
    ('CSE104-SEC02-SP26', 'tenant-001', 'COURSE_SECTION', 'CSE104 Section 02 (SP26)', 'SEC-104-02-SP26', TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET tenant_id = EXCLUDED.tenant_id,
    group_type = EXCLUDED.group_type,
    name = EXCLUDED.name,
    external_ref = EXCLUDED.external_ref,
    active = EXCLUDED.active,
    updated_at = NOW();

-- Memberships: instructors + students per section.
INSERT INTO audience_memberships (
    tenant_id, person_id, group_id, membership_role, active, valid_from, valid_to, created_at, updated_at
)
VALUES
    ('tenant-001', 'ins-001', 'CSE101-SEC01-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-002', 'CSE101-SEC02-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-003', 'CSE101-SEC03-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-003', 'CSE102-SEC01-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-004', 'CSE102-SEC02-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-002', 'CSE103-SEC01-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-003', 'CSE103-SEC02-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-004', 'CSE104-SEC01-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),
    ('tenant-001', 'ins-001', 'CSE104-SEC02-SP26', 'INSTRUCTOR', TRUE, NOW() - INTERVAL '30 days', NULL, NOW(), NOW()),

    ('tenant-001', 'stu-001', 'CSE101-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-001', 'CSE102-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-001', 'CSE103-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-001', 'CSE104-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),

    ('tenant-001', 'stu-002', 'CSE101-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-003', 'CSE101-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-004', 'CSE101-SEC03-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-005', 'CSE102-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-006', 'CSE102-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-007', 'CSE103-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-008', 'CSE103-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-009', 'CSE104-SEC01-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-010', 'CSE104-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-011', 'CSE102-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-012', 'CSE101-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW()),
    ('tenant-001', 'stu-013', 'CSE104-SEC02-SP26', 'STUDENT', TRUE, NOW() - INTERVAL '20 days', NULL, NOW(), NOW())
ON CONFLICT (tenant_id, person_id, group_id, membership_role) DO UPDATE
SET active = EXCLUDED.active,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to,
    updated_at = NOW();

-- Generate assignments: each student evaluates instructor of the same section.
WITH pairs AS (
    SELECT
        s.person_id AS evaluator_id,
        i.person_id AS evaluatee_id,
        s.group_id
    FROM audience_memberships s
    JOIN audience_memberships i
      ON i.tenant_id = s.tenant_id
     AND i.group_id = s.group_id
     AND i.membership_role = 'INSTRUCTOR'
     AND i.active = TRUE
    WHERE s.tenant_id = 'tenant-001'
      AND s.membership_role = 'STUDENT'
      AND s.active = TRUE
), generated AS (
    SELECT
        LOWER(
            SUBSTRING(md5('asg|' || evaluator_id || '|' || evaluatee_id || '|' || group_id), 1, 8) || '-' ||
            SUBSTRING(md5('asg|' || evaluator_id || '|' || evaluatee_id || '|' || group_id), 9, 4) || '-' ||
            SUBSTRING(md5('asg|' || evaluator_id || '|' || evaluatee_id || '|' || group_id), 13, 4) || '-' ||
            SUBSTRING(md5('asg|' || evaluator_id || '|' || evaluatee_id || '|' || group_id), 17, 4) || '-' ||
            SUBSTRING(md5('asg|' || evaluator_id || '|' || evaluatee_id || '|' || group_id), 21, 12)
        ) AS assignment_id,
        evaluator_id,
        evaluatee_id
    FROM pairs
)
INSERT INTO campaign_assignments (
    id, campaign_id, evaluator_id, evaluatee_id, evaluator_role, completed, evaluation_id, created_at, updated_at
)
SELECT
    g.assignment_id,
    '11111111-1111-1111-1111-111111111111',
    g.evaluator_id,
    g.evaluatee_id,
    'EXTERNAL',
    FALSE,
    NULL,
    NOW(),
    NOW()
FROM generated g
ON CONFLICT (id) DO UPDATE
SET campaign_id = EXCLUDED.campaign_id,
    evaluator_id = EXCLUDED.evaluator_id,
    evaluatee_id = EXCLUDED.evaluatee_id,
    evaluator_role = EXCLUDED.evaluator_role,
    updated_at = NOW();

-- Create evaluation rows for first 8 assignments to make reports/UI testable.
WITH selected_assignments AS (
    SELECT
        ca.id AS assignment_id,
        ca.evaluator_id,
        ca.evaluatee_id,
        ROW_NUMBER() OVER (ORDER BY ca.id) AS rn
    FROM campaign_assignments ca
    WHERE ca.campaign_id = '11111111-1111-1111-1111-111111111111'
), base AS (
    SELECT
        LOWER(
            SUBSTRING(md5('ev|' || assignment_id), 1, 8) || '-' ||
            SUBSTRING(md5('ev|' || assignment_id), 9, 4) || '-' ||
            SUBSTRING(md5('ev|' || assignment_id), 13, 4) || '-' ||
            SUBSTRING(md5('ev|' || assignment_id), 17, 4) || '-' ||
            SUBSTRING(md5('ev|' || assignment_id), 21, 12)
        ) AS evaluation_id,
        assignment_id,
        evaluator_id,
        evaluatee_id,
        rn
    FROM selected_assignments
    WHERE rn <= 8
), tpl AS (
    SELECT id AS template_id
    FROM templates
    WHERE status = 'PUBLISHED'
    ORDER BY created_at DESC
    LIMIT 1
)
INSERT INTO evaluations (
    id, campaign_id, assignment_id, evaluator_id, evaluatee_id, template_id,
    status, answers_json, total_score, section_scores_json, created_at, updated_at, submitted_at
)
SELECT
    b.evaluation_id,
    '11111111-1111-1111-1111-111111111111',
    b.assignment_id,
    b.evaluator_id,
    b.evaluatee_id,
    tpl.template_id,
    CASE WHEN b.rn <= 4 THEN 'SUBMITTED' ELSE 'DRAFT' END,
    CASE WHEN b.rn <= 4 THEN '[]' ELSE NULL END,
    CASE WHEN b.rn <= 4 THEN 88.5 ELSE NULL END,
    CASE WHEN b.rn <= 4 THEN '[]' ELSE NULL END,
    NOW(),
    NOW(),
    CASE WHEN b.rn <= 4 THEN NOW() - INTERVAL '1 day' ELSE NULL END
FROM base b
CROSS JOIN tpl
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status,
    answers_json = EXCLUDED.answers_json,
    total_score = EXCLUDED.total_score,
    section_scores_json = EXCLUDED.section_scores_json,
    updated_at = NOW(),
    submitted_at = EXCLUDED.submitted_at;

-- Link assignments to evaluations where created.
UPDATE campaign_assignments ca
SET evaluation_id = e.id,
    completed = (e.status IN ('SUBMITTED', 'COMPLETED')),
    updated_at = NOW()
FROM evaluations e
WHERE ca.campaign_id = '11111111-1111-1111-1111-111111111111'
  AND e.campaign_id = ca.campaign_id
  AND e.assignment_id = ca.id;

-- Keep legacy assignments_json populated for compatibility views.
UPDATE campaigns c
SET assignments_json = COALESCE(
        (
            SELECT jsonb_agg(
                jsonb_build_object(
                    'id', ca.id,
                    'evaluatorId', ca.evaluator_id,
                    'evaluateeId', ca.evaluatee_id,
                    'evaluatorRole', ca.evaluator_role,
                    'completed', ca.completed,
                    'evaluationId', ca.evaluation_id
                )
                ORDER BY ca.id
            )::text
            FROM campaign_assignments ca
            WHERE ca.campaign_id = c.id
        ),
        '[]'
    ),
    updated_at = NOW()
WHERE c.id = '11111111-1111-1111-1111-111111111111';

COMMIT;

-- Quick verification output.
SELECT 'template_count' AS metric, COUNT(*)::text AS value FROM templates
UNION ALL
SELECT 'campaign_count', COUNT(*)::text FROM campaigns
UNION ALL
SELECT 'assignment_count_seed_campaign', COUNT(*)::text FROM campaign_assignments WHERE campaign_id = '11111111-1111-1111-1111-111111111111'
UNION ALL
SELECT 'evaluation_count_seed_campaign', COUNT(*)::text FROM evaluations WHERE campaign_id = '11111111-1111-1111-1111-111111111111'
UNION ALL
SELECT 'audience_person_count', COUNT(*)::text FROM audience_persons WHERE tenant_id = 'tenant-001'
UNION ALL
SELECT 'audience_group_count', COUNT(*)::text FROM audience_groups WHERE tenant_id = 'tenant-001'
UNION ALL
SELECT 'audience_membership_count', COUNT(*)::text FROM audience_memberships WHERE tenant_id = 'tenant-001';
