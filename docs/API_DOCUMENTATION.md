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
2. Campaign lifecycle and step-window management APIs require `ROLE_ADMIN`:
3. `/api/v1/campaigns/{id}/lifecycle/**`
4. `/api/v1/campaigns/{id}/steps`
5. Rich assignment admin APIs require `ROLE_ADMIN`:
6. `/api/v1/assignments/**`

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

### PDF lifecycle operations (feature-flagged)

Feature flags:
1. `features.enable-pdf-lifecycle`
2. `features.enable-step-windows` (for step APIs)

Admin routes:
1. `POST /api/v1/campaigns/{id}/lifecycle/publish`
2. `POST /api/v1/campaigns/{id}/lifecycle/close`
3. `POST /api/v1/campaigns/{id}/lifecycle/reopen`
4. `POST /api/v1/campaigns/{id}/lifecycle/publish-results`
5. `POST /api/v1/campaigns/{id}/lifecycle/impact-preview`
6. `GET /api/v1/campaigns/{id}/lifecycle/events`
7. `GET /api/v1/campaigns/{id}/steps`
8. `PUT /api/v1/campaigns/{id}/steps`

Lifecycle action request body (optional for publish/close/reopen/publish-results):
```json
{
  "reason": "Approved by committee"
}
```

Lifecycle impact preview request:
```json
{
  "action": "PUBLISH_RESULTS"
}
```

Lifecycle impact preview response example:
```json
{
  "campaignId": "c-001",
  "action": "PUBLISH_RESULTS",
  "totalAssignments": 120,
  "completedAssignments": 96,
  "pendingAssignments": 24,
  "summary": "Results become visible to configured viewer roles."
}
```

Lifecycle events response item example:
```json
{
  "id": 101,
  "campaignId": "c-001",
  "fromStatus": "CLOSED",
  "toStatus": "RESULTS_PUBLISHED",
  "action": "PUBLISH_RESULTS",
  "actor": "admin-user",
  "reason": "Board approved release",
  "metadata": {
    "campaignName": "Spring 2026 Faculty Evaluation",
    "completionPercentage": 80.0
  },
  "createdAt": "2026-02-22T08:15:30Z"
}
```

Step upsert request example:
```json
{
  "steps": [
    {
      "stepType": "STUDENT",
      "enabled": true,
      "displayOrder": 1,
      "openAt": "2026-03-01T00:00:00Z",
      "closeAt": "2026-03-10T23:59:59Z",
      "lateAllowed": false,
      "lateDays": 0,
      "instructions": "Complete student feedback first",
      "notes": "Window is strict"
    },
    {
      "stepType": "PEER",
      "enabled": true,
      "displayOrder": 2,
      "openAt": "2026-03-11T00:00:00Z",
      "closeAt": "2026-03-20T23:59:59Z",
      "lateAllowed": true,
      "lateDays": 2,
      "instructions": "Peer review period",
      "notes": null
    }
  ]
}
```

Step response item example:
```json
{
  "id": 10,
  "campaignId": "c-001",
  "stepType": "STUDENT",
  "enabled": true,
  "displayOrder": 1,
  "openAt": "2026-03-01T00:00:00Z",
  "closeAt": "2026-03-10T23:59:59Z",
  "lateAllowed": false,
  "lateDays": 0,
  "instructions": "Complete student feedback first",
  "notes": "Window is strict"
}
```

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

### Rich assignment admin APIs (Phase 3)
1. `GET /api/v1/assignments?campaignId={id}&stepType={type}&sectionId={id}&facultyId={id}&status={status}&evaluatorId={id}&evaluateeId={id}&page=0&size=20&sortBy=updatedAt&sortDir=desc`
2. `GET /api/v1/assignments/{id}`
3. `POST /api/v1/assignments`
4. `PUT /api/v1/assignments/{id}`

List response includes:
1. `items`
2. `page`
3. `size`
4. `totalItems`
5. `totalPages`

Create request example:
```json
{
  "campaignId": "c-001",
  "evaluatorId": "e-100",
  "evaluateeId": "u-200",
  "evaluatorRole": "PEER",
  "stepType": "PEER",
  "sectionId": "SEC-A",
  "facultyId": "FAC-01",
  "anonymityMode": "VISIBLE",
  "status": "ACTIVE"
}
```

Duplicate conflict response (`409`) example:
```json
{
  "type": "https://api.evaluationservice.com/errors/duplicate-assignment",
  "title": "Duplicate Assignment",
  "status": 409,
  "detail": "Duplicate assignment tuple for campaign/evaluator/evaluatee/role",
  "campaignId": "c-001",
  "evaluatorId": "e-100",
  "evaluateeId": "u-200",
  "evaluatorRole": "PEER",
  "existingAssignmentId": "a-existing"
}
```

### Evaluator dashboard summary (Phase 3)
1. `GET /api/v1/evaluators/me/dashboard`

Response fields:
1. `assignedCount`
2. `completedCount`
3. `pendingCount`
4. `draftCount`
5. `submittedCount`
6. `completionPercentage`

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
2. When `features.enable-step-windows=true`, submission is enforced against assignment step window policy:
3. step must be enabled
4. current time must be within open/close window, or inside configured late window
5. inactive assignments are blocked

### Pre-submit validation (Phase 3)
- `POST /api/v1/evaluations/validate-submit`

Uses submit payload shape and returns machine-readable issues before final submission.
Type-aware issue codes now include:
1. `NUMERIC_REQUIRED`
2. `TEXT_REQUIRED`
3. `CHOICE_INVALID`
4. `OPTION_INVALID`
5. `BOOLEAN_REQUIRED`
6. `NPS_OUT_OF_RANGE`

### Get evaluation
- `GET /api/v1/evaluations/{id}`

### Submission receipt (Phase 3)
- `GET /api/v1/evaluations/{id}/receipt`

### Admin submission detail and reopen (Phase 3)
1. `GET /api/v1/evaluations/{id}/admin-detail` (admin only)
2. `POST /api/v1/evaluations/{id}/reopen` (admin only)

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

## Phase 4: Question Bank and Scoring Preview APIs

Question bank APIs are admin-only and require:
1. `ROLE_ADMIN`
2. `features.enable-question-bank=true`

Question bank sets:
1. `POST /api/v1/questions-bank/sets`
2. `GET /api/v1/questions-bank/sets?tenantId={tenantId}&status={status}`

Question bank items:
1. `POST /api/v1/questions-bank/sets/{setId}/items`
2. `GET /api/v1/questions-bank/sets/{setId}/items?status={status}`

Question item versions:
1. `POST /api/v1/questions-bank/items/{itemId}/versions`
2. `GET /api/v1/questions-bank/items/{itemId}/versions?status={status}`
3. `POST /api/v1/questions-bank/items/{itemId}/versions/{versionNo}/activate`
4. `GET /api/v1/questions-bank/items/{itemId}/versions/compare?fromVersion=1&toVersion=2`

Scoring preview:
1. `POST /api/v1/scoring/preview` (admin only)
2. Input: template id + sample answers + optional scoring override
3. Output: total score + section-level breakdown

Scoring preview request example:
```json
{
  "templateId": "tmpl-score",
  "answers": [
    { "questionId": "q1", "value": 8 }
  ]
}
```

---

## Phase 5: Notification Rule Engine APIs

Notification APIs are admin-only and require:
1. `ROLE_ADMIN`
2. `features.enable-notification-rule-engine=true`

Rules:
1. `POST /api/v1/admin/notifications/rules`
2. `GET /api/v1/admin/notifications/rules?campaignId={campaignId}`
3. `GET /api/v1/admin/notifications/rules/{id}`
4. `PUT /api/v1/admin/notifications/rules/{id}`
5. `DELETE /api/v1/admin/notifications/rules/{id}`

Templates:
1. `POST /api/v1/admin/notifications/templates`
2. `GET /api/v1/admin/notifications/templates?campaignId={campaignId}`
3. `GET /api/v1/admin/notifications/templates/{id}`
4. `PUT /api/v1/admin/notifications/templates/{id}`
5. `DELETE /api/v1/admin/notifications/templates/{id}`
6. `POST /api/v1/admin/notifications/templates/{id}/test-render`

Deliveries:
1. `GET /api/v1/admin/notifications/deliveries?campaignId={campaignId}&ruleId={ruleId}&status={status}`
2. `POST /api/v1/admin/notifications/deliveries/{id}/retry`

Runtime behavior:
1. `EvaluationSubmittedEvent` triggers rules with `triggerType=EVALUATION_SUBMITTED`.
2. `CampaignClosedEvent` triggers rules with `triggerType=CAMPAIGN_CLOSED`.
3. Scheduled rules run via `evaluation.service.notification.scheduler-cron` and are suppressed when the feature flag is disabled.
4. `PUBLISHED` template validation requires every `{{placeholder}}` to be listed in `requiredVariables`.

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
