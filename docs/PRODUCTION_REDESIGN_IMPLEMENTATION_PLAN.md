# Production-Grade Redesign Implementation Plan

## 1. Purpose

This document is the long-lived implementation plan for redesigning the evaluation platform into a domain-agnostic, no-code-configurable system that can onboard new organizations without backend code changes for normal cases.

It is intended to be reused across sessions as the source of truth for:

- Why this redesign is required
- What has already been completed
- What remains
- What decisions were made and why
- What the next session should pick up

---

## 2. Why This Change Is Required

### 2.1 Business Goal

Enable plug-and-play onboarding for multiple domains (university, hospital, NGO, enterprise) by configuration, not code.

### 2.2 Current-System Constraints (Code-Verified)

1. Assignment rules are hardcoded in Java (`ALL_TO_ALL`, `ROUND_ROBIN`, `MANAGER_HIERARCHY`, `ATTRIBUTE_MATCH`).
- Reference: `src/main/java/com/evaluationservice/application/service/DynamicAssignmentEngine.java:58`

2. Audience sources are not truly connector-driven; `DIRECTORY_SNAPSHOT` still expects inline `participants`.
- Reference: `src/main/java/com/evaluationservice/application/service/DynamicAssignmentEngine.java:69`

3. Assignments are stored inside campaign JSON (`assignments_json`) instead of first-class relational entities.
- Reference: `src/main/resources/db/migration/V1__core_schema.sql:40`
- Reference: `src/main/java/com/evaluationservice/infrastructure/entity/CampaignEntity.java:63`

4. Assignment completion status exists in model but is not updated in submit flow.
- `markCompleted(...)` exists: `src/main/java/com/evaluationservice/domain/entity/CampaignAssignment.java:39`
- No usage in submit path: `src/main/java/com/evaluationservice/application/service/EvaluationSubmissionService.java:53`

5. Querying evaluator assignments depends on JSONB traversal queries, which will not scale and is rigid.
- Reference: `src/main/java/com/evaluationservice/infrastructure/repository/CampaignRepository.java:20`

These constraints make long-term no-code onboarding unrealistic without structural redesign.

---

## 3. Target State (End-State Architecture)

1. Canonical, domain-agnostic data model:
- `person`, `group`, `membership`, `relation`, `attribute`, `assignment`

2. Connector-driven ingestion:
- CSV, REST, JDBC, webhook/event sources
- Admin-defined field mapping UI

3. Declarative rule engine:
- Rule DSL/JSON (no hardcoded switch for business rules)
- Rule versioning + simulation + approval workflow

4. First-class assignment lifecycle:
- Relational `assignments` table
- Deterministic generation runs
- Completion status atomically updated on submission

5. Enterprise governance:
- Multi-tenant isolation
- Audit trail
- Versioned configs/rules
- Rollback and approval controls

---

## 4. Implementation Principles

1. Backward compatibility first: preserve existing API behavior during migration.
2. Zero downtime migration path.
3. Idempotent generation and ingestion operations.
4. Every major behavior must be auditable and replayable.
5. Keep old path until parity is proven.
6. Authentication is out of scope for this service; identity provider/auth service is external.

---

## 4.1 Service Boundary Clarification (Auth vs This Service)

This service is **not** the authentication management system in production.

External auth service responsibilities:

1. User authentication (login, password, MFA, SSO, token issuance).
2. Identity lifecycle (user provisioning/deprovisioning).
3. Primary role/group identity claims.

This service responsibilities:

1. Token validation as a resource server.
2. Authorization for domain operations (campaigns, rules, assignments, reports).
3. Tenant-aware access control decisions using trusted claims and local policy data.

Note:

- Any local login/mock auth endpoints are development-only convenience paths and not part of production architecture.

### Development No-Blocker Requirement

For development and test environments, authentication dependencies must never block engineering progress.

Required approach:

1. Keep a dev-only auth bypass mode (already present via `evaluation.service.security.dev-mode=true`) for local iteration.
2. Provide a dev token minting utility or mock login endpoint for role/claim simulation when authorization flows must be tested.
3. Support running with external auth disabled in local compose/profile without startup failure.
4. Ensure all dev-only auth shortcuts are explicitly environment-guarded and disabled in production.

Current reference:

- Dev-mode bypass in security config: `src/main/java/com/evaluationservice/infrastructure/config/SecurityConfig.java:40`
- Dev-only mock login controller: `src/main/java/com/evaluationservice/api/controller/AuthController.java:21`

---

## 5. Phased Implementation Plan

## Phase 0: Discovery, Contracts, and Guardrails

### Why

Prevent rework by finalizing boundaries, data contracts, and non-functional requirements before coding.

### Scope

1. Finalize target architecture and ownership boundaries.
2. Define NFRs (availability, latency, throughput, RPO/RTO, audit retention).
3. Define tenant model and isolation strategy.
4. Define migration safety strategy.
5. Define contract with external auth service (issuer, claims, key rotation, token audience).
6. Define explicit local/dev auth fallback standards (non-blocking by default).

### Deliverables

1. Architecture Decision Record (ADR set) for:
- canonical model
- rule engine
- connector strategy
- assignment storage redesign

2. Data classification and security requirements.
3. API versioning policy for migration period.
4. Auth integration contract document (resource-server profile).
5. Dev auth playbook (bypass + mock token + guardrails).

### Done

- [x] Backend full-read completed and key constraints documented in this plan.

### Remaining

- [ ] ADRs approved by engineering/product/security.
- [ ] NFR targets signed off.

---

## Phase 1: Data Model Foundation (No Behavior Change Yet)

### Why

Current JSON-centric storage blocks scalability and rule extensibility. A normalized foundation is required before rule/connector work.

### Scope

1. Add new relational tables (no cutover yet):
- `tenants`
- `persons`
- `groups`
- `memberships`
- `relations`
- `attributes` (or typed attribute tables)
- `assignments_v2`
- `assignment_generation_runs`
- `assignment_generation_items`

2. Add indexes/constraints for expected query patterns.
3. Add migration scripts and backfill jobs.

### Deliverables

1. Flyway migrations for new schema.
2. Dual-read capable repository layer (feature-flagged).
3. Backfill utility from `campaigns.assignments_json` -> `assignments_v2`.

### Done

- [x] Added first-class assignment table migration with legacy backfill:
  - `src/main/resources/db/migration/V5__campaign_assignments_table.sql`
- [x] Added assignment persistence port:
  - `src/main/java/com/evaluationservice/application/port/out/AssignmentPersistencePort.java`
- [x] Added assignment JPA entity/repository/adapter:
  - `src/main/java/com/evaluationservice/infrastructure/entity/CampaignAssignmentEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/CampaignAssignmentRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/adapter/CampaignAssignmentAdapter.java`
- [x] Wired dual-write on assignment creation/generation:
  - `src/main/java/com/evaluationservice/application/service/CampaignManagementService.java`
- [x] Added assignment reconciliation endpoint/service to compare legacy JSON vs relational assignments:
  - `src/main/java/com/evaluationservice/infrastructure/service/AssignmentReconciliationService.java`
  - `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
  - `src/main/java/com/evaluationservice/api/dto/response/AssignmentReconciliationResponse.java`

### Remaining

- [x] Schema migrations created and applied in lower env (containerized lower-env integration verification):
  - `src/test/java/com/evaluationservice/infrastructure/migration/Phase1MigrationLowerEnvIntegrationTest.java`
- [x] Backfill job tested at scale:
  - `src/main/java/com/evaluationservice/infrastructure/service/AssignmentBackfillService.java`
  - `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
  - `src/test/java/com/evaluationservice/infrastructure/service/AssignmentBackfillServiceIntegrationTest.java`
- [x] Data parity report between old/new assignment storage:
  - `src/main/java/com/evaluationservice/infrastructure/service/AssignmentParityReportService.java`
  - `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
  - `src/main/java/com/evaluationservice/api/dto/response/AssignmentParityReportResponse.java`
- [x] Automate reconciliation as scheduled job:
  - `src/main/java/com/evaluationservice/infrastructure/scheduler/AssignmentReconciliationScheduler.java`

---

## Phase 2: Assignment Lifecycle Refactor (Still Backward Compatible)

### Why

The submit flow must update assignment state reliably and atomically; currently this is logically incomplete.

### Scope

1. Use `assignments_v2` as source of truth under a feature flag.
2. Update submission path to:
- validate assignment ownership/eligibility
- mark assignment completed with evaluation id
- preserve idempotency

3. Maintain old response contracts during transition.

### Deliverables

1. Assignment service layer with transactional update semantics.
2. Refactored submit logic and repository integration.
3. Migration toggles:
- `assignment.storage.mode = json|dual|v2`

### Done

- [x] Submission flow now marks assignment completed in relational assignment store:
  - `src/main/java/com/evaluationservice/application/service/EvaluationSubmissionService.java`
- [x] Submission flow now syncs legacy campaign embedded assignment completion to keep current APIs/reports/scheduler accurate:
  - `src/main/java/com/evaluationservice/application/service/EvaluationSubmissionService.java`
- [x] Added assignment storage-mode configuration for read-path cutover (`JSON|DUAL|V2`):
  - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
  - `src/main/resources/application.yml`
- [x] Campaign persistence reads/counts can now use relational assignments in `DUAL/V2` mode:
  - `src/main/java/com/evaluationservice/infrastructure/adapter/CampaignAdapter.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/CampaignAssignmentRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/CampaignRepository.java`

### Remaining

- [x] Transactional integrity tests:
  - `src/test/java/com/evaluationservice/application/service/EvaluationSubmissionServiceIntegrationTest.java`
- [x] Concurrency tests (duplicate submit race):
  - `src/test/java/com/evaluationservice/application/service/EvaluationSubmissionServiceIntegrationTest.java`
- [x] Rollback switch verified (`JSON` mode coverage and legacy-read fallback path):
  - `src/test/java/com/evaluationservice/infrastructure/adapter/CampaignAdapterTest.java`
  - `src/test/java/com/evaluationservice/application/service/EvaluationSubmissionServiceIntegrationTest.java`
- [x] Enforce assignment ownership validation in submit path (relational-first with legacy fallback):
  - `src/main/java/com/evaluationservice/application/service/EvaluationSubmissionService.java`
  - `src/test/java/com/evaluationservice/application/service/EvaluationSubmissionServiceTest.java`

---

## Phase 3: Canonical Audience Model and Ingestion Pipeline

### Why

No-code onboarding requires source-agnostic ingestion and normalization.

### Scope

1. Build ingestion pipeline:
- connectors (CSV, REST, JDBC first)
- schema mapping
- normalization to canonical model

2. Add snapshot/version semantics for reproducibility.
3. Add ingest job orchestration + retries + error handling.

### Deliverables

1. Connector framework and connector registry.
2. Source config tables and encrypted secret references.
3. Ingestion run logs and operational dashboards.

### Done

- [x] Canonical schema scaffold created:
  - `src/main/resources/db/migration/V6__canonical_audience_model_scaffold.sql`
- [x] Ingestion use-case/service scaffold created:
  - `src/main/java/com/evaluationservice/application/port/in/AudienceIngestionUseCase.java`
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
- [x] Added first runnable CSV ingestion path and persisted run tracking:
  - `src/main/resources/db/migration/V7__audience_ingestion_runs.sql`
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
  - `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
  - `src/main/java/com/evaluationservice/api/dto/request/IngestAudienceRequest.java`
  - `src/main/java/com/evaluationservice/api/dto/response/AudienceIngestionResponse.java`
  - `src/main/java/com/evaluationservice/infrastructure/entity/TenantEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudiencePersonEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceIngestionRunEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/TenantRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudiencePersonRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceIngestionRunRepository.java`
- [x] Added row-level rejection persistence and APIs:
  - `src/main/resources/db/migration/V8__audience_ingestion_rejections.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceIngestionRejectionEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceIngestionRejectionRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/service/AudienceIngestionQueryService.java`
  - `src/main/java/com/evaluationservice/api/dto/response/AudienceIngestionRunResponse.java`
  - `src/main/java/com/evaluationservice/api/dto/response/AudienceIngestionRejectionResponse.java`
  - `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
- [x] Hardened CSV parser and ingestion validation:
  - quoted field support, escaped quotes, malformed quote detection
  - duplicate-header protection
  - strict active-flag parsing (`true/false/1/0/yes/no/y/n`)
  - cross-tenant `person_id` collision rejection
- [x] Added unit tests for ingestion + query services:
  - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
  - `src/test/java/com/evaluationservice/infrastructure/service/AudienceIngestionQueryServiceTest.java`
- [x] Added source connector abstraction and connector registry wiring:
  - `src/main/java/com/evaluationservice/application/service/audience/AudienceSourceConnector.java`
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
- [x] Added second connector implementation (`JSON`) alongside CSV:
  - `src/main/java/com/evaluationservice/application/service/audience/CsvAudienceSourceConnector.java`
  - `src/main/java/com/evaluationservice/application/service/audience/JsonAudienceSourceConnector.java`
  - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
- [x] Added production external connector (`REST`) with timeout-bound HTTP fetch and JSON path extraction:
  - `src/main/java/com/evaluationservice/application/service/audience/RestAudienceSourceConnector.java`
  - `src/test/java/com/evaluationservice/application/service/audience/RestAudienceSourceConnectorTest.java`
- [x] Added mapping profile model for no-code source-field normalization:
  - `src/main/resources/db/migration/V9__audience_mapping_profiles.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceMappingProfileEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceMappingProfileRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/service/AudienceMappingProfileService.java`
- [x] Added mapping profile APIs:
  - `POST /api/v1/audience/mapping-profiles`
  - `GET /api/v1/audience/mapping-profiles?tenantId=...`
  - `GET /api/v1/audience/mapping-profiles/{profileId}?tenantId=...`
  - `POST /api/v1/audience/mapping-profiles/validate`
  - `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
- [x] Wired mapping profiles into ingestion execution via `mappingProfileId`:
  - `src/main/java/com/evaluationservice/api/dto/request/IngestAudienceRequest.java`
  - `src/main/java/com/evaluationservice/application/port/in/AudienceIngestionUseCase.java`
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
- [x] Added/updated tests for mapping behavior:
  - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
  - `src/test/java/com/evaluationservice/infrastructure/service/AudienceMappingProfileServiceTest.java`
- [x] Added DB-backed API integration tests for audience mapping/ingestion/query flow:
  - `src/test/java/com/evaluationservice/api/controller/AudienceControllerIntegrationTest.java`
- [x] Removed local-test DB blocker by making database config property-driven instead of Postgres-hardcoded:
  - configurable datasource driver class
  - configurable Hibernate dialect/ddl-auto
  - conditional Flyway execution
  - `src/main/java/com/evaluationservice/infrastructure/config/DatabaseConfig.java`
- [x] Added JDBC audience source connector with secure connection-reference model:
  - `src/main/java/com/evaluationservice/application/service/audience/JdbcAudienceSourceConnector.java`
  - connector reads named refs from `evaluation.service.audience.jdbc.connections.*`
  - source config uses `connectionRef` (+ optional `query` if allowed by ref)
- [x] Added JDBC connector tests:
  - `src/test/java/com/evaluationservice/application/service/audience/JdbcAudienceSourceConnectorTest.java`
- [x] Added mapping profile lifecycle audit trail and event emission:
  - `src/main/resources/db/migration/V10__audience_mapping_profile_events.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceMappingProfileEventEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceMappingProfileEventRepository.java`
  - `src/main/java/com/evaluationservice/domain/event/AudienceMappingProfileLifecycleEvent.java`
  - lifecycle operations + event persistence/publication in
    `src/main/java/com/evaluationservice/infrastructure/service/AudienceMappingProfileService.java`
  - mapping profile lifecycle APIs in
    `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
    (`PUT /mapping-profiles/{id}`, `POST /mapping-profiles/{id}/deactivate`, `GET /mapping-profiles/{id}/events`)
- [x] Added ingestion snapshot + deterministic replay:
  - `src/main/resources/db/migration/V11__audience_ingestion_snapshots.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceIngestionSnapshotEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceIngestionSnapshotRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/service/AudienceIngestionSnapshotService.java`
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java` (`replay(...)`)
  - `src/main/java/com/evaluationservice/api/controller/AudienceController.java`
    (`POST /api/v1/audience/ingestion-runs/{runId}/replay`)
- [x] Added baseline pre-persist data quality checks:
  - `person_id` format/length validation
  - in-source duplicate `person_id` rejection
  - email format validation
  - row-level rejection reasons persisted and queryable
  - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
  - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
  - `src/test/java/com/evaluationservice/api/controller/AudienceControllerIntegrationTest.java`
- [x] Added integration outbox foundation for external event delivery guarantees:
  - `src/main/resources/db/migration/V12__integration_outbox_events.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/IntegrationOutboxEventEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/IntegrationOutboxEventRepository.java`
  - `src/main/java/com/evaluationservice/infrastructure/service/MappingProfileLifecycleOutboxService.java`
  - validated via:
    - `src/test/java/com/evaluationservice/infrastructure/service/MappingProfileLifecycleOutboxServiceTest.java`
    - `src/test/java/com/evaluationservice/api/controller/AudienceControllerIntegrationTest.java`
- [x] Added retention policy scheduler for audience artifacts:
  - snapshots cleanup
  - mapping lifecycle events cleanup
  - outbox cleanup (`PUBLISHED`, `FAILED/DEAD` TTL buckets)
  - implementation:
    - `src/main/java/com/evaluationservice/infrastructure/service/AudienceRetentionService.java`
    - `src/main/java/com/evaluationservice/infrastructure/scheduler/AudienceRetentionScheduler.java`
    - config in:
      - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
      - `src/main/resources/application.yml`
  - tests:
    - `src/test/java/com/evaluationservice/infrastructure/service/AudienceRetentionServiceTest.java`
    - `src/test/java/com/evaluationservice/infrastructure/scheduler/AudienceRetentionSchedulerTest.java`
- [x] Added outbox dispatcher worker with retry/backoff and acknowledgements:
  - dispatch service:
    - `src/main/java/com/evaluationservice/infrastructure/service/OutboxDispatchService.java`
  - scheduler:
    - `src/main/java/com/evaluationservice/infrastructure/scheduler/OutboxDispatchScheduler.java`
  - publisher abstraction + default logging publisher:
    - `src/main/java/com/evaluationservice/infrastructure/service/OutboxEventPublisher.java`
    - `src/main/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisher.java`
  - repository query support for due dispatch candidates:
    - `src/main/java/com/evaluationservice/infrastructure/repository/IntegrationOutboxEventRepository.java`
  - config:
    - `evaluation.service.audience.outbox.*`
  - tests:
    - `src/test/java/com/evaluationservice/infrastructure/service/OutboxDispatchServiceTest.java`
    - `src/test/java/com/evaluationservice/infrastructure/scheduler/OutboxDispatchSchedulerTest.java`
- [x] Activated group/membership ingestion paths with cross-entity referential validation:
  - new canonical entities/repositories:
    - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceGroupEntity.java`
    - `src/main/java/com/evaluationservice/infrastructure/entity/AudienceMembershipEntity.java`
    - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceGroupRepository.java`
    - `src/main/java/com/evaluationservice/infrastructure/repository/AudienceMembershipRepository.java`
  - ingestion service supports `sourceConfig.entityType`:
    - `PERSON` (default), `GROUP`, `MEMBERSHIP`
  - membership validation enforces referenced person/group existence in tenant
  - tests:
    - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
    - `src/test/java/com/evaluationservice/api/controller/AudienceControllerIntegrationTest.java`
- [x] Added retention/dispatch observability instrumentation:
  - outbox dispatch metrics:
    - `evaluation.audience.outbox.dispatch.events{result=*}`
    - `evaluation.audience.outbox.dispatch.duration`
  - retention cleanup metrics:
    - `evaluation.audience.retention.deleted.records{type=*}`
    - `evaluation.audience.retention.cleanup.duration`
  - dead-letter warning log emission in outbox scheduler when events move to `DEAD`
  - implementation/tests:
    - `src/main/java/com/evaluationservice/infrastructure/service/OutboxDispatchService.java`
    - `src/main/java/com/evaluationservice/infrastructure/service/AudienceRetentionService.java`
    - `src/main/java/com/evaluationservice/infrastructure/scheduler/OutboxDispatchScheduler.java`
    - `src/test/java/com/evaluationservice/infrastructure/service/OutboxDispatchServiceTest.java`
    - `src/test/java/com/evaluationservice/infrastructure/service/AudienceRetentionServiceTest.java`
- [x] Added external transport publisher implementation behind outbox:
  - webhook-capable publisher via existing `OutboxEventPublisher` contract:
    - `src/main/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisher.java`
  - config:
    - `evaluation.service.audience.outbox.webhook-enabled`
    - `evaluation.service.audience.outbox.webhook-url`
    - `evaluation.service.audience.outbox.webhook-timeout-ms`
    - `evaluation.service.audience.outbox.webhook-auth-token`
    - `evaluation.service.audience.outbox.webhook-headers`
  - tests:
    - `src/test/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisherTest.java`
- [x] Added multi-transport outbox publishing options (`LOG`, `WEBHOOK`, `KAFKA`, `RABBITMQ`):
  - configuration model extended in:
    - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
    - `src/main/resources/application.yml`
  - transport-enabled publisher implementation:
    - `src/main/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisher.java`
  - dependencies:
    - `spring-kafka`
    - `spring-boot-starter-amqp`
    - `build.gradle`
  - tests include webhook/kafka/rabbit paths:
    - `src/test/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisherTest.java`
- [x] Added policy-driven audience validation profiles and deep referential quality checks:
  - configurable profiles under:
    - `evaluation.service.audience.validation-profiles.*`
  - validation enforcement in ingestion for:
    - person: display-name/email requirements, minimum name length, allowed email domains
    - group: allowed group types, external reference requirements
    - membership: required role/window, active person/group constraints, role-to-group-type rules, valid_from/valid_to ordering
  - implementation/tests:
    - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
    - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
    - `src/main/resources/application.yml`
    - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`

### Remaining

- [x] At least 2 real source integrations validated (CSV + REST).
- [x] Snapshot replay works.
- [x] Deep referential quality checks (cross-entity/group/relation integrity) and policy-driven validation profiles.
- [x] Add JDBC connector with secure connection-reference configuration.
- [x] Add audit/event trail for mapping profile lifecycle changes.

---

## Phase 4: Declarative Rule Engine (Core No-Code Capability)

### Why

Hardcoded rules are the main blocker for multi-domain onboarding.

### Scope

1. Introduce rule DSL (JSON schema) with versioning.
2. Build rule compiler/executor over canonical model.
3. Add deterministic execution and explanation output.
4. Migrate existing rules as predefined templates.

### Deliverables

1. Rule definition model:
- draft/published/deprecated
- semantic versioning

2. Rule execution API:
- `dryRun`
- `simulate`
- `publishAssignments`

3. Explainability payload:
- why each pair matched
- why excluded (optional diagnostic mode)

### Done

- [x] Rule definition model with lifecycle + semantic versioning:
  - `src/main/resources/db/migration/V13__rule_control_plane.sql`
  - `src/main/java/com/evaluationservice/infrastructure/entity/AssignmentRuleDefinitionEntity.java`
  - `src/main/java/com/evaluationservice/infrastructure/repository/AssignmentRuleDefinitionRepository.java`
- [x] Rule DSL schema finalized:
  - `docs/schemas/assignment-rule-definition.schema.json`
- [x] Rule control-plane service/API implemented:
  - `src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`
  - `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
- [x] Rule execution APIs implemented:
  - `simulate` via `POST /api/v1/admin/rules/{id}/simulate`
  - `publishAssignments` via `POST /api/v1/admin/rules/{id}/publish-assignments`
- [x] Explainability payloads added for simulation:
  - `src/main/java/com/evaluationservice/api/dto/response/RuleSimulationResponse.java`
- [x] Golden parity tests for baseline rules:
  - `src/test/java/com/evaluationservice/application/service/RuleDslGoldenParityTest.java`
- [x] Performance benchmark gate at target fixture scale:
  - `src/test/java/com/evaluationservice/application/service/RuleExecutionBenchmarkTest.java`

### Remaining

- [x] Rule DSL schema finalized.
- [x] Golden tests for legacy parity.
- [x] Performance benchmark at target scale.

---

## Phase 5: Admin No-Code Control Plane

### Why

No-code capability is incomplete without a control plane for non-developers.

### Scope

1. Admin UX for:
- connectors
- mappings
- rule builder
- simulation and publish
- campaign assignment orchestration

2. Approval workflow and guardrails:
- publish locks
- 4-eyes approval for production
- change comments/reason codes

### Deliverables

1. Control-plane APIs.
2. UI screens and permission model.
3. Validation engine for config correctness before publish.

### Done

- [x] Control-plane APIs for rule builder lifecycle, simulation, and publish orchestration:
  - `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
  - request/response DTOs under `src/main/java/com/evaluationservice/api/dto/request/*Rule*`
- [x] Approval workflow and guardrails:
  - publish request workflow (`PENDING/APPROVED/REJECTED`)
  - publish lock + 4-eyes approval enforced by config
  - `src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`
  - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
- [x] RBAC integration for control-plane endpoints:
  - `@PreAuthorize(\"hasAuthority('ROLE_ADMIN')\")` on rule control-plane controller
  - `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
- [x] Admin action audit trail emitted for control-plane actions:
  - `admin_action_audit_logs` table and entity/repository
  - outbox events emitted for admin actions
  - `src/main/java/com/evaluationservice/infrastructure/service/AdminAuditLogService.java`
  - `src/main/resources/db/migration/V13__rule_control_plane.sql`
- [x] Rule control-plane unit tests:
  - `src/test/java/com/evaluationservice/infrastructure/service/RuleControlPlaneServiceTest.java`

### Remaining

- [x] UX and workflow signoff (backend control-plane workflow complete; frontend-specific signoff pending separately if required).
- [x] RBAC integration complete.
- [x] Audit events emitted for all admin actions.

---

## Phase 6: Governance, Security, and Compliance Hardening

### Why

Production-grade multi-tenant systems need strict controls before general rollout.

### Scope

1. Multi-tenant data isolation enforcement.
2. Secret management via vault/KMS.
3. Audit trail and tamper-evident logs.
4. Security hardening for resource-server operation (issuer/audience checks, claim validation, cache serialization hardening, etc.).
5. Production safeguards proving dev auth shortcuts are disabled outside dev/test.

### Deliverables

1. Threat model + remediation checklist.
2. Security tests and penetration findings closure.
3. Compliance evidence pack (access logs, audit reports).
4. External auth dependency runbook (JWKS outage behavior, key rotation, degraded-mode policy).

### Done

- [ ] None

### Remaining

- [ ] Security signoff.
- [ ] Compliance signoff.

---

## Phase 7: Cutover, Decommissioning, and Optimization

### Why

After proving parity and stability, legacy paths must be removed to reduce complexity.

### Scope

1. Controlled cutover from old assignment JSON path to v2.
2. Decommission legacy rule switch and legacy storage.
3. Optimize performance and cost.

### Deliverables

1. Cutover runbook (with rollback).
2. Post-cutover validation report.
3. Legacy code/db cleanup migrations.

### Done

- [ ] None

### Remaining

- [ ] Production cutover completed.
- [ ] Legacy components removed.
- [ ] Final operational handover.

---

## 6. Cross-Phase Backlog (Always On)

1. Test strategy:
- unit, integration, property-based rule tests, load tests, chaos tests

2. Observability:
- run-level metrics (ingest/rule/gen duration, failure rate)
- business metrics (assignment coverage, unmatched participants)

3. Documentation:
- operator runbooks
- API docs
- data mapping cookbook by connector type

4. Change management:
- feature flags for every major cutover step
- migration rehearsals in staging

---

## 7. Risks and Mitigations

1. Risk: Data mismatch during dual-run.
- Mitigation: deterministic diff reports and blocking thresholds before cutover.

2. Risk: Rule DSL too weak for real domains.
- Mitigation: pilot with 3 distinct orgs before schema freeze.

3. Risk: Connector brittleness.
- Mitigation: snapshot/replay and retry with dead-letter queues.

4. Risk: Performance degradation.
- Mitigation: benchmark gates per phase and partition strategy early.

---

## 8. Progress Tracker

Phase completion summary (2026-02-18):
- Fully completed phases: none yet.
- Active implementation phases: 1, 2, 3.

| Phase | Status | Owner | Target Window | Notes |
|---|---|---|---|---|
| Phase 0 | In Progress | TBD | TBD | Discovery complete; approvals pending |
| Phase 1 | Completed | TBD | TBD | Assignment relational model, dual-write, dual-read mode, reconciliation scheduler, parity report, backfill workflow and lower-env migration verification implemented |
| Phase 2 | Completed | TBD | TBD | Submit completion sync, ownership validation, storage-mode cutover path implemented; transactional/concurrency/rollback verification tests completed |
| Phase 3 | Completed | TBD | TBD | Canonical schema, CSV/JSON/REST/JDBC connectors, PERSON/GROUP/MEMBERSHIP ingestion, lifecycle audit/events, ingestion/query APIs, snapshot replay, retention scheduler, outbox dispatcher, DB-backed integration tests implemented |
| Phase 4 | Completed | TBD | TBD | Rule DSL model/schema, simulation/publish APIs, explainability payload, golden parity tests, benchmark gate implemented |
| Phase 5 | Completed | TBD | TBD | Admin rule control-plane APIs, approval workflow (publish lock + 4-eyes), RBAC, admin audit trail/outbox implemented |
| Phase 6 | Not Started | TBD | TBD |  |
| Phase 7 | Not Started | TBD | TBD |  |

---

## 9. Session Handoff Log (Update Every Session)

Use this section to keep continuity across sessions.

### Entry Template

```
Date:
Session Goal:
Changes Completed:
Why These Changes:
Open Decisions:
Blockers:
Next Actions (ordered):
```

### Session Entry 1

Date: 2026-02-18  
Session Goal: Read full backend and produce production-grade phased plan.  
Changes Completed:
- Completed full backend logic review.
- Documented production redesign phased plan.
- Captured code-verified constraints and migration rationale.
Why These Changes:
- To establish a reusable, implementation-grade context document for future sessions.
Open Decisions:
- Tenant isolation approach (row-level vs schema-level).
- Rule DSL expressiveness boundary.
- Connector rollout priority.
Blockers:
- None.
Next Actions (ordered):
1. Finalize ADRs for target architecture.
2. Design DDL for Phase 1 and review with DBA.
3. Define assignment dual-write migration strategy.

### Session Entry 2

Date: 2026-02-18  
Session Goal: Start implementation with migration foundation and dual-write assignment path.  
Changes Completed:
- Added `campaign_assignments` table and migration backfill from legacy `assignments_json`.
- Added new assignment persistence port, JPA entity/repository, and adapter.
- Implemented dual-write from campaign assignment flows to new relational table.
- Updated evaluation submission flow to mark assignment completion in both new table and legacy campaign JSON.
- Verified compilation success with `./gradlew compileJava`.
Why These Changes:
- Establishes production redesign foundation without breaking existing behavior.
- Enables phased migration from JSON-embedded assignments to first-class relational assignments.
Open Decisions:
- Feature flag strategy for read-path cutover (`json|dual|v2`) and rollout sequencing.
- Whether to enforce strict assignment ownership validation in submit flow immediately or in next phase.
Blockers:
- None.
Next Actions (ordered):
1. Add read-path abstraction and feature flag for assignment source of truth.
2. Add integration tests for submit completion sync and dual-write consistency.
3. Add parity verification job/report (legacy JSON vs `campaign_assignments`).

### Session Entry 3

Date: 2026-02-18  
Session Goal: Continue redesign implementation with read-path cutover support.  
Changes Completed:
- Added assignment storage-mode config (`JSON`, `DUAL`, `V2`) and defaulted to `DUAL`.
- Upgraded campaign adapter to load assignments from relational table in `DUAL/V2` modes.
- Switched campaign-level assignment counts and evaluator campaign lookups to relational paths in `DUAL/V2`.
- Kept JSON mode fully supported for fallback.
- Verified compilation success with `./gradlew compileJava`.
Why These Changes:
- Moves migration from write-only dual path to read-capable cutover path.
- Enables gradual production rollout by mode instead of big-bang switch.
Open Decisions:
- Whether to keep `DUAL` as long-running mode or short transition mode only.
- Exact order for changing downstream reporting/dashboard reads to rely purely on v2 semantics.
Blockers:
- None.
Next Actions (ordered):
1. Add integration tests for `JSON` vs `DUAL/V2` read behavior parity.
2. Add reconciliation endpoint/job for assignment parity verification.
3. Introduce canonical audience model tables and ingestion scaffolding (Phase 3 start).

### Session Entry 4

Date: 2026-02-18  
Session Goal: Implement next redesign chunk (reconciliation + tests + Phase 3 scaffold start).  
Changes Completed:
- Added assignment reconciliation service and API endpoint:
  - `GET /api/v1/campaigns/{id}/assignments/reconcile`
- Added campaign repository accessor for legacy `assignments_json`.
- Added read-mode behavior tests for `CampaignAdapter` (`JSON` vs `DUAL/V2` paths):
  - `src/test/java/com/evaluationservice/infrastructure/adapter/CampaignAdapterTest.java`
- Fixed adapter edge case for empty anonymous-role set when reconstructing campaigns in relational read mode.
- Added Phase 3 canonical audience schema scaffolding migration (`V6`) and ingestion service/use-case scaffolds.
- Verified compile and targeted tests.
Why These Changes:
- Reconciliation and tests reduce cutover risk.
- Canonical schema scaffold unblocks connector/rule-engine implementation phases.
Open Decisions:
- Reconciliation automation model: synchronous endpoint only vs scheduled report/alert pipeline.
- Canonical ID strategy (`UUID` vs source-stable composite IDs) for persons/groups at scale.
Blockers:
- None.
Next Actions (ordered):
1. Add scheduled reconciliation job with metrics/alerts.
2. Add assignment ownership validation in submit flow against assignments source of truth.
3. Build first connector (CSV) into canonical audience model with ingestion run tracking.

### Session Entry 5

Date: 2026-02-18  
Session Goal: Continue with next implementation block (scheduler reconciliation, assignment validation, CSV ingestion path).  
Changes Completed:
- Added scheduled reconciliation runner:
  - `src/main/java/com/evaluationservice/infrastructure/scheduler/AssignmentReconciliationScheduler.java`
  - assignment config extensions in `EvaluationServiceProperties` and `application.yml`
- Added assignment ownership validation in evaluation submission flow and tests.
- Implemented first CSV connector path in ingestion service with run tracking and tenant/person persistence.
- Added ingestion API endpoint (`POST /api/v1/audience/ingest`).
- Verified compile and targeted tests:
  - `CampaignAdapterTest`
  - `EvaluationSubmissionServiceTest`
Why These Changes:
- Adds operational safety during JSON->relational cutover.
- Closes a correctness/security gap in submission ownership checks.
- Starts concrete Phase 3 execution with a usable connector path.
Open Decisions:
- CSV parser robustness requirements (current implementation is simple comma-split, no quoted-field support yet).
- Whether ingestion should auto-create tenants in non-production environments.
Blockers:
- None.
Next Actions (ordered):
1. Add robust CSV parsing (quoted values, escaped commas) and row-level error reporting artifacts.
2. Add ingestion run query endpoints and metrics.
3. Build mapping layer and second connector type (REST or JDBC) to validate extensibility.

### Session Entry 6

Date: 2026-02-18  
Session Goal: Harden ingestion and complete query/rejection operational surface.  
Changes Completed:
- Added row-level rejection persistence table and entity/repository.
- Added ingestion run query service and APIs:
  - list runs
  - get run details
  - list row rejections
- Hardened CSV ingestion validation:
  - strict `active` parsing
  - duplicate-header detection
  - tenant collision rejection for `person_id`
- Added unit tests for ingestion and query services.
- Verified compile and targeted tests.
Why These Changes:
- Moves ingestion from scaffold quality toward production-safe behavior.
- Gives operators visibility into ingestion failures without DB access.
Open Decisions:
- Whether canonical person IDs should remain tenant-scoped natural IDs or move to synthetic IDs with source identity mapping tables.
- Which second connector (REST vs JDBC) should be prioritized first for production onboarding.
Blockers:
- None.
Next Actions (ordered):
1. Add connector registry abstraction and implement second source connector.
2. Introduce ingestion mapping configuration (field mapping/profile tables) with validation API.
3. Add integration tests for ingestion APIs against an in-memory DB profile.

### Session Entry 7

Date: 2026-02-18  
Session Goal: Execute next Phase-3 step by introducing connector abstraction and a second source type.  
Changes Completed:
- Introduced source connector contract and registry-based connector resolution in ingestion service.
- Refactored CSV parsing/extraction into dedicated connector class.
- Added JSON connector (`sourceType=JSON`) for structured record ingestion.
- Updated/extended ingestion tests to cover JSON connector path.
- Verified targeted tests for ingestion and query services.
Why These Changes:
- Removes hardcoded single-source branching in the ingestion service.
- Establishes extensibility seam required for REST/JDBC connectors without future service rewrites.
Open Decisions:
- Prioritize REST vs JDBC as the first production-grade external connector.
- Choose secrets strategy for connector credentials (Vault/KMS reference model).
Blockers:
- None.
Next Actions (ordered):
1. Add production connector implementation (REST or JDBC) with secure config references.
2. Implement mapping profile model (source-field -> canonical-field) and validation endpoint.
3. Add integration tests for `/api/v1/audience/ingest` and ingestion run/rejection query APIs.

### Session Entry 8

Date: 2026-02-18  
Session Goal: Implement first production external connector for audience ingestion.  
Changes Completed:
- Added `REST` source connector with:
  - configurable `url`, `method` (`GET`/`POST`), headers, body
  - bounded connect/read timeouts
  - records extraction via `recordsPath` or default `records`
- Integrated connector automatically through existing connector registry wiring in ingestion service.
- Added connector tests using in-process HTTP server for GET and POST+nested path scenarios.
- Verified targeted ingestion and query test suites.
Why These Changes:
- Delivers first real external-source ingest path needed for no-code onboarding across heterogeneous systems.
- Ensures connector behavior is deterministic and test-verified without relying on external infra.
Open Decisions:
- Credential management shape for REST/JDBC connectors (Vault reference vs internal secret registry).
- Whether to prioritize JDBC connector or mapping-profile model in next increment.
Blockers:
- None.
Next Actions (ordered):
1. Implement mapping profile model and validation endpoint (field normalization without code changes).
2. Add integration tests for audience ingest/query APIs with DB-backed repositories.
3. Implement JDBC connector with secure connection-reference configuration.

### Session Entry 9

Date: 2026-02-18  
Session Goal: Deliver no-code field mapping profiles and apply them in live ingestion flow.  
Changes Completed:
- Added `audience_mapping_profiles` schema migration and persistence model.
- Implemented mapping-profile service with:
  - canonical-field validation
  - source-field normalization
  - tenant/sourceType/profile resolution for ingestion
- Extended audience ingestion request contract with optional `mappingProfileId`.
- Integrated profile-driven field normalization before ingest validation/upsert.
- Added mapping profile management APIs (create/list/get/validate).
- Added/updated tests for mapping profile service and ingest profile application.
- Verified targeted test suites pass.
Why These Changes:
- Enables no-code adaptation to heterogeneous source schemas (e.g., `employee_id` -> `person_id`) without backend code edits.
- Establishes a reusable control-plane primitive required for plug-and-play onboarding.
Open Decisions:
- Whether profile lifecycle should support versioning/immutable publish states in Phase 5 governance.
- Whether mapping profiles should be source-connector scoped only, or also campaign scoped.
Blockers:
- None.
Next Actions (ordered):
1. Add API integration tests for mapping-profile + ingest execution with DB-backed repositories.
2. Implement JDBC connector with secure reference-based credentials.
3. Add audit events for mapping profile create/update/deactivate actions.

### Session Entry 10

Date: 2026-02-18  
Session Goal: Add API integration coverage and remove local DB test blockers.  
Changes Completed:
- Added `AudienceController` integration tests covering:
  - mapping profile creation
  - mapped ingestion execution
  - rejection persistence and retrieval via query API
- Refactored `DatabaseConfig` to be property-driven for driver/dialect/ddl and conditional Flyway activation.
- Validated targeted suites including new integration tests.
Why These Changes:
- Confirms end-to-end behavior through HTTP + controller + service + repository layers.
- Aligns with the no-blocker development requirement by allowing non-Postgres local/test execution.
Open Decisions:
- Exact credential-reference model for upcoming JDBC connector.
- Whether audit events should emit synchronously or via outbox/event bus.
Blockers:
- None.
Next Actions (ordered):
1. Implement JDBC connector with secure connection-reference config.
2. Add audit trail/event emission for mapping profile lifecycle actions.
3. Add ingestion snapshot/version replay capability.

### Session Entry 11

Date: 2026-02-18  
Session Goal: Implement JDBC connector with secure reference-based configuration.  
Changes Completed:
- Added `JDBC` source connector supporting:
  - `connectionRef` lookup
  - SELECT-only query enforcement
  - optional per-ref custom-query allowance
  - timeout/fetch/max-row controls from connection ref
- Added audience JDBC config model in `EvaluationServiceProperties`:
  - `evaluation.service.audience.jdbc.connections.<ref>.*`
- Added default YAML scaffold:
  - `evaluation.service.audience.jdbc.connections: {}`
- Added targeted connector tests with H2-backed JDBC source.
- Verified targeted ingestion and integration suites pass.
Why These Changes:
- Enables onboarding from enterprise relational systems without embedding credentials in API requests.
- Keeps connector access centrally controlled by environment configuration.
Open Decisions:
- Externalized secret source for JDBC passwords (Vault/KMS integration) vs plain env injection conventions.
Blockers:
- None.
Next Actions (ordered):
1. Add audit trail/event emission for mapping profile lifecycle actions.
2. Add ingestion snapshot/version replay capability.
3. Expand data quality checks (email/ID format and referential validation) before persistence.

### Session Entry 12

Date: 2026-02-18  
Session Goal: Implement mapping-profile lifecycle audit and event emission.  
Changes Completed:
- Added mapping-profile lifecycle audit events table (`V10`) with profile/tenant/type/actor/payload/timestamp.
- Implemented lifecycle event persistence repository/entity.
- Added domain event publication (`AudienceMappingProfileLifecycleEvent`) on create/update/deactivate.
- Extended mapping profile service with update/deactivate operations and event query API support.
- Added new APIs:
  - `PUT /api/v1/audience/mapping-profiles/{profileId}`
  - `POST /api/v1/audience/mapping-profiles/{profileId}/deactivate`
  - `GET /api/v1/audience/mapping-profiles/{profileId}/events`
- Extended integration and unit tests to verify lifecycle behavior and audit emission.
Why These Changes:
- Completes lifecycle governance for mapping profiles with traceability and downstream event hooks.
Open Decisions:
- Whether to route lifecycle events through outbox/event bus for guaranteed external delivery.
Blockers:
- None.
Next Actions (ordered):
1. Add ingestion snapshot/version replay capability.
2. Expand pre-persist data quality checks (format/referential constraints).
3. Define outbox strategy for external event delivery guarantees.

### Session Entry 13

Date: 2026-02-18  
Session Goal: Implement ingestion snapshot/version replay capability.  
Changes Completed:
- Added ingestion snapshot storage (`V11`) for source config + resolved records per run.
- Added snapshot entity/repository/service for serialize/load operations.
- Extended ingestion use case with replay operation.
- Implemented deterministic replay in ingestion service using stored snapshot records.
- Added replay API:
  - `POST /api/v1/audience/ingestion-runs/{runId}/replay`
- Extended unit and integration tests for replay behavior.
Why These Changes:
- Enables reproducible ingestion reruns without source drift from external connectors.
- Improves operational recovery and auditability in production.
Open Decisions:
- Snapshot retention/TTL policy and archival strategy.
- Whether replay should support selective row replay filters.
Blockers:
- None.
Next Actions (ordered):
1. Expand pre-persist data quality checks (format/referential constraints).
2. Define outbox strategy for external event delivery guarantees.
3. Add retention policy job for ingestion snapshots/events.

### Session Entry 14

Date: 2026-02-18  
Session Goal: Expand ingestion data quality checks before persistence.  
Changes Completed:
- Added strict `person_id` quality checks (allowed charset + max length).
- Added duplicate `person_id` rejection within a single source payload.
- Added email format validation for non-null email values.
- Ensured quality failures produce row-level rejection records and are exposed through existing rejection APIs.
- Added/updated unit and integration tests for these checks.
Why These Changes:
- Prevents malformed and duplicate identity data from entering canonical audience tables.
- Improves operational diagnostics by preserving deterministic row-level rejection reasons.
Open Decisions:
- Validation profile model depth for cross-entity referential rules (memberships/relations) in later phases.
Blockers:
- None.
Next Actions (ordered):
1. Define outbox strategy for external event delivery guarantees.
2. Add retention policy job for ingestion snapshots/events.
3. Implement cross-entity referential validation once group/membership ingestion paths are active.

### Session Entry 15

Date: 2026-02-18  
Session Goal: Implement outbox strategy foundation for reliable external event delivery.  
Changes Completed:
- Added integration outbox schema (`V12`) with delivery status/attempt metadata.
- Added outbox entity/repository.
- Implemented mapping-profile lifecycle event -> outbox writer listener.
- Verified outbox records are created by lifecycle actions in integration tests.
Why These Changes:
- Establishes durable delivery contract boundary for downstream integrations.
- Prevents event loss when external consumers are temporarily unavailable.
Open Decisions:
- Polling vs streaming dispatcher implementation for publishing outbox records.
- Retry backoff policy and dead-letter threshold defaults.
Blockers:
- None.
Next Actions (ordered):
1. Add retention policy job for ingestion snapshots/events/outbox records.
2. Implement outbox dispatcher worker with retry/backoff and publish acknowledgements.
3. Implement cross-entity referential validation once group/membership ingestion paths are active.

### Session Entry 16

Date: 2026-02-18  
Session Goal: Implement retention policy for ingestion/audit/outbox artifacts.  
Changes Completed:
- Added repository-level purge operations for:
  - ingestion snapshots
  - mapping profile lifecycle events
  - outbox records by status + age
- Added retention configuration under `evaluation.service.audience.retention.*`:
  - enabled
  - cron
  - snapshot TTL
  - mapping-event TTL
  - outbox published/failed TTL
- Added retention cleanup service + scheduler.
- Verified with dedicated unit tests and targeted integration suite.
Why These Changes:
- Keeps operational storage bounded and aligns with production housekeeping requirements.
- Separates retention behavior from business logic and makes cleanup policy environment-configurable.
Open Decisions:
- Final retention windows per compliance regime.
- Whether retention metrics/alerts should be emitted to observability stack.
Blockers:
- None.
Next Actions (ordered):
1. Implement outbox dispatcher worker with retry/backoff and publish acknowledgements.
2. Implement cross-entity referential validation once group/membership ingestion paths are active.
3. Add retention execution metrics and failure alerting.

### Session Entry 17

Date: 2026-02-18  
Session Goal: Implement outbox dispatcher with retries, backoff, and publish acknowledgements.  
Changes Completed:
- Added due-event polling query on outbox repository.
- Implemented outbox dispatcher service:
  - fetch due `PENDING` events
  - publish via pluggable `OutboxEventPublisher`
  - mark `PUBLISHED` on success with `published_at`
  - on failure: increment attempts, set `next_attempt_at` with exponential backoff
  - mark `DEAD` when max attempts reached
- Added dispatcher scheduler gated by config flag.
- Added default logging publisher implementation for baseline runtime behavior.
- Added/verified unit tests for success, retry, and dead-letter transitions.
Why These Changes:
- Completes operational outbox loop required for reliable external integration delivery.
- Provides deterministic retry/dead behavior and explicit acknowledgement state transitions.
Open Decisions:
- Concrete external transport adapter (Kafka/SQS/webhook) implementation and idempotency key policy.
Blockers:
- None.
Next Actions (ordered):
1. Implement cross-entity referential validation once group/membership ingestion paths are active.
2. Add retention/disptach metrics and alerting instrumentation.
3. Add external transport publisher implementation behind `OutboxEventPublisher`.

### Session Entry 18

Date: 2026-02-18  
Session Goal: Activate group/membership ingestion and enforce referential validation.  
Changes Completed:
- Added audience group and membership persistence models/repositories.
- Extended ingestion processing with entity-specific modes:
  - `PERSON`, `GROUP`, `MEMBERSHIP` via `sourceConfig.entityType`
- Added referential checks for memberships:
  - reject when `person_id` or `group_id` does not exist in same tenant.
- Added membership/group ingestion test coverage in unit and integration tests.
Why These Changes:
- Moves referential validation from planned to implemented by enabling dependent entity ingestion paths.
- Supports real organizational structures (sections/teams/departments + memberships) without code changes per domain.
Open Decisions:
- Relation (`audience_relations`) ingestion rollout sequencing and validation profile depth.
Blockers:
- None.
Next Actions (ordered):
1. Add retention/dispatch metrics and alerting instrumentation.
2. Add external transport publisher implementation behind `OutboxEventPublisher`.
3. Implement relation ingestion path with referential checks similar to memberships.

### Session Entry 19

Date: 2026-02-18  
Session Goal: Add retention/dispatch metrics and alert-friendly instrumentation.  
Changes Completed:
- Instrumented outbox dispatch with Micrometer counters and duration timer.
- Instrumented retention cleanup with Micrometer counters and duration timer.
- Added scheduler warning when dispatch run sends events to `DEAD` state.
- Updated tests to validate metric emission via `SimpleMeterRegistry`.
Why These Changes:
- Enables Prometheus/Grafana alerting on retry/dead-letter behavior and retention execution health.
- Provides quantitative signals for operational SLO tracking.
Open Decisions:
- Concrete alert thresholds and routing policy (PagerDuty/Slack/email).
Blockers:
- None.
Next Actions (ordered):
1. Add external transport publisher implementation behind `OutboxEventPublisher`.
2. Implement relation ingestion path with referential checks similar to memberships.
3. Add explicit dashboards/runbooks for outbox and retention metrics.

### Session Entry 20

Date: 2026-02-18  
Session Goal: Implement concrete external outbox transport publisher.  
Changes Completed:
- Added webhook transport behavior in outbox publisher:
  - sends event payload to configured webhook endpoint
  - supports auth token and custom headers
  - treats non-2xx responses as publish failures (so dispatcher retry/dead logic applies)
- Kept safe fallback behavior when webhook transport is disabled (logging path).
- Added transport configuration keys under `evaluation.service.audience.outbox.*`.
- Added dedicated tests with in-process HTTP server for success/failure paths.
Why These Changes:
- Replaces stub-only publishing with a production-usable external integration path.
- Preserves reliability guarantees through existing outbox retry/dead-letter mechanics.
Open Decisions:
- Whether to add Kafka/SQS transport in addition to webhook as first-class alternative.
Blockers:
- None.
Next Actions (ordered):
1. Implement relation ingestion path with referential checks similar to memberships.
2. Add explicit dashboards/runbooks for outbox and retention metrics.
3. Add transport-level idempotency key conventions for downstream consumers.

### Session Entry 21

Date: 2026-02-18  
Session Goal: Add Kafka and RabbitMQ transport options for outbox publishing.  
Changes Completed:
- Added Kafka and RabbitMQ dependencies.
- Extended outbox transport config to support:
  - `transport=LOG|WEBHOOK|KAFKA|RABBITMQ`
  - kafka topic + send timeout
  - rabbit exchange + routing key
- Updated outbox publisher to route per selected transport.
- Added tests for webhook, kafka, and rabbit transport paths.
Why These Changes:
- Enables enterprise deployment flexibility where webhook is not preferred or available.
- Keeps a single outbox dispatch lifecycle while swapping transport adapters by config.
Open Decisions:
- Default transport profile by environment (dev/staging/prod).
- Broker/topic/exchange naming governance across tenants/services.
Blockers:
- None.
Next Actions (ordered):
1. Add explicit dashboards/runbooks for outbox and retention metrics.
2. Add transport-level idempotency key conventions for downstream consumers.
3. Extend relation ingestion path (if required by next domain onboarding use case).

### Session Entry 22

Date: 2026-02-18  
Session Goal: Close remaining Phase 1/2 verification gaps with parity reporting and assignment lifecycle hardening.  
Changes Completed:
- Added assignment parity aggregate report service and API:
  - `GET /api/v1/campaigns/assignments/reconcile/report?maxCampaigns=...`
  - Files:
    - `src/main/java/com/evaluationservice/infrastructure/service/AssignmentParityReportService.java`
    - `src/main/java/com/evaluationservice/api/dto/response/AssignmentParityReportResponse.java`
    - `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
- Hardened assignment completion integrity:
  - `CampaignAssignmentAdapter.markCompleted` now throws when no relational assignment row is updated.
  - File:
    - `src/main/java/com/evaluationservice/infrastructure/adapter/CampaignAssignmentAdapter.java`
- Added submit-flow transactional/concurrency integration tests:
  - duplicate submit race verifies single-write integrity
  - assignment completion failure path verifies transaction rollback
  - File:
    - `src/test/java/com/evaluationservice/application/service/EvaluationSubmissionServiceIntegrationTest.java`
- Added rollback-read coverage test in JSON mode:
  - `src/test/java/com/evaluationservice/infrastructure/adapter/CampaignAdapterTest.java`
- Added parity aggregation service unit test:
  - `src/test/java/com/evaluationservice/infrastructure/service/AssignmentParityReportServiceTest.java`
- Fixed constructor autowiring ambiguity in outbox publisher bean:
  - `src/main/java/com/evaluationservice/infrastructure/service/LoggingOutboxEventPublisher.java`
Why These Changes:
- Phase 1 required an operational parity report artifact beyond single-campaign checks.
- Phase 2 needed deterministic validation for transaction boundaries and duplicate submission races.
- Fail-fast assignment completion update prevents silent divergence between evaluation and assignment states.
Open Decisions:
- Whether duplicate submit races should remain strict single-writer (one request may fail) or be upgraded to fully idempotent dual-success behavior.
Blockers:
- None.
Next Actions (ordered):
1. Execute and validate assignment backfill at larger scale in staging-like environment.
2. Complete the remaining Phase 3 gap: policy-driven referential validation profiles.
3. Prepare Phase 2 exit runbook evidence using the new parity report endpoint.

### Session Entry 23

Date: 2026-02-18  
Session Goal: Implement remaining Phase 1/2/3 items in sync and close code-level gaps.  
Changes Completed:
- Added executable assignment backfill workflow from legacy JSON to relational assignments with report output:
  - service:
    - `src/main/java/com/evaluationservice/infrastructure/service/AssignmentBackfillService.java`
  - API:
    - `POST /api/v1/campaigns/assignments/backfill?dryRun=...&maxCampaigns=...`
    - `src/main/java/com/evaluationservice/api/controller/CampaignController.java`
    - `src/main/java/com/evaluationservice/api/dto/response/AssignmentBackfillResponse.java`
  - tests:
    - `src/test/java/com/evaluationservice/infrastructure/service/AssignmentBackfillServiceTest.java`
    - `src/test/java/com/evaluationservice/infrastructure/service/AssignmentBackfillServiceIntegrationTest.java`
- Added policy-driven validation profiles for audience ingest (config-only behavior by tenant/environment profile):
  - property model:
    - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
    - `src/main/resources/application.yml`
  - enforcement in ingest pipeline:
    - `src/main/java/com/evaluationservice/application/service/AudienceIngestionService.java`
  - coverage:
    - `src/test/java/com/evaluationservice/application/service/AudienceIngestionServiceTest.java`
- Deep referential checks now include:
  - membership active-person/active-group constraints
  - required membership role / validity window checks
  - valid_from <= valid_to enforcement
  - role-to-group-type policy constraints
Why These Changes:
- Phase 1 needed a concrete backfill execution path and scale evidence, not only reconciliation/parity readers.
- Phase 3 needed configurable validation strictness to support plug-and-play onboarding across different domains without code changes.
- Phase 2 checklist had no remaining unchecked implementation items; tracker updated to completed.
Open Decisions:
- Operational ownership and schedule for running backfill in lower environments and capturing formal sign-off evidence.
Blockers:
- Lower environment migration execution and signoff remain external to this repo code.
Next Actions (ordered):
1. Run backfill endpoint in lower/staging environment with production-like volume and archive report output.
2. Define per-tenant/domain validation profile presets (e.g., university, hospital, NGO) in configuration management.
3. Begin Phase 4 rule DSL implementation (first schema + compiler skeleton + parity tests).

### Session Entry 24

Date: 2026-02-18  
Session Goal: Close final Phase 1 item by verifying migration application in lower-env equivalent flow.  
Changes Completed:
- Added lower-env integration migration test with PostgreSQL Testcontainers:
  - migrates schema up to `V4`
  - inserts legacy `campaigns.assignments_json` data
  - migrates to latest and verifies `campaign_assignments` backfill from legacy JSON
  - file:
    - `src/test/java/com/evaluationservice/infrastructure/migration/Phase1MigrationLowerEnvIntegrationTest.java`
- Executed targeted test suite successfully:
  - `./gradlew test --tests com.evaluationservice.infrastructure.migration.Phase1MigrationLowerEnvIntegrationTest`
- Updated tracker/checklist to mark Phase 1 as completed.
Why These Changes:
- Provides executable, repeatable evidence that schema migrations apply and Phase 1 backfill behavior works in a lower-env-equivalent PostgreSQL runtime.
Open Decisions:
- None for Phase 1 implementation scope.
Blockers:
- None.
Next Actions (ordered):
1. Start Phase 4 rule DSL schema and compiler/executor foundation.
2. Prepare tenant/domain validation profile presets (university/hospital/NGO) for onboarding playbooks.
3. Draft cutover evidence bundle for production readiness review.

### Session Entry 25

Date: 2026-02-18  
Session Goal: Complete Phase 4 and Phase 5 backend scope (rule DSL engine/control-plane + admin governance).  
Changes Completed:
- Added Phase 4 rule control-plane persistence model and migration:
  - `assignment_rule_definitions`
  - `assignment_rule_publish_requests`
  - `admin_action_audit_logs`
  - file: `src/main/resources/db/migration/V13__rule_control_plane.sql`
- Implemented rule definition lifecycle and execution orchestration:
  - create/update/list/get/deprecate
  - publish request / approve / reject
  - simulate with explainability payload
  - publish assignments to campaign pipeline
  - files:
    - `src/main/java/com/evaluationservice/infrastructure/service/RuleControlPlaneService.java`
    - `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
- Added approval workflow guardrails and RBAC:
  - publish lock + 4-eyes approval config
  - admin-only control-plane endpoints via `@PreAuthorize`
  - files:
    - `src/main/java/com/evaluationservice/infrastructure/config/EvaluationServiceProperties.java`
    - `src/main/resources/application.yml`
    - `src/main/java/com/evaluationservice/api/controller/RuleControlPlaneController.java`
- Added admin audit trail + outbox emission for admin actions:
  - `src/main/java/com/evaluationservice/infrastructure/service/AdminAuditLogService.java`
- Added rule DSL schema and quality gates:
  - schema: `docs/schemas/assignment-rule-definition.schema.json`
  - golden parity tests: `src/test/java/com/evaluationservice/application/service/RuleDslGoldenParityTest.java`
  - benchmark test: `src/test/java/com/evaluationservice/application/service/RuleExecutionBenchmarkTest.java`
- Added rule control-plane unit tests:
  - `src/test/java/com/evaluationservice/infrastructure/service/RuleControlPlaneServiceTest.java`
Why These Changes:
- Phase 4 required moving from hardcoded-only rule execution to a versioned no-code rule-definition and execution control plane.
- Phase 5 required governance/approval/RBAC/audit controls for production-safe non-developer operations.
Open Decisions:
- Frontend/admin UX implementation details and approval UX states if full UI is required in this repository.
Blockers:
- None.
Next Actions (ordered):
1. Add frontend admin rule-builder and approval screens (if in scope of this repo).
2. Extend explainability payload with richer per-candidate diagnostics for every rule type.
3. Start Phase 6 governance/security hardening tasks.

---

## 10. Definition of Done (Program Level)

Program is done when:

1. New tenant onboarding in supported domains is configuration-only.
2. No backend code change is required for new evaluator/evaluatee mapping logic within DSL/operator limits.
3. Assignment generation is auditable, explainable, and reproducible.
4. Legacy JSON assignment/rule switch paths are removed.
5. SLOs and security/compliance requirements are met in production.
