# Evaluation Service Runbook (Phases 1 to 5)

## 1. Purpose and Audience

This runbook explains what the system does, how it works, and why it was designed this way.

This document is for:
1. Non-technical stakeholders (operations, business owners, compliance).
2. Technical stakeholders (backend engineers, SRE, platform engineers, QA).

Scope of this runbook:
1. Assignment storage migration and reliability (Phase 1 and 2).
2. Canonical audience ingestion platform (Phase 3).
3. Rule DSL and rule control plane (Phase 4).
4. Admin no-code governance workflow and guardrails (Phase 5).

---

## 2. Executive Summary (Non-Technical)

The system now supports configuration-driven evaluation operations across different domains (university, hospital, NGO, enterprise) with minimal code changes.

What is new:
1. Assignment data moved from fragile JSON-only storage to a reliable relational model with safety checks.
2. Evaluation submission flow now validates ownership and supports strong integrity checks.
3. Audience onboarding is connector-driven (CSV/JSON/REST/JDBC), with mapping profiles and replay.
4. Rule operations are now managed by admin APIs with versioning and approval workflow.
5. Governance controls exist for admin actions: approval gates, role-based control, audit logs, outbox events.

Why this matters:
1. Lower onboarding friction for new organizations.
2. Better data integrity and traceability.
3. Safer production governance and auditability.
4. Clear path for scale and operational reliability.

---

## 3. System Architecture (Technical)

Main layers:
1. API controllers (`src/main/java/com/evaluationservice/api/controller`)
2. Application services (`src/main/java/com/evaluationservice/application/service`)
3. Infrastructure services/repositories/entities
4. DB migrations (`src/main/resources/db/migration`)

Core bounded areas:
1. Campaign and assignment lifecycle.
2. Evaluation submission lifecycle.
3. Audience ingestion and mapping.
4. Rule control plane and admin governance.
5. Integration outbox and dispatch.

---

## 4. Data Model and Migration Story

### 4.1 Legacy Assignment Problem

Legacy `campaigns.assignments_json` was not ideal for:
1. Efficient querying.
2. Safe updates.
3. Reconciliation and auditability.
4. Scaling reads/writes.

### 4.2 New Assignment Model

Relational table:
1. `campaign_assignments` (migration `V5__campaign_assignments_table.sql`)
2. Dual-write strategy from campaign operations.
3. Configurable read mode:
4. `JSON` (legacy)
5. `DUAL` (relational-first read while preserving compatibility)
6. `V2` (relational source of truth)

### 4.3 Safety Mechanisms

1. Reconciliation endpoint:
2. `GET /api/v1/campaigns/{id}/assignments/reconcile`
3. Aggregated parity report:
4. `GET /api/v1/campaigns/assignments/reconcile/report?maxCampaigns=...`
5. Scheduled reconciliation:
6. `AssignmentReconciliationScheduler`
7. Backfill API:
8. `POST /api/v1/campaigns/assignments/backfill?dryRun=...&maxCampaigns=...`
9. Lower-env migration verification test:
10. `Phase1MigrationLowerEnvIntegrationTest`

---

## 5. Evaluation Submission Lifecycle (Phase 2)

Submission path (`EvaluationSubmissionService`):
1. Idempotency check by assignment.
2. Campaign must be active.
3. Assignment ownership validation (campaign/evaluator/evaluatee consistency).
4. Template load and scoring execution.
5. Evaluation persisted.
6. Assignment marked completed in relational store.
7. Legacy campaign assignment state synced for compatibility.
8. Domain event emitted.

Integrity features:
1. Assignment completion update fails fast if row not found.
2. Transaction and concurrency tests are in place.
3. Rollback behavior verified in integration tests.

Known race semantics:
1. Duplicate concurrent submit race is controlled by DB uniqueness.
2. Single-writer integrity guaranteed.

---

## 6. Audience Ingestion Platform (Phase 3)

### 6.1 Canonical Audience Tables

Key entities include:
1. Tenant.
2. Person.
3. Group.
4. Membership.
5. Ingestion run.
6. Rejection rows.
7. Mapping profiles.
8. Mapping profile events.
9. Ingestion snapshots.
10. Integration outbox events.

### 6.2 Connectors

Supported source connectors:
1. CSV.
2. JSON.
3. REST.
4. JDBC (connection-reference based).

Connector abstraction:
1. `AudienceSourceConnector` contract.
2. Plug-in connector registry in ingestion service.

### 6.3 Ingestion Workflow

1. Receive ingest request with `tenantId`, `sourceType`, `sourceConfig`, optional `mappingProfileId`, `dryRun`.
2. Load source records from connector.
3. Apply mapping profile if provided.
4. Save snapshot for replay.
5. Validate records by entity type (`PERSON`, `GROUP`, `MEMBERSHIP`).
6. Persist valid records if not dry-run.
7. Persist row-level rejections for invalid records.
8. Mark run status and statistics.

### 6.4 Replay Workflow

1. Replay by `runId`.
2. Load stored snapshot.
3. Re-process with optional dry-run override.
4. Produce new run and rejection outputs.

### 6.5 Validation Profiles (Policy-Driven)

Config path:
1. `evaluation.service.audience.validation-profiles.*`

Examples of profile rules:
1. Require person display name.
2. Minimum display name length.
3. Require person email.
4. Restrict email domains.
5. Restrict allowed group types.
6. Require group external reference.
7. Require membership role.
8. Require membership validity window.
9. Require active person/group for membership.
10. Role-to-group-type policy.
11. Validity window ordering (`valid_from <= valid_to`).

Why this exists:
1. Different organizations need different data quality policies.
2. Enables no-code strictness changes by configuration, not deployments.

### 6.6 Mapping Profiles

Capabilities:
1. Create/update/deactivate.
2. List/get.
3. Validate mapping payload.
4. Lifecycle events (audit trail).

### 6.7 Outbox and Retention

Outbox:
1. Durable event storage for integrations.
2. Dispatcher with retry/backoff/dead-letter behavior.
3. Transports:
4. `LOG`
5. `WEBHOOK`
6. `KAFKA`
7. `RABBITMQ`

Retention:
1. Snapshot TTL.
2. Mapping event TTL.
3. Outbox TTL (published and failed paths).
4. Scheduled cleanup jobs.

Observability:
1. Dispatch metrics.
2. Retention metrics.
3. Dead-letter warnings.

---

## 7. Rule DSL and Rule Engine Control Plane (Phase 4)

### 7.1 Rule Definition Model

Tables and entities:
1. `assignment_rule_definitions`
2. `AssignmentRuleDefinitionEntity`

Rule lifecycle:
1. `DRAFT`
2. `PUBLISHED`
3. `DEPRECATED`

Versioning:
1. Semantic version format required (`x.y.z`).

Schema:
1. `docs/schemas/assignment-rule-definition.schema.json`

### 7.2 Supported Rule Types

Current supported types:
1. `ALL_TO_ALL`
2. `ROUND_ROBIN`
3. `MANAGER_HIERARCHY`
4. `ATTRIBUTE_MATCH`

Note:
1. Execution still uses the proven dynamic engine strategies.
2. Control plane adds versioning/governance/explainability around execution.

### 7.3 Rule APIs

Admin endpoints (`ROLE_ADMIN`) under `/api/v1/admin/rules`:
1. Create rule definition.
2. Update draft rule.
3. List rule definitions.
4. Get single rule definition.
5. Create publish request.
6. Approve publish request.
7. Reject publish request.
8. Deprecate rule.
9. Simulate rule.
10. Publish assignments to a campaign.
11. Get capabilities.

### 7.4 Simulation and Explainability

Simulation returns:
1. Generated evaluator/evaluatee pairs.
2. Reason text for each match.
3. Exclusion diagnostics when diagnostic mode is on.

### 7.5 Quality Gates

Implemented tests:
1. Golden parity baseline test for rule outputs.
2. Benchmark latency/throughput gate for fixture-scale execution.
3. Service-level workflow tests.

---

## 8. Admin No-Code Governance (Phase 5)

### 8.1 Approval Workflow

Tables:
1. `assignment_rule_publish_requests`
2. `AssignmentRulePublishRequestEntity`

States:
1. `PENDING`
2. `APPROVED`
3. `REJECTED`

### 8.2 Guardrails

Config path:
1. `evaluation.service.admin.publish-lock-enabled`
2. `evaluation.service.admin.require-four-eyes-approval`

Behavior:
1. If publish lock is enabled, rule must be published before assignment publication.
2. If 4-eyes is enabled, requester cannot approve own publish request.

### 8.3 RBAC

Rule control-plane endpoints require:
1. `ROLE_ADMIN`

Implementation:
1. `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on rule controller.

### 8.4 Admin Audit Trail

Tables:
1. `admin_action_audit_logs`
2. `AdminActionAuditLogEntity`

Audit content:
1. Tenant.
2. Actor.
3. Action.
4. Aggregate type/id.
5. Reason/comment.
6. Structured payload.

Outbox propagation:
1. Admin actions also emit integration outbox events for downstream systems.

---

## 9. API Runbook (Operational Use)

### 9.1 Assignment Safety and Migration

1. Reconcile single campaign:
2. `GET /api/v1/campaigns/{id}/assignments/reconcile`
3. Reconcile aggregate report:
4. `GET /api/v1/campaigns/assignments/reconcile/report?maxCampaigns=1000`
5. Backfill:
6. `POST /api/v1/campaigns/assignments/backfill?dryRun=true&maxCampaigns=1000`

Recommended run order:
1. Run backfill in dry-run.
2. Run backfill with dry-run false.
3. Run parity report.
4. Validate inconsistency thresholds.
5. Keep scheduler running until stable.

### 9.2 Audience Ingestion

1. Ingest:
2. `POST /api/v1/audience/ingest`
3. List runs:
4. `GET /api/v1/audience/ingestion-runs?tenantId=...`
5. Get run:
6. `GET /api/v1/audience/ingestion-runs/{runId}`
7. Get rejections:
8. `GET /api/v1/audience/ingestion-runs/{runId}/rejections`
9. Replay:
10. `POST /api/v1/audience/ingestion-runs/{runId}/replay`

### 9.3 Mapping Profiles

1. Create/update/deactivate/list/get/validate endpoints under `/api/v1/audience/mapping-profiles`.
2. Events endpoint available for lifecycle audit visibility.

### 9.4 Rule Control Plane

1. Create draft.
2. Submit publish request.
3. Approve/reject by separate admin.
4. Run simulation.
5. Publish assignments to campaign.
6. Deprecate when superseded.

---

## 10. Configuration Runbook

Main config file:
1. `src/main/resources/application.yml`

Key config families:
1. Assignment migration:
2. `evaluation.service.assignment.storage-mode`
3. `evaluation.service.assignment.reconciliation-*`
4. Audience ingestion:
5. `evaluation.service.audience.jdbc.connections.*`
6. `evaluation.service.audience.validation-profiles.*`
7. `evaluation.service.audience.retention.*`
8. `evaluation.service.audience.outbox.*`
9. Admin governance:
10. `evaluation.service.admin.publish-lock-enabled`
11. `evaluation.service.admin.require-four-eyes-approval`

Security mode:
1. `evaluation.service.security.dev-mode=true` for local/dev unblock.
2. Must be false in production.

---

## 11. Monitoring and Operational Signals

Monitor continuously:
1. Assignment parity mismatch counts.
2. Backfill inserted/skipped/invalid counts.
3. Ingestion processed/rejected counts by source and tenant.
4. Rejection reason trends.
5. Outbox pending/retry/dead rates.
6. Retention cleanup execution metrics.
7. Rule simulation/publish frequency and failure rates.
8. Admin audit event volume and anomaly patterns.

Alert ideas:
1. Sudden spike in ingestion rejections.
2. Non-zero sustained dead-letter outbox events.
3. Rising assignment parity mismatches after cutover.
4. Repeated publish approval violations.

---

## 12. Troubleshooting Guide

### 12.1 Assignment mismatch

Symptoms:
1. Reconciliation shows only-in-legacy or only-in-relational.

Actions:
1. Run backfill dry-run.
2. Run backfill apply mode.
3. Re-run parity report.
4. Inspect malformed legacy JSON entries.

### 12.2 Ingestion failures

Symptoms:
1. Run status `FAILED` or high rejections.

Actions:
1. Pull rejection list by run ID.
2. Fix source payload/mapping profile.
3. Replay from snapshot.
4. Check validation profile strictness.

### 12.3 Rule publish blocked

Symptoms:
1. Publish requests cannot be approved or assignments cannot publish.

Actions:
1. Check rule status (`DRAFT/PUBLISHED`).
2. Check publish lock config.
3. Check 4-eyes constraint (approver must differ from requester).

### 12.4 Missing permissions

Symptoms:
1. `403` on admin rule endpoints.

Actions:
1. Confirm token has `ROLE_ADMIN`.
2. Confirm security mode is not dev-mode when testing RBAC.

---

## 13. Domain Onboarding Playbook (University/Hospital/NGO)

Same product flow for any domain:
1. Define tenant.
2. Configure connector source.
3. Create mapping profile for source fields to canonical fields.
4. Select validation profile (default/strict/custom).
5. Run dry ingestion and inspect rejections.
6. Run ingest and snapshot.
7. Create rule definitions with semantic versions.
8. Simulate and review explainability.
9. Request publish, approve, publish assignments.
10. Monitor metrics and audit logs.

Domain examples:
1. University:
2. Groups as sections/courses, memberships as student enrollments.
3. Hospital:
4. Groups as units/departments, memberships as staff rosters/rotations.
5. NGO:
6. Groups as programs/field teams, memberships as volunteer/beneficiary assignments.

---

## 14. Security and Governance Notes

Current state:
1. Auth service is external; this service enforces JWT and RBAC checks.
2. Dev mode bypass exists for local development.
3. Admin actions are auditable and emitted to outbox.

Production requirements:
1. Keep dev mode disabled.
2. Rotate secrets and tokens through platform standards.
3. Protect outbox transports and endpoints.
4. Review audit logs regularly.

---

## 15. Why This Design

Design choices and rationale:
1. Dual-write/read migration prevents risky big-bang cutovers.
2. Reconciliation/parity/backfill tooling enables measurable safety.
3. Canonical audience model decouples source systems from business rules.
4. Snapshot + replay allows deterministic reprocessing.
5. Policy-driven validation profiles enable multi-domain plug-and-play behavior.
6. Rule control plane enables governed no-code evolution.
7. Approval workflow + RBAC + audit logs provide production governance.
8. Outbox guarantees reliable integration delivery and traceability.

---

## 16. File Index (Key Artifacts)

Assignment migration and reliability:
1. `src/main/resources/db/migration/V5__campaign_assignments_table.sql`
2. `src/main/java/com/evaluationservice/infrastructure/service/AssignmentReconciliationService.java`
3. `src/main/java/com/evaluationservice/infrastructure/service/AssignmentParityReportService.java`
4. `src/main/java/com/evaluationservice/infrastructure/service/AssignmentBackfillService.java`

Audience platform:
1. `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
2. `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
3. `src/main/java/com/evaluationservice/infrastructure/service/AudienceIngestionSnapshotService.java`
4. `src/main/java/com/evaluationservice/infrastructure/service/AudienceMappingProfileService.java`
5. `src/main/java/com/evaluationservice/infrastructure/service/OutboxDispatchService.java`
6. `src/main/java/com/evaluationservice/infrastructure/scheduler/AudienceRetentionScheduler.java`

Rule control plane and admin governance:
1. `src/main/resources/db/migration/V13__rule_control_plane.sql`
2. `src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`
3. `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
4. `src/main/java/com/evaluationservice/infrastructure/service/AdminAuditLogService.java`
5. `docs/schemas/assignment-rule-definition.schema.json`

Test evidence:
1. `src/test/java/com/evaluationservice/infrastructure/migration/Phase1MigrationLowerEnvIntegrationTest.java`
2. `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
3. `src/test/java/com/evaluationservice/infrastructure/service/RuleControlPlaneServiceTest.java`
4. `src/test/java/com/evaluationservice/application/service/RuleDslGoldenParityTest.java`
5. `src/test/java/com/evaluationservice/application/service/RuleExecutionBenchmarkTest.java`

---

## 17. Exhaustive Constraint, Rule, Policy, Enum Reference

This section is the complete operational catalog for backend technical controls currently implemented.

Format:
1. What it is.
2. Use case.
3. How to use.

### 17.1 Domain Enums

#### `CampaignStatus` (`src/main/java/com/evaluationservice/domain/enums/CampaignStatus.java`)
1. `DRAFT`: campaign being configured.
2. `SCHEDULED`: campaign planned for future activation.
3. `ACTIVE`: evaluations are accepted.
4. `CLOSED`: submissions stopped.
5. `ARCHIVED`: historical only.

Use case:
1. Enforces lifecycle correctness and prevents invalid operations.

How to use:
1. Valid transitions only:
2. `DRAFT -> SCHEDULED|ACTIVE`
3. `SCHEDULED -> ACTIVE|DRAFT`
4. `ACTIVE -> CLOSED`
5. `CLOSED -> ARCHIVED`
6. `ARCHIVED ->` no transition.

#### `EvaluationStatus` (`src/main/java/com/evaluationservice/domain/enums/EvaluationStatus.java`)
1. `DRAFT`
2. `SUBMITTED`
3. `SCORING`
4. `COMPLETED`
5. `FLAGGED`
6. `INVALIDATED`

Use case:
1. Controls submission and scoring state machine.

How to use:
1. `submit()` is allowed only from `DRAFT`.
2. Terminal states are `COMPLETED` and `INVALIDATED` (`isTerminal()`).

#### `EvaluatorRole` (`src/main/java/com/evaluationservice/domain/enums/EvaluatorRole.java`)
1. `SELF`
2. `PEER`
3. `SUPERVISOR`
4. `SUBORDINATE`
5. `EXTERNAL`

Use case:
1. Labels relationship type for evaluator/evaluatee pairing and anonymization behavior.

How to use:
1. Pass in assignment generation config and assignment rows.

#### `QuestionType` (`src/main/java/com/evaluationservice/domain/enums/QuestionType.java`)
1. `SINGLE_CHOICE`
2. `MULTIPLE_CHOICE`
3. `LIKERT_SCALE`
4. `OPEN_TEXT`
5. `NUMERIC_RATING`
6. `BOOLEAN`
7. `MATRIX`
8. `RANKING`
9. `NPS`
10. `FILE_UPLOAD`

Use case:
1. Defines expected answer structure per question.

How to use:
1. For `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `RANKING`, `MATRIX`, provide options (`requiresOptions()`).

#### `ScoringMethod` (`src/main/java/com/evaluationservice/domain/enums/ScoringMethod.java`)
1. `WEIGHTED_AVERAGE`
2. `SIMPLE_AVERAGE`
3. `MEDIAN`
4. `PERCENTILE_RANK`
5. `CUSTOM_FORMULA`

Use case:
1. Selects calculation strategy for evaluation scores.

How to use:
1. Set in template or campaign.
2. For `CUSTOM_FORMULA`, provide template custom formula payload.

#### `TemplateStatus` (`src/main/java/com/evaluationservice/domain/enums/TemplateStatus.java`)
1. `DRAFT`
2. `PUBLISHED`
3. `DEPRECATED`
4. `ARCHIVED`

Use case:
1. Prevents editing published templates and controls campaign eligibility.

How to use:
1. New campaigns should use `PUBLISHED` templates only (`isUsableForNewCampaigns()`).

#### `SystemSettingCategory` (`src/main/java/com/evaluationservice/domain/enums/SystemSettingCategory.java`)
1. `SCORING`
2. `CAMPAIGN`
3. `NOTIFICATION`
4. `FEATURES`
5. `PAGINATION`

Use case:
1. Groups settings for storage/UI/API organization.

How to use:
1. Persist category with each system setting.

### 17.2 Config Enums and Controlled Value Sets

#### `AssignmentStorageMode` (`src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`)
1. `JSON`
2. `DUAL`
3. `V2`

Use case:
1. Assignment migration cutover control.

How to use:
1. Set `evaluation.service.assignment.storage-mode`.
2. `JSON` = legacy read path.
3. `DUAL` = compatibility mode during migration.
4. `V2` = relational source of truth.

#### Rule Control Plane Value Sets (`src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`)
1. Supported rule types:
2. `ALL_TO_ALL`
3. `ROUND_ROBIN`
4. `MANAGER_HIERARCHY`
5. `ATTRIBUTE_MATCH`
6. Supported audience source types:
7. `INLINE`
8. `DIRECTORY_SNAPSHOT`

Use case:
1. Prevent unsupported no-code configurations from entering runtime.

How to use:
1. Use only listed values in rule definitions and publish/simulate requests.

#### Ingestion Entity Type Set (`src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`)
1. `PERSON` / `PERSONS`
2. `GROUP` / `GROUPS`
3. `MEMBERSHIP` / `MEMBERSHIPS`

Use case:
1. Selects canonical table target during ingestion.

How to use:
1. Set `sourceConfig.entityType` in ingestion input.

#### Outbox Transport Values (`src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`)
1. `LOG`
2. `WEBHOOK`
3. `KAFKA`
4. `RABBITMQ`

Use case:
1. Controls event delivery channel for integration outbox dispatcher.

How to use:
1. Set `evaluation.service.audience.outbox.transport`.
2. Enable provider-specific settings under `.webhook`, `.kafka`, `.rabbitmq`.

#### String Status Sets (DB-persisted text statuses)
1. Ingestion run status: `RUNNING`, `SUCCEEDED`, `FAILED`.
2. Rule definition status: `DRAFT`, `PUBLISHED`, `DEPRECATED`.
3. Rule publish request status: `PENDING`, `APPROVED`, `REJECTED`.
4. Outbox status: `PENDING`, `PUBLISHED`, `FAILED`, `DEAD`.

Use case:
1. These are stored as strings in persistence and used by schedulers/dispatch logic.

How to use:
1. Keep producer and consumer expectations aligned with these exact uppercase values.

### 17.3 API Request Validation Constraints (DTO Layer)

Source: `src/main/java/com/evaluationservice/api/dto/request`.

Constraint types in use:
1. `@NotBlank`: required non-empty string.
2. `@NotNull`: required non-null object/list/map/date/enum.
3. `@NotEmpty`: required non-empty collection/map.
4. `@FutureOrPresent`: timestamp must be now or future.

Key request constraints:
1. Campaign create/update:
2. `name`, `templateId`, `startDate`, `endDate` required (create).
3. `name`, `startDate`, `endDate` required (update), and `endDate` must be `@FutureOrPresent`.
4. Evaluation submit:
5. `campaignId`, `assignmentId`, `evaluatorId`, `evaluateeId`, `templateId`, `answers` required.
6. Assignment generation:
7. `audienceSourceType`, `audienceSourceConfig`, `assignmentRuleType`, `assignmentRuleConfig` required.
8. Rule control plane:
9. Rule create/update requires `tenantId`, `name`, `semanticVersion`, `ruleType`, non-empty `ruleConfig`.
10. Publish/simulate requires `tenantId` and required body keys.
11. Audience ingestion:
12. `tenantId` and `sourceType` required.
13. Mapping profiles:
14. `tenantId`/`name`/`sourceType` plus non-empty field mappings where applicable.
15. Auth login:
16. `username`, `password` required.

Use case:
1. Reject structurally invalid payloads before business logic.

How to use:
1. Client must satisfy DTO constraints or receives `400`.

### 17.4 Domain and Service-Level Technical Constraints

#### Campaign Aggregate Rules (`src/main/java/com/evaluationservice/domain/entity/Campaign.java`)
1. Campaign name cannot be blank.
2. `minimumRespondents` is clamped to at least `1`.
3. Assignment add/replace/config update allowed only in `DRAFT` or `SCHEDULED`.
4. Campaign update allowed only in `DRAFT` or `SCHEDULED`.
5. Deadline extension allowed only in `ACTIVE`.

Use case:
1. Stops mid-flight mutation that would invalidate evaluation integrity.

How to use:
1. Finalize assignment/rule configuration before activation.

#### Template Aggregate Rules (`src/main/java/com/evaluationservice/domain/entity/Template.java`)
1. Template name cannot be blank.
2. Publish requires at least one section.
3. Publish requires at least one question.
4. Most mutations require `DRAFT` state.
5. Deprecation allowed only for `PUBLISHED`.

Use case:
1. Prevents unusable or partially configured templates from production use.

How to use:
1. Complete sections/questions first, then publish.

#### Evaluation Aggregate Rules (`src/main/java/com/evaluationservice/domain/entity/Evaluation.java`)
1. Cannot modify terminal (`COMPLETED`/`INVALIDATED`) evaluation.
2. Submit only from `DRAFT`.
3. Submit requires at least one answer.

Use case:
1. Ensures one-way submission semantics and audit integrity.

How to use:
1. Save draft first, then submit once complete.

#### Value Object Constraints (`src/main/java/com/evaluationservice/domain/value`)
1. `DateRange`: `endDate` cannot be before `startDate`.
2. `Weight`: must be `> 0` and `<= 1`.
3. `Score`: null disallowed; divide-by-zero blocked in `divide`.
4. IDs (`CampaignId`, `TemplateId`, `EvaluationId`) cannot be null/blank.

Use case:
1. Centralized invariant enforcement across domain behavior.

How to use:
1. Construct through value objects, not raw primitives in core domain logic.

### 17.5 Dynamic Assignment Engine Rule Constraints

Source: `src/main/java/com/evaluationservice/application/service/DynamicAssignmentEngine.java`.

Global constraints:
1. Supported audience source types: `INLINE`, `DIRECTORY_SNAPSHOT`.
2. `audienceSourceConfig.participants` must be an array.
3. Every participant must have `userId` or `id`.
4. Unsupported `assignmentRuleType` fails.

Common participant keys:
1. Reserved keys: `userId`, `id`, `supervisorId`, `managerId`, `attributes`.
2. Non-reserved keys become participant attributes automatically.

Rule-specific config:
1. `ALL_TO_ALL`:
2. `evaluatorRole` (default `PEER`)
3. `allowSelfEvaluation` (default `false`)
4. `maxEvaluatorsPerEvaluatee` (default unlimited)
5. `ROUND_ROBIN`:
6. `evaluatorRole` (default `PEER`)
7. `allowSelfEvaluation` (default `false`)
8. `evaluatorsPerEvaluatee` (default `1`, min effective `1`)
9. `MANAGER_HIERARCHY`:
10. `evaluatorRole` (default `SUPERVISOR`)
11. `includeSelfEvaluation` (default `false`)
12. `requireKnownManager` (default `true`)
13. `ATTRIBUTE_MATCH`:
14. `matchAttribute` (default `department`)
15. `evaluatorRole` (default `PEER`)
16. `allowSelfEvaluation` (default `false`)
17. `maxEvaluatorsPerEvaluatee` (default `3`, min effective `1`)

Use case:
1. Makes evaluator assignment domain-specific without code change.

How to use:
1. Keep a consistent participant schema.
2. Choose rule type based on org structure:
3. Hierarchy-based org: `MANAGER_HIERARCHY`.
4. Cohort/section/team based org: `ATTRIBUTE_MATCH` with `matchAttribute=section|unit|team`.
5. Broad peer review: `ROUND_ROBIN` or `ALL_TO_ALL`.

### 17.6 Rule Control Plane Policies and Constraints

Source: `src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`.

Constraints:
1. `tenantId` required and must exist.
2. `semanticVersion` must match `x.y.z` regex (`^\\d+\\.\\d+\\.\\d+$`).
3. `ruleType` must be in supported set.
4. `ruleConfig` must be non-empty.
5. Only `DRAFT` rule definitions can be updated.
6. Only `DRAFT` rule definitions can request publish.
7. Only one `PENDING` publish request per rule definition.
8. Approve/reject only when request is `PENDING`.
9. 4-eyes policy: approver cannot equal requester when enabled.
10. Publish lock policy: assignments can be published only from `PUBLISHED` rules when enabled.

Use case:
1. Enterprise governance, safe approvals, and repeatable no-code rule release.

How to use:
1. Flow:
2. Create draft -> update draft -> request publish -> approve by second admin -> publish assignments.

### 17.7 Audience Ingestion Validation Policies

Source: `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`.

Base constraints:
1. `tenantId` required and must exist.
2. `sourceType` required and connector must exist.
3. Source must return at least one record.
4. Snapshot replay requires existing run snapshot and valid tenant.

Person record constraints:
1. `person_id` required.
2. `person_id` regex: `^[A-Za-z0-9._:@-]{1,128}$`.
3. `display_name` required if profile says so.
4. `display_name` min length from profile.
5. `email` required if profile says so.
6. Email format regex check + max length `320`.
7. Allowed email domains check if configured.
8. `active` values allowed: `true,false,1,0,yes,no,y,n` (default true if blank).

Group record constraints:
1. `group_id` required and same ID pattern enforcement.
2. `name` required.
3. `group_type` required.
4. `group_type` must be in allowed list if configured.
5. `external_ref` required if profile says so.
6. `active` value policy same as person.

Membership record constraints:
1. `person_id`, `group_id` required.
2. `membership_role` required if profile says so.
3. Duplicate membership rows in single source payload are rejected.
4. Person/group must exist.
5. Active person/group required if profile says so.
6. Role-to-group-type compatibility enforced if configured.
7. `valid_from`/`valid_to` ISO-8601 parsing.
8. If profile requires validity window, both values must exist.
9. `valid_to` cannot be before `valid_from`.
10. `active` value policy same as person/group.

Validation profile resolution policy:
1. Requested profile key lookup is case-tolerant.
2. Fallback chain: exact -> lowercase -> uppercase -> `default` -> `DEFAULT` -> built-in permissive defaults.

Use case:
1. Organization-specific data quality enforcement without code changes.

How to use:
1. Configure `evaluation.service.audience.validation-profiles.<profileName>.*`.
2. Send `sourceConfig.validationProfile=<profileName>` in ingest request.

### 17.8 Security and Authorization Policies

Sources:
1. `src/main/java/com/evaluationservice/infrastructure/config/SecurityConfig.java`
2. `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`

Policies:
1. `dev-mode=true` permits all requests (local development only).
2. Non-dev mode requires authentication for most APIs.
3. Publicly permitted routes include auth/public/docs/health/info/prometheus.
4. Rule control plane requires `ROLE_ADMIN` via method-level `@PreAuthorize`.

Use case:
1. Keeps development unblocked while enforcing production RBAC.

How to use:
1. Set `evaluation.service.security.dev-mode=false` in all production environments.
2. Ensure admin tokens carry `ROLE_ADMIN` for rule governance endpoints.

### 17.9 Database Constraints (Schema-Level)

Sources: `src/main/resources/db/migration/V1...V13`.

Implemented classes of constraints:
1. Primary keys on all core tables.
2. Foreign key referential integrity (with `ON DELETE CASCADE` where designed).
3. Unique constraints preventing duplication.
4. Explicit check constraint on campaign date ordering.
5. Not-null constraints on required persisted fields.

Critical uniqueness constraints:
1. `campaign_assignments`: unique `(campaign_id, evaluator_id, evaluatee_id, evaluator_role)`.
2. `evaluations`: unique `assignment_id`.
3. `audience_memberships`: unique `(tenant_id, person_id, group_id, membership_role)`.
4. `audience_relations`: unique `(tenant_id, source_person_id, target_person_id, relation_type)`.
5. `audience_attributes`: unique `(tenant_id, subject_type, subject_id, attribute_key)`.
6. `audience_mapping_profiles`: unique `(tenant_id, source_type, name)`.
7. `assignment_rule_definitions`: unique `(tenant_id, name, semantic_version)`.
8. RBAC role-permission mapping unique `(role_id, permission_id)`.

Critical check constraints:
1. `campaigns`: `end_date > start_date`.

Use case:
1. Hard guarantees against data corruption and race-condition duplicates.

How to use:
1. Treat DB constraints as authoritative invariants; service logic should align with them.

### 17.10 Operational Policy Controls (Config Keys)

Source: `src/main/resources/application.yml` and `EvaluationServiceProperties`.

Major policy controls:
1. `evaluation.service.assignment.*`: migration read mode, reconciliation scheduler, scan limits.
2. `evaluation.service.audience.validation-profiles.*`: ingest strictness by profile.
3. `evaluation.service.audience.retention.*`: TTL and cleanup scheduling.
4. `evaluation.service.audience.outbox.*`: retry/backoff/batch/max-attempts/transport settings.
5. `evaluation.service.admin.publish-lock-enabled`: publish gating.
6. `evaluation.service.admin.require-four-eyes-approval`: dual-control enforcement.
7. `evaluation.service.security.dev-mode`: local auth bypass.

Use case:
1. No-code operational behavior changes.

How to use:
1. Change via environment-specific config management.
2. Validate changes first in lower environment with reconciliation/simulation endpoints.
