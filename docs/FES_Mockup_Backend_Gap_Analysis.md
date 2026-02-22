# FES UI/UX Mockup vs Backend Gap Analysis

Last updated: 2026-02-22  
Scope: Compare `docs/FES_UIUX_Full_Menus_Detailed_Mockups_v1.2.pdf` against current backend implementation in `src/main/java`, `src/main/resources/db/migration`, and API contracts in `docs/openapi.yaml`.

## 1. Executive Summary

This backend already provides a strong operational core for campaigns, assignments, submissions, reporting, settings, audience ingestion, and rule governance.  
However, the PDF mockup describes a product model that is more education-domain specific and more UI-workflow explicit than the current API/domain model.

Key conclusion:
1. Core backbone is present (`Login`, `Campaign`, `Assignment generation`, `Evaluation submit`, `Reports`).
2. Many PDF menu modules are missing as first-class backend capabilities (`Steps Setup`, `Rating Scale`, `Designation Mapping`, `Notification Templates`, `Delivery Log`, `Users & Roles` admin APIs).
3. Campaign lifecycle and step-window backend parity is now implemented in Phase 2 (as of 2026-02-22), while scoring/configuration and assignment-rich workflows remain as major gaps.

## 2. Source Baseline

Primary sources reviewed:
1. `docs/FES_UIUX_Full_Menus_Detailed_Mockups_v1.2.pdf`
2. `docs/openapi.yaml`
3. `docs/API_DOCUMENTATION.md`
4. `docs/FEATURES.md`
5. `docs/SYSTEM_RUNBOOK_PHASE1_TO_5.md`
6. `src/main/java/com/evaluationservice/api/controller/*`
7. `src/main/java/com/evaluationservice/application/service/*`
8. `src/main/java/com/evaluationservice/domain/*`
9. `src/main/resources/db/migration/*`
10. `src/test/java/*`

## 3. Current Backend Capability Snapshot

Implemented and stable modules:
1. Authentication (dev-mode login): `POST /api/v1/auth/login`.
2. Campaign management: create/read/update/list/activate/close/archive/extend deadline.
3. Assignment flows:
4. Manual assignment add.
5. Dynamic assignment generation (ALL_TO_ALL, ROUND_ROBIN, MANAGER_HIERARCHY, ATTRIBUTE_MATCH).
6. Assignment reconciliation and backfill.
7. Evaluation flows:
8. Submit evaluation.
9. Save draft.
10. List by campaign/evaluatee.
11. Admin moderation (flag/invalidate).
12. Reports:
13. Individual report.
14. Campaign report.
15. CSV/PDF export (feature-flag gated).
16. System settings and campaign-level overrides.
17. Audience ingestion platform (CSV/JSON/REST/JDBC + mapping profiles + replay + retention).
18. Rule control plane with approval workflow and audit logging.
19. Outbox dispatch and retention schedulers.

## 4. High-Impact Model Mismatches vs PDF

### 4.1 Lifecycle mismatch

PDF lifecycle:
1. Draft
2. Published/Open
3. Closed
4. Results Published
5. Archived
6. Reopen flow

Backend lifecycle (current):
1. `DRAFT`
2. `SCHEDULED`
3. `ACTIVE`
4. `PUBLISHED_OPEN`
5. `CLOSED`
6. `RESULTS_PUBLISHED`
7. `ARCHIVED`

Evidence:
1. `src/main/java/com/evaluationservice/domain/enums/CampaignStatus.java`
2. `src/main/java/com/evaluationservice/api/controller/CampaignController.java`

Current status:
1. `Publish`, `Close`, `Reopen`, `Publish Results`, and impact preview APIs are now available.
2. Lifecycle event history API is available.
3. Lifecycle metadata (`publishedAt`, `reopenedAt`, `resultsPublishedAt`, `locked`) is persisted and exposed in campaign response.
4. Remaining work is around downstream module effects (assignment and result policy modules in later phases), not base lifecycle state support.

### 4.2 Step-window model missing

PDF requires per-step configuration (`Student`, `Peer`, `Self`, `Department`) with independent windows and lifecycle actions.

Backend now has campaign-level dates plus step-window APIs and persistence.

Evidence:
1. `src/main/java/com/evaluationservice/domain/entity/Campaign.java`
2. `src/main/java/com/evaluationservice/api/dto/request/CreateCampaignRequest.java`

Current status:
1. `GET/PUT /api/v1/campaigns/{id}/steps` implemented.
2. Validation implemented for step type, order, and window constraints.
3. Remaining work is deeper evaluator-flow consumption of step windows in Phase 3.

### 4.3 Scoring and weights semantics mismatch

PDF uses category percentages summing to 100.

Backend `Weight` value object only accepts `(0,1]`.

Evidence:
1. `src/main/java/com/evaluationservice/domain/value/Weight.java`
2. `src/main/java/com/evaluationservice/application/service/ScoringService.java`

Impact:
1. Directly sending PDF-style `30/25/20` breaks validation.
2. Score preview formulas in `S-24` need dedicated normalization logic and DTOs.

### 4.4 Question/rating vocabulary mismatch

PDF types: `SINGLE_SELECTION`, `OPEN_ENDED`, etc.  
Backend types: `SINGLE_CHOICE`, `OPEN_TEXT`, `LIKERT_SCALE`, etc.

Evidence:
1. `src/main/java/com/evaluationservice/domain/enums/QuestionType.java`

Impact:
1. UI contracts need mapping layer or enum harmonization.

### 4.5 Domain coverage mismatch

PDF expects education-oriented entities (designation, semester, section, faculty, step-level assignment metadata).

Backend assignments are generic evaluator/evaluatee tuples without section/faculty/step/anonymity/status fields.

Evidence:
1. `src/main/java/com/evaluationservice/domain/entity/CampaignAssignment.java`
2. `src/main/resources/db/migration/V5__campaign_assignments_table.sql`

Impact:
1. `S-19`/`S-20` data grids cannot be represented as designed.

## 5. Screen-by-Screen Coverage Matrix (`S-01` to `S-29`)

Legend:
1. `Full`: backend supports the intended workflow.
2. `Partial`: some data/operations exist but not full PDF workflow.
3. `Missing`: no direct backend module/contract.

| Screen | Status | Existing Backend Support | Gap | Required Backend Additions |
|---|---|---|---|---|
| S-01 Login | Full (dev mode) | `POST /api/v1/auth/login` | No captcha/rate-limit flow | Auth hardening endpoints/policy for failed attempts + captcha |
| S-02 Admin Dashboard | Partial | `GET /api/v1/dashboard/stats` | No config blocker chips/progress widget/recent-submission detail | Dashboard aggregates for config completeness and submission feed |
| S-03 Campaign List | Partial | Campaign list/create/update/activate/close/archive | No year/semester/lock workflows | Add campaign fields (`year`,`semester`,`locked`) + filters + lock API |
| S-04 Campaign Detail | Partial | Campaign detail/update | No config checklist tab model | Config completeness endpoint by module |
| S-05 Steps Setup | Partial | `GET/PUT /api/v1/campaigns/{id}/steps` | No full UI workflow projection endpoints yet | Extend to UI-specific projections and Phase 3 evaluator-flow integration |
| S-06 Question Types | Missing | Template question type enum | No campaign-scoped type/default remarks rule admin | Question-type config endpoints |
| S-07 Categories & Weights | Partial | Template sections+weights | Percent-100 model not supported | Campaign category % tables + sum=100 validators |
| S-08 Designation Mapping | Missing | None | No designation overrides | Designation mapping schema + APIs + fallback logic |
| S-09 Rating Scale | Missing | Numeric answers only | No rating label/value config | Rating scale CRUD + uniqueness constraints |
| S-10 Question Bank List | Partial | Questions embedded in templates | No shared reusable bank/list/filter/import | Question bank module and APIs |
| S-11 Question Detail | Partial | Template question editing | No standalone bank question detail lifecycle | Bank question detail endpoints |
| S-12 Submissions Admin List | Partial | Evaluation list by campaign | Missing step/section/faculty/department filters and exports by filter | Enhanced submissions query + export endpoints |
| S-13 Submission Detail | Partial | `GET /api/v1/evaluations/{id}` | No reopen workflow/calc breakdown projection | Admin detail projection + reopen endpoint |
| S-14 Evaluator Dashboard | Partial | `GET /campaigns/assignments/me` | No dedicated progress/KPI payload | Evaluator dashboard endpoint with grouped counts |
| S-15 Evaluation Form | Partial | Save draft + submit | PDF mandatory-remarks logic not explicitly modeled | Question-level submit-time validation policy |
| S-16 Review & Submit | Partial | Submission pipeline exists | No pre-submit validation summary endpoint | Validation endpoint with field-level issues/deep links |
| S-17 Submitted Confirmation | Partial | Submit response includes id/status | No receipt contract | Submission receipt endpoint/DTO |
| S-18 Faculty My Results | Partial | Reports endpoints exist | No faculty policy gate + stepwise visibility semantics | Faculty results endpoint with anonymity/threshold policy |
| S-19 Assignments Admin List | Partial | Assignments exist as tuples | Missing step/section/faculty/anonymity/status model | Assignment schema extension + list/filter endpoint |
| S-20 Assignment Detail | Partial | Add assignments bulk | No singular create/edit with duplicate diagnostics | Assignment CRUD + duplicate/reference validators |
| S-21 Lifecycle & Step Windows | Partial | Publish/close/reopen/publish-results, lifecycle events, step-window APIs | Downstream business modules not fully wired to lifecycle/step policies | Phase 3/4 integration with assignment, submission, and results modules |
| S-22 Publish/Close Modal | Partial | Impact preview API available | Modal-specific enriched breakdown fields still basic | Expand impact DTO to include additional per-role/per-step counters |
| S-23 Scoring & Weighting | Partial | Scoring method at template/campaign | Missing normalization/rounding/designation override options | Scoring settings per campaign/step + resolver |
| S-24 Score Preview | Partial | Scores computed on real submissions | No dedicated preview endpoint for sample/what-if | Preview calculation endpoint + breakdown DTO |
| S-25 Question Bank Library | Missing | No standalone library sets | No import conflict resolver/add-to-campaign workflow | Library set/version schema + import/clone/export APIs |
| S-26 Question Versions | Partial | Template versioning exists | No per-question immutable version history/compare | Question version tables + compare endpoint |
| S-27 Notification Rules | Missing | Scheduler reminder logic only | No configurable per-campaign notification rules | Notification rule CRUD + trigger scheduling model |
| S-28 Notification Template Detail | Missing | None | No template editor/variable validation/test-send APIs | Template CRUD + variable resolver + test send endpoint |
| S-29 Delivery Log | Partial | Outbox and event persistence exists | No notification-domain delivery log query/filter API | Delivery event projection + filter/retry APIs |

## 6. Menu-Level Gap Summary

From PDF role menus:

### 6.1 Admin menu items with clear backend support
1. Dashboard
2. Evaluation Campaigns
3. Assignments
4. Submissions
5. Reports
6. Settings

### 6.2 Admin menu items partially supported (indirect/structural only)
1. Campaign Lifecycle
2. Questions Bank
3. Question Versions
4. Scoring & Weighting
5. Audit Log (write path exists, read APIs missing)

### 6.3 Admin menu items effectively missing
1. Steps Setup
2. Question Types (as managed module)
3. Categories & Weights (campaign percent model)
4. Designation Mapping
5. Rating Scale
6. Notifications & Reminders as rules/templates/delivery-log module
7. Users & Roles management APIs

### 6.4 Evaluator/Faculty menu implications
1. Evaluator dashboard and assignment list are mostly possible.
2. Faculty results experience requires dedicated policy-aware endpoints not currently present.

## 7. Data Model Delta Needed for Full PDF Parity

Recommended new/expanded entities:
1. `campaign_steps`:
2. `campaign_id`, `step_type`, `enabled`, `order_no`, `open_at`, `close_at`, `late_policy`, `notes`
3. `campaign_categories`:
4. `campaign_id`, `name`, `weight_percent`
5. `designation_weight_mappings`:
6. `campaign_id`, `designation`, `category_id`, `weight_percent`
7. `rating_scales` and `rating_scale_items`:
8. campaign-scoped label/value pairs
9. `question_bank_items` and `question_versions`:
10. immutable version snapshots + status
11. `campaign_assignments` extension:
12. `step_type`, `section_id`, `faculty_id`, `anonymity_mode`, `status`, `window_override`
13. `notification_rules`, `notification_templates`, `notification_deliveries`
14. `campaign_lifecycle_events` (or expanded campaign state events)

## 8. API Delta Needed (High-Level)

Required endpoint groups:
1. `/api/v1/campaigns/{id}/steps/*`
2. `/api/v1/campaigns/{id}/categories/*`
3. `/api/v1/campaigns/{id}/designation-mappings/*`
4. `/api/v1/campaigns/{id}/rating-scale/*`
5. `/api/v1/questions-bank/*` and `/api/v1/questions/{id}/versions/*`
6. `/api/v1/assignments/*` (resource-centric CRUD with rich filters)
7. `/api/v1/campaigns/{id}/lifecycle/publish|close|reopen|publish-results`
8. `/api/v1/campaigns/{id}/lifecycle/impact-preview`
9. `/api/v1/evaluations/{id}/validate-submit`
10. `/api/v1/faculty/results*`
11. `/api/v1/notifications/rules*`, `/templates*`, `/deliveries*`
12. `/api/v1/admin/audit-logs*`
13. `/api/v1/admin/users*` and `/roles*` (if within service scope)

## 9. Priority and Phasing Recommendation

### Phase A (unblock core UI flow)
1. Lifecycle expansion (publish/reopen/results published).
2. Step windows.
3. Assignment model extension (step/section/faculty/anonymity/status).
4. Pre-submit validation endpoint.

### Phase B (configuration completeness)
1. Categories & weights (% model).
2. Rating scale.
3. Designation mapping.
4. Score preview endpoint.

### Phase C (content governance)
1. Question bank.
2. Question versioning and compare.

### Phase D (communications and operations)
1. Notification rules/templates/delivery log.
2. Audit log read APIs.
3. Users/roles admin APIs (if in-service ownership is confirmed).

## 10. Risks and Notes

1. Current backend includes advanced no-code audience/rule-control capabilities not represented in the PDF; product decisions are needed on whether to expose them in UI.
2. Weight-unit mismatch (`0..1` vs `0..100`) must be resolved centrally to avoid silent scoring bugs.
3. Migration should preserve existing APIs; add new resources first, then deprecate old shape gradually.
4. `S-22` modal logic should be server-driven (impact preview endpoint) to avoid client-side inconsistency.

## 11. Evidence Pointers (Key Files)

1. Lifecycle enums/transitions: `src/main/java/com/evaluationservice/domain/enums/CampaignStatus.java`
2. Campaign APIs: `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
3. Evaluation APIs: `src/main/java/com/evaluationservice/api/controller/EvaluationController.java`
4. Report APIs: `src/main/java/com/evaluationservice/api/controller/ReportController.java`
5. Settings APIs: `src/main/java/com/evaluationservice/api/controller/SystemSettingsController.java`
6. Assignment schema: `src/main/resources/db/migration/V5__campaign_assignments_table.sql`
7. Core schema: `src/main/resources/db/migration/V1__core_schema.sql`
8. Weight constraints: `src/main/java/com/evaluationservice/domain/value/Weight.java`
9. Scoring service: `src/main/java/com/evaluationservice/application/service/ScoringService.java`
10. Scheduler lifecycle behavior: `src/main/java/com/evaluationservice/infrastructure/scheduler/CampaignScheduler.java`
11. Admin audit write path: `src/main/java/com/evaluationservice/infrastructure/service/AdminAuditLogService.java`
12. OpenAPI contract: `docs/openapi.yaml`
