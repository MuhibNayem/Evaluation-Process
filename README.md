# Evaluation Service

Production-grade evaluation orchestration service with dynamic assignment, canonical audience ingestion, rule governance, and auditable admin control-plane workflows.

## What This Service Does

1. Manages template and campaign lifecycles.
2. Generates evaluator-evaluatee assignments dynamically from configurable sources/rules.
3. Accepts evaluation submissions and scoring flows.
4. Provides reporting and dashboard summaries.
5. Supports no-code audience onboarding through connectors + mapping profiles + validation profiles.
6. Provides governed rule-definition lifecycle with approval workflow and audit/outbox events.

## Implementation Status

Phases completed in codebase:
1. Phase 1: Assignment relational migration, parity checks, backfill.
2. Phase 2: Submission integrity and assignment completion consistency.
3. Phase 3: Canonical audience model, ingestion connectors, replay, retention, outbox dispatcher.
4. Phase 4: Rule control plane (versioned rule definitions, simulation, publish pipeline).
5. Phase 5: Admin guardrails (4-eyes, publish lock), RBAC-protected control plane, audit logging.

Primary references:
1. Runbook: `docs/SYSTEM_RUNBOOK_PHASE1_TO_5.md`
2. API docs: `docs/API_DOCUMENTATION.md`
3. OpenAPI: `docs/openapi.yaml`
4. Redesign plan log: `docs/PRODUCTION_REDESIGN_IMPLEMENTATION_PLAN.md`

## Tech Stack

1. Java 25 (preview enabled)
2. Spring Boot 4.0.1
3. PostgreSQL + Flyway
4. Redis cache
5. Spring Security (JWT resource-server mode + dev-mode local bypass)
6. Observability: Micrometer, Prometheus, Grafana, Loki, Zipkin
7. Messaging integrations: webhook, Kafka, RabbitMQ via outbox transport

## Architecture

Hexagonal layering:
1. Domain: entities, rules, invariants, value objects.
2. Application: use-case services (campaign, evaluation, ingestion, scoring).
3. API: REST controllers and DTO contracts.
4. Infrastructure: repositories, schedulers, outbox dispatcher, adapters.

Key backend folders:
1. `src/main/java/com/evaluationservice/api/controller`
2. `src/main/java/com/evaluationservice/application/service`
3. `src/main/java/com/evaluationservice/domain`
4. `src/main/java/com/evaluationservice/infrastructure`
5. `src/main/resources/db/migration`

## Authentication Model

This service can run in two modes:

1. Development mode (`evaluation.service.security.dev-mode=true`):
2. Request auth is bypassed for productivity.
3. Mock login endpoint enabled: `POST /api/v1/auth/login`.

4. Secured mode (`evaluation.service.security.dev-mode=false`):
5. JWT auth enforced by resource-server security filter chain.
6. Rule control plane requires `ROLE_ADMIN`.

Note:
1. In production, authentication is expected to be provided by a dedicated external auth service/issuer.

## Quick Start

### Prerequisites

1. Java 25 SDK
2. Docker + Docker Compose

### Start dependencies

```bash
docker-compose up -d postgres redis prometheus loki promtail grafana zipkin
```

### Build and run

```bash
./gradlew clean build
./gradlew bootRun
```

Service URL:
1. `http://localhost:8080`

## Core API Surface

### Campaigns

1. `POST /api/v1/campaigns`
2. `POST /api/v1/campaigns/{id}/assignments/dynamic`
3. `GET /api/v1/campaigns/{id}/assignments/reconcile`
4. `POST /api/v1/campaigns/assignments/backfill`

### Evaluations

1. `POST /api/v1/evaluations`
2. `PUT /api/v1/evaluations/{id}`

### Audience Ingestion

1. `POST /api/v1/audience/ingest`
2. `POST /api/v1/audience/ingestion-runs/{runId}/replay`
3. Mapping profile APIs under `/api/v1/audience/mapping-profiles`

### Rule Control Plane

1. `POST /api/v1/admin/rules`
2. `POST /api/v1/admin/rules/{id}/publish-requests`
3. `POST /api/v1/admin/rules/publish-requests/{publishRequestId}/approve`
4. `POST /api/v1/admin/rules/{id}/simulate`
5. `POST /api/v1/admin/rules/{id}/publish-assignments`

## Configuration Highlights

Main config: `src/main/resources/application.yml`

High-impact keys:
1. `evaluation.service.security.dev-mode`
2. `evaluation.service.assignment.storage-mode`
3. `evaluation.service.assignment.reconciliation-*`
4. `evaluation.service.audience.validation-profiles.*`
5. `evaluation.service.audience.retention.*`
6. `evaluation.service.audience.outbox.*`
7. `evaluation.service.admin.publish-lock-enabled`
8. `evaluation.service.admin.require-four-eyes-approval`
9. `evaluation.service.features.enable-reports`
10. `evaluation.service.features.enable-csv-export`
11. `evaluation.service.features.enable-pdf-export`

## Operations and Observability

Important endpoints:
1. Health: `GET /actuator/health`
2. Prometheus: `GET /actuator/prometheus`
3. Info: `GET /actuator/info`

Local UIs:
1. Prometheus: `http://localhost:9090`
2. Grafana: `http://localhost:3000`
3. Zipkin: `http://localhost:9411`
4. Loki: `http://localhost:3100`

## Testing

Run full tests:

```bash
./gradlew test
```

Run targeted tests:

```bash
./gradlew test --tests com.evaluationservice.infrastructure.service.RuleControlPlaneServiceTest
```

## Notes for Integrators

1. For exact request/response schemas, rely on `docs/openapi.yaml`.
2. For operational behavior and policy details, use `docs/SYSTEM_RUNBOOK_PHASE1_TO_5.md`.
3. For API usage examples, use `docs/API_DOCUMENTATION.md`.
