# FES Mockup Gap Implementation Plan (Phased Task Breakdown)

Last updated: 2026-02-22  
Primary reference: `docs/FES_UIUX_Full_Menus_Detailed_Mockups_v1.2.pdf`  
Gap baseline: `docs/FES_Mockup_Backend_Gap_Analysis.md`

## 1. Objective

Implement full backend parity with the FES mockup (`S-01` to `S-29`) while preserving existing production-grade capabilities and minimizing breakage to current API consumers.

This plan is:
1. Detailed.
2. Phase-based.
3. Task-oriented with acceptance criteria.
4. Backward-compatible by default.

## 2. Delivery Principles

1. Backward compatibility first.
2. Expand data model before behavior cutover.
3. Introduce new APIs in parallel with legacy endpoints.
4. Protect critical workflows with integration tests before migration.
5. Feature flag major behavior changes.
6. Produce API contracts and migration scripts before UI integration starts.

## 3. Scope Map to Mockup

Target parity areas:
1. Lifecycle states and step windows (`S-05`, `S-21`, `S-22`).
2. Configuration modules (`S-06`..`S-11`, `S-23`, `S-24`).
3. Assignment model and admin views (`S-19`, `S-20`).
4. Evaluator and submission workflow details (`S-14`..`S-17`).
5. Faculty result policy (`S-18`).
6. Notifications rules/templates/delivery logs (`S-27`..`S-29`).
7. Audit/users/roles admin coverage.

Out-of-scope unless explicitly approved:
1. Replacing existing audience ingestion/rule control plane.
2. Removing existing legacy API routes in this implementation window.

## 4. Phase Overview

1. Phase 0: Foundations, contracts, and safety rail setup.
2. Phase 1: Core schema expansion (Schema Only for repeated domains).
3. Phase 2: Lifecycle engine and step-window execution (Behavior/API).
4. Phase 3: Assignment domain expansion and evaluator workflow parity (Behavior/API).
5. Phase 4: Question bank + versioning + scoring preview (Behavior/API).
6. Phase 5: Notifications rules/templates/delivery logs (Behavior/API).
7. Phase 6: Admin operations (audit listing, users/roles API coverage).
8. Phase 7: Cutover hardening, migration completion, and deprecation prep.

### 4.2 Current Delivery Status (As of 2026-02-22)

1. Phase 0: Completed.
2. Phase 1: Completed (schema scaffold delivered for parity domains).
3. Phase 2: Completed for backend behavior/API scope.
4. Phase 3: Completed.
5. Phase 4: Completed (question bank/versioning/scoring preview delivered).
6. Phase 5: Not started.
7. Phase 6: Not started.
8. Phase 7: Not started.

### 4.1 Repeated Domain Clarification (No Double Implementation)

The following domains intentionally appear in multiple phases with different execution type:
1. Assignments:
2. Phase 1 = schema extension only.
3. Phase 3 = full business logic and API implementation.
4. Notifications:
5. Phase 1 = table scaffolding only.
6. Phase 5 = rules/templates/delivery behavior and APIs.
7. Lifecycle:
8. Phase 1 = supporting schema only.
9. Phase 2 = state machine, transition APIs, and runtime behavior.

Rule: when a domain appears in an earlier phase and a later phase, the earlier phase must not implement runtime behavior, only structure.

---

## 5. Detailed Tasks by Phase

## Phase 0: Foundations and Contracts

### Goal
Lock a safe implementation baseline before changing runtime behavior.

### Tasks

1. Confirm canonical parity matrix.
2. Review and sign off `docs/FES_Mockup_Backend_Gap_Analysis.md`.
3. Tag each screen as `Phase target` and `Dependency`.

4. Create API versioning policy for new modules.
5. Decide whether to:
6. Add under `/api/v1/...` with backward compatibility.
7. Or introduce `/api/v2/...` for high-change resources.

8. Create feature flags for high-risk modules.
9. `features.enable-step-windows`
10. `features.enable-pdf-lifecycle`
11. `features.enable-question-bank`
12. `features.enable-notification-rule-engine`

13. Define migration and rollback policy.
14. Forward migration scripts must be idempotent.
15. Rollback procedures documented for each phase.

16. Add observability baselines.
17. Metrics for API error rates per module.
18. Metrics for lifecycle transitions.
19. Metrics for validation failures.

17. Add architecture docs.
18. Data model ADR for step-window and category/rating domains.
19. API ADR for assignment resource split.

### Deliverables
1. `docs/FES_API_EVOLUTION_POLICY.md`
2. `docs/FES_MIGRATION_ROLLBACK_GUIDE.md`
3. Feature-flag additions in `application.yml`.

### Acceptance Criteria
1. Approved contract decisions recorded.
2. Feature flags available and default-safe.
3. Test baseline green before Phase 1 starts.

---

## Phase 1: Core Schema Expansion

### Goal
Add new tables/entities for PDF parity without changing existing flows.
Execution type: `Schema Only`.

### Tasks

1. Add lifecycle/state expansion schema.
2. Introduce states supporting `PUBLISHED_OPEN`, `RESULTS_PUBLISHED`, and reopen semantics.
3. Add transition event table for auditability.

4. Add steps schema.
5. `campaign_steps` table with:
6. `campaign_id`
7. `step_type` (`STUDENT`, `PEER`, `SELF`, `DEPARTMENT`)
8. `enabled`
9. `display_order`
10. `open_at`, `close_at`
11. `late_allowed`, `late_days`
12. `instructions`, `notes`

13. Add category and weight schema.
14. `campaign_categories` table:
15. `category_name`
16. `weight_percent`
17. unique constraints per campaign.

18. Add designation mapping schema.
19. `designation_weight_mappings` table:
20. `campaign_id`, `designation`, `category_id`, `weight_percent`.

21. Add rating scale schema.
22. `campaign_rating_scales` and `campaign_rating_scale_items`.
23. Unique value constraints per campaign.

24. Add question-bank scaffold.
25. `question_bank_sets`, `question_bank_items`, `question_bank_item_versions`.

26. Extend assignments schema (non-breaking, Schema Only).
27. Add nullable columns:
28. `step_type`, `section_id`, `faculty_id`, `anonymity_mode`, `status`.

29. Add notification rule/template/delivery tables (Schema Only).

30. Update JPA entities/repositories for all new tables.
31. Keep old entities and mapping paths intact.

32. Seed defaults.
33. Seed default steps and default category/rating templates for existing campaigns if required.

### Deliverables
1. New Flyway migrations (next sequential versions).
2. New entity/repository classes.
3. Migration verification tests.
4. Explicit handoff checklist for Phase 2/3/5 behavior implementation.

### Acceptance Criteria
1. Migrations apply cleanly on fresh DB and existing DB.
2. Existing APIs remain behaviorally unchanged.
3. New schema passes integration smoke test.
4. No new runtime behavior is introduced for assignments/notifications/lifecycle beyond current behavior.

---

## Phase 2: Lifecycle Engine and Step Windows

### Goal
Implement PDF-compatible lifecycle transitions and per-step windows.
Execution type: `Behavior/API`.

### Tasks

1. Extend domain enums and transition rules.
2. Add lifecycle actions:
3. `publish`
4. `close`
5. `reopen`
6. `publish-results`
7. `archive`

8. Add lifecycle service methods.
9. Enforce transition guards.
10. Publish lock behavior on config modules after publish.

11. Implement step window management APIs.
12. CRUD/reorder endpoints.
13. Validation:
14. no overlapping invalid windows.
15. close > open.
16. disabled steps are excluded from evaluator flows.

17. Implement lifecycle impact preview API (`S-22` backend support).
18. Return:
19. lock effects
20. draft/submission impact counts
21. role visibility impacts for results publish.

22. Update scheduler logic.
23. Combine campaign state and step window checks.
24. Ensure deadline reminders are step-aware.

25. Emit lifecycle audit events and outbox events.

### Implementation Status (2026-02-22)

Completed:
1. Extended lifecycle states and transition rules (`PUBLISHED_OPEN`, `RESULTS_PUBLISHED`, reopen path).
2. Added lifecycle APIs:
3. `POST /api/v1/campaigns/{id}/lifecycle/publish`
4. `POST /api/v1/campaigns/{id}/lifecycle/close`
5. `POST /api/v1/campaigns/{id}/lifecycle/reopen`
6. `POST /api/v1/campaigns/{id}/lifecycle/publish-results`
7. `POST /api/v1/campaigns/{id}/lifecycle/impact-preview`
8. `GET /api/v1/campaigns/{id}/lifecycle/events`
9. Added step window APIs:
10. `GET /api/v1/campaigns/{id}/steps`
11. `PUT /api/v1/campaigns/{id}/steps`
12. Added strict step validation:
13. allowed step types
14. unique and continuous display order
15. non-negative late days
16. `closeAt > openAt`
17. campaign state lock (`DRAFT`/`SCHEDULED` only for edit)
18. Added admin-only authorization on lifecycle and steps endpoints.
19. Added actor and reason capture for lifecycle actions and lifecycle event log.
20. Persisted lifecycle metadata in campaign model:
21. `publishedAt`
22. `reopenedAt`
23. `resultsPublishedAt`
24. `locked`
25. Updated scheduler to treat `PUBLISHED_OPEN` as open for close/reminder flow.
26. Updated OpenAPI contract for lifecycle and steps resources.
27. Added/updated unit tests for transition rules, lifecycle metadata, and step validation/service behavior.

Deferred to next phase or hardening pass:
1. Step-aware reminder logic by step window timing (currently campaign-level reminder flow).
2. Dedicated integration tests for lifecycle impact preview endpoint and authorization matrix (unit coverage exists).
3. Outbox emission for lifecycle events (DB lifecycle audit events are implemented).

### API Tasks
1. Add routes:
2. `POST /api/v1/campaigns/{id}/lifecycle/publish`
3. `POST /api/v1/campaigns/{id}/lifecycle/close`
4. `POST /api/v1/campaigns/{id}/lifecycle/reopen`
5. `POST /api/v1/campaigns/{id}/lifecycle/publish-results`
6. `POST /api/v1/campaigns/{id}/lifecycle/impact-preview`
7. `GET/PUT /api/v1/campaigns/{id}/steps`

### Test Tasks
1. Unit tests for state machine.
2. Integration tests for impact preview.
3. Scheduler tests for step-aware reminder logic.
4. Authorization tests for admin-only lifecycle actions.

### Acceptance Criteria
1. Lifecycle endpoints reflect PDF state semantics.
2. Step windows can be configured and validated.
3. Existing campaign endpoints continue to work.

---

## Phase 3: Assignment Model Expansion and Evaluator Flow Parity

### Goal
Align assignment and evaluator/submission backend behavior with screens `S-12` to `S-20`.
Execution type: `Behavior/API` using Phase 1 schema.

### Tasks

1. Refactor assignment domain model.
2. Introduce rich assignment aggregate:
3. campaign, step, section, faculty, evaluator, anonymity, status.

4. Create assignment resource APIs.
5. List with filters (campaign/step/department/section/faculty/status).
6. Create/edit single assignment.
7. Duplicate detection with explicit error payload.

8. Implement evaluator dashboard aggregate endpoint (`S-14`).
9. Provide assigned/draft/submitted counts and progress.

10. Implement pre-submit validation endpoint (`S-16`).
11. Validate required questions and required remarks.
12. Return machine-readable issue references for UI deep links.

13. Implement submission receipt endpoint (`S-17`).

14. Add admin submission detail projection.

### Implementation Status (2026-02-22, Progress Update)

Completed in current slice:
1. Added rich assignment admin APIs:
2. `GET /api/v1/assignments` (filter + pagination + sorting)
3. `GET /api/v1/assignments/{id}`
4. `POST /api/v1/assignments`
5. `PUT /api/v1/assignments/{id}`
6. Added assignment metadata handling in persistence model:
7. `stepType`, `sectionId`, `facultyId`, `anonymityMode`, `status`
8. Added evaluator dashboard aggregate endpoint:
9. `GET /api/v1/evaluators/me/dashboard`
10. Added pre-submit validation endpoint:
11. `POST /api/v1/evaluations/validate-submit`
12. Added submission receipt endpoint:
13. `GET /api/v1/evaluations/{id}/receipt`
14. Added admin submission detail endpoint:
15. `GET /api/v1/evaluations/{id}/admin-detail`
16. Added admin submission reopen endpoint:
17. `POST /api/v1/evaluations/{id}/reopen`
18. Added targeted unit tests for assignment service, validation service, and reopen behavior.

Remaining for Phase 3:
1. Integrate step-window enforcement into evaluator submission eligibility checks. Completed.
2. Expand validation semantics (question-type-specific rules, stronger remarks policy variants). Completed (baseline type-aware checks added).
3. Add assignment duplicate diagnostics payload with richer conflict context. Completed (409 ProblemDetail with conflict metadata).
4. Add integration tests for new Phase 3 endpoints and security matrix. Completed (HTTP integration coverage added for assignment/admin submission/validation access behavior).

---

## Phase 4: Question Bank + Versioning + Scoring Preview

### Implementation Status (2026-02-22)

Completed:
1. Added question bank set APIs:
2. `POST /api/v1/questions-bank/sets`
3. `GET /api/v1/questions-bank/sets`
4. Added question bank item APIs:
5. `POST /api/v1/questions-bank/sets/{setId}/items`
6. `GET /api/v1/questions-bank/sets/{setId}/items`
7. Added versioning APIs:
8. `POST /api/v1/questions-bank/items/{itemId}/versions`
9. `GET /api/v1/questions-bank/items/{itemId}/versions`
10. `POST /api/v1/questions-bank/items/{itemId}/versions/{versionNo}/activate`
11. `GET /api/v1/questions-bank/items/{itemId}/versions/compare`
12. Implemented version lifecycle semantics:
13. supported statuses (`DRAFT`, `ACTIVE`, `RETIRED`)
14. active-version switching updates item defaults
15. activation retires previously active versions
16. Added scoring preview API:
17. `POST /api/v1/scoring/preview`
18. Added scoring preview validation and section-level breakdown response.
19. Added feature flag gating for question bank APIs (`features.enable-question-bank`).
20. Added admin-only authorization for question bank and scoring preview endpoints.
21. Updated OpenAPI and API documentation.
22. Added unit and integration tests for Phase 4 services and endpoints.
15. Include per-category breakdown and mandatory-remark status.

16. Add reopen submission action (admin).
17. Ensure audit trail written.

### API Tasks
1. `GET /api/v1/assignments`
2. `POST /api/v1/assignments`
3. `PUT /api/v1/assignments/{id}`
4. `GET /api/v1/assignments/{id}`
5. `GET /api/v1/evaluations/{id}/validate-submit`
6. `GET /api/v1/evaluations/{id}/receipt`
7. `POST /api/v1/evaluations/{id}/reopen`
8. `GET /api/v1/evaluator/dashboard`

### Data Tasks
1. Backfill new assignment fields where possible.
2. Mark unknown values explicitly (not null placeholders if required).

### Test Tasks
1. Assignment duplicate validation tests.
2. Validation endpoint correctness tests.
3. Reopen workflow and audit tests.

### Acceptance Criteria
1. `S-19/S-20` data requirements represented in API.
2. `S-16` pre-submit contract implemented.
3. Admin reopen behavior available and audited.

---

## Phase 4: Configuration Completeness (Question Types, Categories, Rating, Designation, Question Bank/Versions, Scoring Preview)

### Goal
Deliver full configuration surface for `S-06` to `S-11`, `S-23`, `S-24`, `S-25`, `S-26`.
Execution type: `Behavior/API`.

### Tasks

1. Implement question type management module.
2. Campaign-scoped type activation/deactivation.
3. Default remarks mandatory rule.

4. Implement categories and weight APIs.
5. Enforce total weight = 100.
6. Add lock-precondition checks.

5. Implement designation mapping APIs.
6. Add copy-from-designation action support.
7. Enforce per-designation total = 100.

8. Implement rating scale APIs.
9. Unique numeric values.
10. Active/inactive support.

11. Implement scoring settings API.
12. Fields:
13. scoring mode
14. normalization
15. rounding precision
16. include open-ended marks toggle
17. designation override behavior.

18. Implement score preview endpoint.
19. Sample/real submission selector.
20. Category raw/max/normalized/weight/weighted output.

21. Implement question bank library.
22. Library sets.
23. Import/export.
24. Add-to-campaign.
25. conflict mapping (category mismatch resolver endpoint).

26. Implement question version history and compare endpoint.
27. Enforce immutable references for historical submissions.

### API Tasks
1. `/api/v1/campaigns/{id}/question-types*`
2. `/api/v1/campaigns/{id}/categories*`
3. `/api/v1/campaigns/{id}/designation-mappings*`
4. `/api/v1/campaigns/{id}/rating-scale*`
5. `/api/v1/campaigns/{id}/scoring-settings*`
6. `/api/v1/campaigns/{id}/score-preview`
7. `/api/v1/questions-bank/*`
8. `/api/v1/questions/{id}/versions/*`

### Test Tasks
1. Weight and rating invariants.
2. Designation fallback logic.
3. Question version immutability against old submissions.
4. Score preview consistency vs persisted scoring.

### Acceptance Criteria
1. Backend fully supports `S-06`..`S-11`, `S-23`..`S-26`.
2. All configuration invariants enforced server-side.

---

## Phase 5: Notifications Rules, Templates, Delivery Logs

### Goal
Implement backend parity for `S-27` to `S-29`.
Execution type: `Behavior/API` using Phase 1 notification schema scaffolding.

### Tasks

1. Introduce notification rule CRUD.
2. Trigger type, audience, channel, schedule, enabled flag.

3. Introduce notification template CRUD.
4. Template variables registry by trigger.
5. Validation engine for required variables.
6. Preview rendering endpoint with sample data.
7. Send-test endpoint.

8. Build delivery event ingestion/projection.
9. Capture sent/failed/delivered statuses.
10. Include error code and retry metadata.

11. Implement delivery log query API.
12. Filter by date/rule/status/channel/recipient.
13. Export CSV endpoint.
14. Retry failed event endpoint.

15. Integrate existing outbox dispatch with notification domain events.
16. Preserve existing outbox transport choices.

### API Tasks
1. `/api/v1/notifications/rules/*`
2. `/api/v1/notifications/templates/*`
3. `/api/v1/notifications/templates/{id}/preview`
4. `/api/v1/notifications/templates/{id}/send-test`
5. `/api/v1/notifications/delivery-log*`
6. `/api/v1/notifications/delivery-log/{id}/retry`

### Test Tasks
1. Variable validation tests.
2. Delivery status transition tests.
3. Retry and backoff tests.
4. Integration tests with webhook adapter.

### Acceptance Criteria
1. `S-27`, `S-28`, `S-29` backend-complete.
2. Delivery log supports operational troubleshooting.

---

## Phase 6: Admin Operations (Audit Logs, Users/Roles, Permissions Matrix)

### Goal
Close remaining admin menu gaps and security governance coverage.

### Tasks

1. Implement audit log read APIs.
2. List/filter by actor/action/date/aggregate.
3. Detail endpoint with payload JSON.

4. Implement users and roles admin APIs (if this service owns them).
5. Role list/create/update/deactivate.
6. Permission assignment endpoints.
7. User-role assignment endpoints.

8. Add permission matrix endpoint.
9. Returns actions allowed per role and module.

9. Ensure method-level security for all new admin endpoints.
10. Add explicit authorization tests.

### API Tasks
1. `/api/v1/admin/audit-logs*`
2. `/api/v1/admin/roles*`
3. `/api/v1/admin/permissions*`
4. `/api/v1/admin/users/{id}/roles*`
5. `/api/v1/admin/permission-matrix`

### Test Tasks
1. Admin-only access tests.
2. Audit read/write consistency tests.

### Acceptance Criteria
1. Admin operational menu parity complete.
2. RBAC behavior documented and tested.

---

## Phase 7: Cutover, Hardening, and Deprecation Readiness

### Goal
Production-safe rollout and transition from mixed model to stable target model.

### Tasks

1. Feature-flag progressive rollout.
2. Enable new modules per environment.
3. Shadow-mode validations before hard enforcement.

4. Data parity checks and backfills.
5. Reconcile old and new assignment semantics.
6. Validate score consistency between engines.

7. Performance testing.
8. High-cardinality filters on submissions/assignments/delivery logs.
9. Scheduler throughput under step windows + notifications.

10. Security and compliance checks.
11. Authorization matrix review.
12. PII masking checks for faculty results and exports.

13. Documentation and SDK updates.
14. Update `docs/openapi.yaml`.
15. Update `docs/API_DOCUMENTATION.md`.
16. Add operations runbook entries for new modules.

17. Deprecation notices for legacy endpoints/fields.
18. Publish migration guidance for frontend consumers.

### Acceptance Criteria
1. All required parity endpoints documented.
2. All regression suites pass.
3. SLO and error budgets remain within target post-rollout.

---

## 6. Work Breakdown by Stream (Cross-Phase)

## Stream A: Database and Migration

Tasks:
1. Author Flyway migrations.
2. Add indexes for all new filter-heavy paths.
3. Add unique/check constraints for scoring and rating invariants.
4. Add migration integration tests (fresh DB + upgrade DB).

Done criteria:
1. No migration drift.
2. No blocking locks in migration path.

## Stream B: Domain and Application Services

Tasks:
1. Extend aggregates and invariants.
2. Add new service methods and transition guards.
3. Keep old flows delegated where backward compatibility needed.

Done criteria:
1. Domain invariants enforced centrally.
2. Service behavior deterministic and test-covered.

## Stream C: API and Contracts

Tasks:
1. Add DTOs and controllers for each phase.
2. Maintain consistent error model (`ProblemDetail`).
3. Version or namespace endpoints as decided in Phase 0.

Done criteria:
1. `openapi.yaml` complete and accurate.
2. Consumer-facing examples provided.

## Stream D: Security and Governance

Tasks:
1. Role/permission mapping for each new action.
2. Method-level guards.
3. Audit event write for all admin-impacting actions.

Done criteria:
1. Unauthorized access tests pass.
2. Audit completeness validated.

## Stream E: Testing and QA

Tasks:
1. Unit tests for invariants and transitions.
2. Integration tests for every new endpoint group.
3. Concurrency tests for submit/reopen/lifecycle transitions.
4. Contract tests for request/response schema.

Done criteria:
1. All critical paths have automated tests.
2. No untested state transition remains.

## Stream F: Operations and Rollout

Tasks:
1. Feature flag rollout plan.
2. Dashboard and alert additions.
3. Runbooks for failure and rollback.

Done criteria:
1. On-call can triage every new module.
2. Rollback tested in lower environment.

---

## 7. Suggested Execution Order (Sprints)

1. Sprint 1: Phase 0 + Phase 1 migrations/entities.
2. Sprint 2: Phase 2 lifecycle + steps APIs.
3. Sprint 3: Phase 3 assignments/evaluator/submission parity.
4. Sprint 4: Phase 4 config modules part A (categories/rating/designation/scoring).
5. Sprint 5: Phase 4 config modules part B (question bank/versioning/preview).
6. Sprint 6: Phase 5 notifications module.
7. Sprint 7: Phase 6 admin ops.
8. Sprint 8: Phase 7 hardening, rollout, deprecation readiness.

## 8. Dependency Matrix (Critical)

1. Step windows depend on lifecycle state model.
2. Assignment parity depends on step/window schema.
3. Faculty results policy depends on lifecycle + designation/category/rating models.
4. Notification templates depend on trigger/rule model.
5. UI integration should start after each phase’s OpenAPI is stable.

## 9. Definition of Done (Global)

Each phase is complete only when:
1. Code merged.
2. Migrations applied and verified.
3. API docs updated.
4. Tests added and green.
5. Observability/alerts updated.
6. Runbook updated.
7. Backward compatibility verified.

## 10. Tracking Template

Use the following per-task status fields:
1. `Task ID`
2. `Phase`
3. `Owner`
4. `Status` (`Todo/In Progress/Blocked/Done`)
5. `Dependencies`
6. `PR/Commit`
7. `Test Evidence`
8. `Rollback Note`

Recommended task ID style:
1. `P2-LC-001` (Phase 2, Lifecycle, Task 001)
2. `P4-QB-010` (Phase 4, Question Bank, Task 010)

## 11. Immediate Next Tasks (Week 1)

1. Approve phase sequencing and endpoint version strategy.
2. Create Phase 1 migration design doc and ERD.
3. Implement Phase 1 Flyway migrations and entity scaffolding.
4. Add Phase 1 migration integration tests.
5. Publish initial OpenAPI stubs for lifecycle + steps endpoints.
