# API Documentation v1.1

**Base URL**: `http://localhost:8080`  
**Content-Type**: `application/json`

## Authentication

### Token endpoint
- `POST /api/v1/auth/login`

Request body:
```json
{
  "username": "admin",
  "password": "admin"
}
```

Success response:
```json
{
  "token": "<jwt-token>"
}
```

Mock users currently supported:
- `admin/admin`
- `evaluator/evaluator`
- `evaluatee/evaluatee`

### Auth behavior by environment
- In dev/testing, `evaluation.service.security.dev-mode=true` permits all requests.
- In secured mode, send:
```http
Authorization: Bearer <jwt-token>
```

---

## Campaigns

### Create campaign
- `POST /api/v1/campaigns`

```json
{
  "name": "Q4 Engineering Performance Review",
  "description": "Year-end 360 evaluation",
  "templateId": "tmpl_2893f9a-112",
  "templateVersion": 1,
  "startDate": "2026-10-01T09:00:00Z",
  "endDate": "2026-10-31T17:00:00Z",
  "scoringMethod": "WEIGHTED_AVERAGE",
  "anonymousMode": true,
  "anonymousRoles": ["PEER", "SUBORDINATE"],
  "minimumRespondents": 3
}
```

### Get campaign
- `GET /api/v1/campaigns/{id}`

### List campaigns
- `GET /api/v1/campaigns?status=ACTIVE&page=0&size=20`

### Activate campaign
- `POST /api/v1/campaigns/{id}/activate`

### Close campaign
- `POST /api/v1/campaigns/{id}/close`

### Archive campaign
- `POST /api/v1/campaigns/{id}/archive`

### Add assignments
- `POST /api/v1/campaigns/{id}/assignments`

```json
{
  "assignments": [
    {
      "evaluatorId": "user_101",
      "evaluateeId": "user_200",
      "evaluatorRole": "SUPERVISOR"
    },
    {
      "evaluatorId": "user_102",
      "evaluateeId": "user_200",
      "evaluatorRole": "PEER"
    }
  ]
}
```

### Extend deadline
- `POST /api/v1/campaigns/{id}/extend-deadline`

```json
{
  "newEndDate": "2026-11-07T17:00:00Z"
}
```

### Campaign progress
- `GET /api/v1/campaigns/{id}/progress`

Response:
```json
{
  "completionPercentage": 30.0
}
```

### My assignments
- `GET /api/v1/campaigns/assignments/me`

Response example:
```json
[
  {
    "id": "assign_1",
    "campaignId": "camp_1",
    "campaignName": "Q4 Engineering Performance Review",
    "endDate": "2026-10-31T17:00:00Z",
    "evaluateeId": "user_200",
    "status": "PENDING",
    "evaluationId": null
  }
]
```

---

## Evaluations

### Submit evaluation
- `POST /api/v1/evaluations`

```json
{
  "campaignId": "camp_1",
  "assignmentId": "assign_1",
  "evaluatorId": "user_101",
  "evaluateeId": "user_200",
  "templateId": "tmpl_1",
  "answers": [
    {
      "questionId": "q_technical_01",
      "value": 4.5,
      "textResponse": "Excellent grasp of Java",
      "selectedOptions": [],
      "metadata": { "confidence": "high" }
    }
  ]
}
```

Note:
- If a non-anonymous authenticated user is present, backend uses the authenticated identity as `evaluatorId`.

### Get evaluation
- `GET /api/v1/evaluations/{id}`

### Update evaluation draft
- `PUT /api/v1/evaluations/{id}`

```json
{
  "answers": [
    {
      "questionId": "q_technical_01",
      "value": 5,
      "textResponse": "Updated answer",
      "selectedOptions": [],
      "metadata": {}
    }
  ]
}
```

### List by campaign
- `GET /api/v1/evaluations/campaign/{campaignId}?page=0&size=20`

### List by evaluatee
- `GET /api/v1/evaluations/evaluatee/{evaluateeId}?page=0&size=20`

### Flag evaluation
- `POST /api/v1/evaluations/{id}/flag`

### Invalidate evaluation
- `POST /api/v1/evaluations/{id}/invalidate`

### Evaluation response shape
```json
{
  "id": "eval_1",
  "campaignId": "camp_1",
  "assignmentId": "assign_1",
  "evaluatorId": "user_101",
  "evaluateeId": "user_200",
  "templateId": "tmpl_1",
  "answers": [
    {
      "id": "ans_1",
      "questionId": "q_technical_01",
      "value": "4.5",
      "selectedOptions": [],
      "textResponse": "Excellent",
      "metadata": {"confidence": "high"}
    }
  ],
  "status": "COMPLETED",
  "totalScore": 84.5,
  "answerCount": 1,
  "createdAt": "2026-10-10T10:00:00Z",
  "submittedAt": "2026-10-10T10:05:00Z"
}
```

---

## Templates

### Create template
- `POST /api/v1/templates`

### Get template
- `GET /api/v1/templates/{id}`

### List templates
- `GET /api/v1/templates?category=PERFORMANCE&page=0&size=20`

### Publish template
- `POST /api/v1/templates/{id}/publish`

### Deprecate template
- `POST /api/v1/templates/{id}/deprecate`

### Delete template
- `DELETE /api/v1/templates/{id}`

---

## Reports

### Individual report
- `GET /api/v1/reports/individual?evaluateeId={evaluateeId}&campaignId={campaignId}`

### Campaign report
- `GET /api/v1/reports/campaign/{campaignId}`

### Export CSV
- `GET /api/v1/reports/export/csv/{campaignId}`

### Export PDF
- `GET /api/v1/reports/export/pdf?evaluateeId={evaluateeId}&campaignId={campaignId}`

Report feature flags:
- `evaluation.service.features.enable-reports`
- `evaluation.service.features.enable-csv-export`
- `evaluation.service.features.enable-pdf-export`

---

## Admin Settings

### List all settings
- `GET /api/v1/admin/settings`

### List settings by category
- `GET /api/v1/admin/settings/category/{category}`

### Get setting by key
- `GET /api/v1/admin/settings/{key}`

### Update setting
- `PUT /api/v1/admin/settings/{key}`

```json
{
  "value": "true"
}
```

### Get campaign overrides
- `GET /api/v1/admin/settings/campaigns/{campaignId}`

### Set campaign override
- `PUT /api/v1/admin/settings/campaigns/{campaignId}/{key}`

```json
{
  "value": "false"
}
```

### Remove campaign override
- `DELETE /api/v1/admin/settings/campaigns/{campaignId}/{key}`

---

## Error Format

The API returns RFC 9457 `ProblemDetail` for handled failures.

Typical statuses:
- `200 OK`
- `201 Created`
- `204 No Content`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`
- `422 Unprocessable Entity`
- `500 Internal Server Error`
