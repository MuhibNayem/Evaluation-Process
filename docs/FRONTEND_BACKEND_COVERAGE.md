# Frontend-Backend Coverage Matrix

Last updated: 2026-02-19

## Scope
This matrix maps backend API features in `src/main/java/com/evaluationservice/api/controller/*` to current frontend coverage in `client/src/routes/*`.

Legend:
- `Full`: Implemented and reachable in UI
- `Partial`: Implemented but only partly surfaced
- `Backend-only`: No direct UI (ops/system concern)

## 1) Auth
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/auth/login` | `client/src/routes/(auth)/login/+page.svelte` | Full |

Notes:
- Backend login endpoint is active only when `evaluation.service.security.dev-mode=true`.
- Frontend auto-auth is env controlled (`VITE_CLIENT_AUTO_AUTH`), no longer hardcoded.

## 2) Dashboard
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `GET /api/v1/dashboard/stats` | `client/src/routes/(app)/+page.svelte` | Full |

## 3) Templates
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/templates` | `client/src/routes/(app)/templates/new/+page.svelte` | Full |
| `PUT /api/v1/templates/{id}` | `client/src/routes/(app)/templates/[id]/edit/+page.svelte` | Full |
| `GET /api/v1/templates/{id}` | `client/src/routes/(app)/templates/[id]/+page.svelte` | Full |
| `GET /api/v1/templates` | `client/src/routes/(app)/templates/+page.svelte` | Full |
| `POST /api/v1/templates/{id}/publish` | `client/src/routes/(app)/templates/[id]/+page.svelte` | Full |
| `POST /api/v1/templates/{id}/deprecate` | `client/src/routes/(app)/templates/[id]/+page.svelte` | Full |
| `DELETE /api/v1/templates/{id}` | `client/src/routes/(app)/templates/[id]/+page.svelte` | Full |

## 4) Campaigns
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/campaigns` | `client/src/routes/(app)/campaigns/new/+page.svelte` | Full |
| `GET /api/v1/campaigns/{id}` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `PUT /api/v1/campaigns/{id}` | `client/src/routes/(app)/campaigns/[id]/edit/+page.svelte` | Full |
| `GET /api/v1/campaigns` | `client/src/routes/(app)/campaigns/+page.svelte` | Full |
| `POST /api/v1/campaigns/{id}/activate` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `POST /api/v1/campaigns/{id}/close` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `POST /api/v1/campaigns/{id}/archive` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `POST /api/v1/campaigns/{id}/assignments` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `POST /api/v1/campaigns/{id}/assignments/dynamic` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` + builder | Full |
| `POST /api/v1/campaigns/{id}/extend-deadline` | `client/src/routes/(app)/campaigns/[id]/+page.svelte` | Full |
| `GET /api/v1/campaigns/{id}/progress` | campaign details page | Full |
| `GET /api/v1/campaigns/assignments/me` | `client/src/routes/(app)/evaluations/+page.svelte` and `.../evaluations/new/[id]/+page.svelte` | Full |
| `GET /api/v1/campaigns/{id}/assignments/reconcile` | `client/src/routes/(app)/campaigns/+page.svelte` (assignment tools) | Full |
| `GET /api/v1/campaigns/assignments/reconcile/report` | `client/src/routes/(app)/campaigns/+page.svelte` | Full |
| `POST /api/v1/campaigns/assignments/backfill` | `client/src/routes/(app)/campaigns/+page.svelte` | Full |

## 5) Evaluations
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/evaluations` | `client/src/routes/(app)/evaluations/new/[id]/+page.svelte` | Full |
| `GET /api/v1/evaluations/{id}` | `client/src/routes/(app)/evaluations/[id]/+page.svelte` | Full |
| `PUT /api/v1/evaluations/{id}` | `client/src/routes/(app)/evaluations/[id]/+page.svelte` | Full |
| `GET /api/v1/evaluations/campaign/{campaignId}` | reports flow | Full |
| `GET /api/v1/evaluations/evaluatee/{evaluateeId}` | not primary navigation | Partial |
| `POST /api/v1/evaluations/{id}/flag` | `client/src/routes/(app)/evaluations/[id]/+page.svelte` | Full |
| `POST /api/v1/evaluations/{id}/invalidate` | `client/src/routes/(app)/evaluations/[id]/+page.svelte` | Full |

## 6) Reports
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `GET /api/v1/reports/individual` | `client/src/routes/(app)/reports/+page.svelte` (JSON view) | Full |
| `GET /api/v1/reports/campaign/{campaignId}` | `client/src/routes/(app)/reports/+page.svelte` (JSON view) | Full |
| `GET /api/v1/reports/export/csv/{campaignId}` | `client/src/routes/(app)/reports/+page.svelte` | Full |
| `GET /api/v1/reports/export/pdf` | `client/src/routes/(app)/reports/+page.svelte` | Full |

Notes:
- PDF endpoint may return `403` when `evaluation.service.features.enable-pdf-export=false`.

## 7) Audience Ingestion + Mapping
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/audience/ingest` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `GET /api/v1/audience/ingestion-runs` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `GET /api/v1/audience/ingestion-runs/{runId}` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `GET /api/v1/audience/ingestion-runs/{runId}/rejections` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `POST /api/v1/audience/ingestion-runs/{runId}/replay` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `POST /api/v1/audience/mapping-profiles` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `PUT /api/v1/audience/mapping-profiles/{profileId}` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `POST /api/v1/audience/mapping-profiles/{profileId}/deactivate` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `GET /api/v1/audience/mapping-profiles` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `GET /api/v1/audience/mapping-profiles/{profileId}` | indirectly via list/load form | Partial |
| `GET /api/v1/audience/mapping-profiles/{profileId}/events` | `client/src/routes/(app)/audience/+page.svelte` | Full |
| `POST /api/v1/audience/mapping-profiles/validate` | `client/src/routes/(app)/audience/+page.svelte` | Full |

## 8) Admin Rule Control Plane
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `POST /api/v1/admin/rules` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `PUT /api/v1/admin/rules/{id}` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `GET /api/v1/admin/rules` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `GET /api/v1/admin/rules/{id}` | list/load flow (ID-driven updates) | Partial |
| `POST /api/v1/admin/rules/{id}/publish-requests` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `POST /api/v1/admin/rules/publish-requests/{publishRequestId}/approve` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `POST /api/v1/admin/rules/publish-requests/{publishRequestId}/reject` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `POST /api/v1/admin/rules/{id}/deprecate` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `POST /api/v1/admin/rules/{id}/simulate` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `POST /api/v1/admin/rules/{id}/publish-assignments` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |
| `GET /api/v1/admin/rules/capabilities` | `client/src/routes/(app)/admin/rules/+page.svelte` | Full |

## 9) System Settings
| Backend Endpoint | Frontend Coverage | Status |
|---|---|---|
| `GET /api/v1/admin/settings` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `GET /api/v1/admin/settings/category/{category}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `GET /api/v1/admin/settings/{key}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `PUT /api/v1/admin/settings/{key}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `GET /api/v1/admin/settings/campaigns/{campaignId}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `PUT /api/v1/admin/settings/campaigns/{campaignId}/{key}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |
| `DELETE /api/v1/admin/settings/campaigns/{campaignId}/{key}` | `client/src/routes/(app)/admin/settings/+page.svelte` | Full |

## 10) Non-UI / Operational Backend Features
These are intentionally backend/ops features with no direct UI page.

- Eureka registration and discovery
- Flyway schema migrations
- Redis cache layer
- Outbox dispatch and retention schedulers
- Outbox transports: `LOG`, `WEBHOOK`, `KAFKA`, `RABBITMQ`
- Messaging provisioning beans (`Kafka topic`, `RabbitMQ exchange`) when enabled
- Metrics/actuator/Prometheus/Zipkin plumbing

## Dev Mode + Infra Sync Checklist
- `docker-compose.yml` includes PostgreSQL, Redis, RabbitMQ, Kafka, Eureka, and app dependencies.
- `application-docker.yml` points Spring to container brokers (`rabbitmq`, `kafka`).
- `application.yml`:
  - disables Redis repository auto-scan noise (`spring.data.redis.repositories.enabled=false`)
  - allows Rabbit health indicator toggle via `RABBIT_HEALTH_ENABLED`
- Seed migration exists for default tenant used by audience/rule APIs:
  - `src/main/resources/db/migration/V14__seed_default_tenant.sql`
- Frontend tenant default is env driven:
  - `client/src/lib/config.ts` with `VITE_DEFAULT_TENANT_ID`

## Residual Gaps (Low Risk)
1. `GET /api/v1/evaluations/evaluatee/{evaluateeId}` is not a first-class page action.
2. Direct `GET /api/v1/admin/rules/{id}` and `GET /api/v1/audience/mapping-profiles/{profileId}` are not explicitly invoked as standalone actions; current UI uses list-driven editing.

These do not block dev-mode end-to-end use but can be added as dedicated detail fetch actions if needed.
