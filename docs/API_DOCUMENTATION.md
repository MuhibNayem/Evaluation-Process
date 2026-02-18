# API Documentation v1.2

## Changelog (v1.1 -> v1.2)

Release context:
1. Align documentation with implemented backend redesign through Phase 5.

Added endpoint groups:
1. Audience ingestion APIs under `/api/v1/audience/**`:
2. Ingest, run listing/details, rejection listing, replay.
3. Mapping profile CRUD/lifecycle/validation APIs.
4. Rule control plane APIs under `/api/v1/admin/rules/**`:
5. Rule draft create/update/list/get.
6. Publish request workflow (request/approve/reject).
7. Rule deprecate, simulation, publish assignments, capabilities.
8. Assignment migration safety endpoints:
9. `GET /api/v1/campaigns/{id}/assignments/reconcile`
10. `GET /api/v1/campaigns/assignments/reconcile/report`
11. `POST /api/v1/campaigns/assignments/backfill`
12. Dashboard stats endpoint:
13. `GET /api/v1/dashboard/stats`

Updated endpoint coverage:
1. Template update documented: `PUT /api/v1/templates/{id}`.
2. Admin settings and campaign override routes documented fully.
3. Dynamic assignment docs aligned with supported runtime rule types and source types.

Schema/contract updates:
1. Request/response contracts now reflect current DTOs for all implemented controllers.
2. Rule and assignment governance payloads documented (simulation, publish request/decision, capabilities).
3. Audience ingestion payloads documented with `sourceConfig`, `mappingProfileId`, and replay options.

Auth and behavior clarifications:
1. Documented dev-mode authentication bypass behavior explicitly.
2. Documented `ROLE_ADMIN` requirement for rule control-plane endpoints.
3. Documented report endpoint feature-flag behavior (`403` when disabled).

Base URL: `http://localhost:8080`  
Content-Type: `application/json`

## Authentication and Authorization

Auth in this service is environment-dependent:
1. `evaluation.service.security.dev-mode=true`:
2. All routes are permitted.
3. Mock login endpoint is active: `POST /api/v1/auth/login`.
4. `evaluation.service.security.dev-mode=false`:
5. JWT auth is enforced for protected routes.
6. Send `Authorization: Bearer <token>`.

Admin-only APIs:
1. `/api/v1/admin/rules/**` requires `ROLE_ADMIN` (method-level guard).

### Mock login (dev mode only)
- `POST /api/v1/auth/login`

Request:
```json
{
  "username": "admin",
  "password": "admin"
}
```

Supported credentials:
1. `admin/admin`
2. `evaluator/evaluator`
3. `evaluatee/evaluatee`

Response:
```json
{
  "token": "<jwt-token>"
}
```

---

## Campaign APIs

### Create campaign
- `POST /api/v1/campaigns`

### Get campaign
- `GET /api/v1/campaigns/{id}`

### Update campaign
- `PUT /api/v1/campaigns/{id}`

Constraints:
1. `name`, `startDate`, `endDate` required.
2. `endDate` must be present/future by DTO validation.

### List campaigns
- `GET /api/v1/campaigns?status=ACTIVE&page=0&size=20`

### Lifecycle operations
1. `POST /api/v1/campaigns/{id}/activate`
2. `POST /api/v1/campaigns/{id}/close`
3. `POST /api/v1/campaigns/{id}/archive`

### Manual assignment
- `POST /api/v1/campaigns/{id}/assignments`

Request:
```json
{
  "assignments": [
    {
      "evaluatorId": "user_101",
      "evaluateeId": "user_200",
      "evaluatorRole": "SUPERVISOR"
    }
  ]
}
```

### Dynamic assignment generation
- `POST /api/v1/campaigns/{id}/assignments/dynamic`

Supported rule types:
1. `ALL_TO_ALL`
2. `ROUND_ROBIN`
3. `MANAGER_HIERARCHY`
4. `ATTRIBUTE_MATCH`

Supported audience source types in engine:
1. `INLINE`
2. `DIRECTORY_SNAPSHOT`

Request example:
```json
{
  "audienceSourceType": "INLINE",
  "audienceSourceConfig": {
    "participants": [
      { "userId": "t1", "section": "A", "supervisorId": "h1" },
      { "userId": "s1", "section": "A", "supervisorId": "t1" }
    ]
  },
  "assignmentRuleType": "ATTRIBUTE_MATCH",
  "assignmentRuleConfig": {
    "matchAttribute": "section",
    "evaluatorRole": "PEER",
    "maxEvaluatorsPerEvaluatee": 2,
    "allowSelfEvaluation": false
  },
  "replaceExistingAssignments": false,
  "dryRun": true
}
```

### Extend deadline
- `POST /api/v1/campaigns/{id}/extend-deadline`

Request:
```json
{
  "newEndDate": "2026-12-15T17:00:00Z"
}
```

### Campaign progress
- `GET /api/v1/campaigns/{id}/progress`

### My assignments
- `GET /api/v1/campaigns/assignments/me`

### Assignment parity and migration safety
1. `GET /api/v1/campaigns/{id}/assignments/reconcile`
2. `GET /api/v1/campaigns/assignments/reconcile/report?maxCampaigns=1000`
3. `POST /api/v1/campaigns/assignments/backfill?dryRun=true&maxCampaigns=1000`

---

## Evaluation APIs

### Submit evaluation
- `POST /api/v1/evaluations`

Required fields:
1. `campaignId`
2. `assignmentId`
3. `evaluatorId`
4. `evaluateeId`
5. `templateId`
6. `answers`

Note:
1. If authenticated non-anonymous user exists, backend uses authenticated identity as evaluator.

### Get evaluation
- `GET /api/v1/evaluations/{id}`

### Save draft
- `PUT /api/v1/evaluations/{id}`

### List evaluations
1. `GET /api/v1/evaluations/campaign/{campaignId}?page=0&size=20`
2. `GET /api/v1/evaluations/evaluatee/{evaluateeId}?page=0&size=20`

### Moderation actions
1. `POST /api/v1/evaluations/{id}/flag`
2. `POST /api/v1/evaluations/{id}/invalidate`

---

## Template APIs

1. `POST /api/v1/templates`
2. `PUT /api/v1/templates/{id}`
3. `GET /api/v1/templates/{id}`
4. `GET /api/v1/templates?category=PERFORMANCE&page=0&size=20`
5. `POST /api/v1/templates/{id}/publish`
6. `POST /api/v1/templates/{id}/deprecate`
7. `DELETE /api/v1/templates/{id}`

---

## Report APIs

Feature flags:
1. `evaluation.service.features.enable-reports`
2. `evaluation.service.features.enable-csv-export`
3. `evaluation.service.features.enable-pdf-export`

When disabled, endpoint returns `403`.

Endpoints:
1. `GET /api/v1/reports/individual?evaluateeId={evaluateeId}&campaignId={campaignId}`
2. `GET /api/v1/reports/campaign/{campaignId}`
3. `GET /api/v1/reports/export/csv/{campaignId}`
4. `GET /api/v1/reports/export/pdf?evaluateeId={evaluateeId}&campaignId={campaignId}`

---

## Admin Settings APIs

1. `GET /api/v1/admin/settings`
2. `GET /api/v1/admin/settings/category/{category}`
3. `GET /api/v1/admin/settings/{key}`
4. `PUT /api/v1/admin/settings/{key}`
5. `GET /api/v1/admin/settings/campaigns/{campaignId}`
6. `PUT /api/v1/admin/settings/campaigns/{campaignId}/{key}`
7. `DELETE /api/v1/admin/settings/campaigns/{campaignId}/{key}`

Write payload format:
```json
{
  "value": "true"
}
```

---

## Audience Ingestion APIs (Phase 3)

### Ingestion and replay
1. `POST /api/v1/audience/ingest`
2. `GET /api/v1/audience/ingestion-runs?tenantId={tenantId}&limit=50`
3. `GET /api/v1/audience/ingestion-runs/{runId}`
4. `GET /api/v1/audience/ingestion-runs/{runId}/rejections?limit=200`
5. `POST /api/v1/audience/ingestion-runs/{runId}/replay`

Ingest request shape:
```json
{
  "tenantId": "tenant-001",
  "sourceType": "CSV",
  "sourceConfig": {
    "entityType": "PERSON",
    "validationProfile": "strict",
    "content": "person_id,display_name,email,active\\nu1,Teacher One,t1@example.edu,true"
  },
  "mappingProfileId": 101,
  "dryRun": true
}
```

### Mapping profile management
1. `POST /api/v1/audience/mapping-profiles`
2. `PUT /api/v1/audience/mapping-profiles/{profileId}`
3. `POST /api/v1/audience/mapping-profiles/{profileId}/deactivate`
4. `GET /api/v1/audience/mapping-profiles?tenantId={tenantId}`
5. `GET /api/v1/audience/mapping-profiles/{profileId}?tenantId={tenantId}`
6. `GET /api/v1/audience/mapping-profiles/{profileId}/events?tenantId={tenantId}&limit=50`
7. `POST /api/v1/audience/mapping-profiles/validate`

---

## Rule Control Plane APIs (Phase 4/5)

Base path: `/api/v1/admin/rules`  
Access: `ROLE_ADMIN`

### Rule definition lifecycle
1. `POST /api/v1/admin/rules`
2. `PUT /api/v1/admin/rules/{id}`
3. `GET /api/v1/admin/rules?tenantId={tenantId}&status={status}`
4. `GET /api/v1/admin/rules/{id}?tenantId={tenantId}`

### Publish approval workflow
1. `POST /api/v1/admin/rules/{id}/publish-requests`
2. `POST /api/v1/admin/rules/publish-requests/{publishRequestId}/approve`
3. `POST /api/v1/admin/rules/publish-requests/{publishRequestId}/reject`
4. `POST /api/v1/admin/rules/{id}/deprecate`

### Simulation and assignment publishing
1. `POST /api/v1/admin/rules/{id}/simulate`
2. `POST /api/v1/admin/rules/{id}/publish-assignments`
3. `GET /api/v1/admin/rules/capabilities`

Workflow guardrails (config):
1. `evaluation.service.admin.publish-lock-enabled`
2. `evaluation.service.admin.require-four-eyes-approval`

Behavior:
1. If publish lock enabled, assignment publication requires rule status `PUBLISHED`.
2. If 4-eyes enabled, requester cannot approve own publish request.

---

## Dashboard API

- `GET /api/v1/dashboard/stats`

Returns:
1. Campaign counts
2. Template counts
3. Evaluation counts
4. Completion rate
5. Recent activity list

---

## OpenAPI Source of Truth

Use the machine-readable contract for schema-accurate payloads:
- `docs/openapi.yaml`
