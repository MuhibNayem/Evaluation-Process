# API Documentation v1.0

**Base URL**: `http://localhost:8080`
**Content-Type**: `application/json`

## 🔐 Authentication
All endpoints require a specific HTTP Header for authentication:

```http
Authorization: Bearer <your-jwt-token>
```
*   **401 Unauthorized**: Missing or invalid token.
*   **403 Forbidden**: Token valid but user lacks permission.

---

## 📦 Campaigns
Lifecycle management for evaluation cycles.

### 1. Create a Campaign
**POST** `/api/v1/campaigns`

Create a new evaluation cycle linked to a template.

**Request Body:**
```json
{
  "name": "Q4 Engineering Performance Review",
  "description": "Year-end 360 evaluation for all engineering levels.",
  "templateId": "tmpl_2893f9a-112",
  "templateVersion": 1,
  "startDate": "2023-10-01T09:00:00Z",
  "endDate": "2023-10-31T17:00:00Z",
  "scoringMethod": "WEIGHTED_AVERAGE",
  "anonymousMode": true,
  "anonymousRoles": ["PEER", "SUBORDINATE"],
  "minimumRespondents": 3
}
```
*   `scoringMethod`: `WEIGHTED_AVERAGE`, `SIMPLE_AVERAGE`, `MEDIAN`, `PERCENTILE_RANK`
*   `anonymousRoles`: Roles whose feedback will be anonymized in reports.

**Response (201 Created):**
```json
{
  "id": "camp_550e8400-e29b",
  "name": "Q4 Engineering Performance Review",
  "status": "DRAFT",
  "createdAt": "2023-09-15T10:00:00Z",
  "...": "other fields"
}
```

### 2. Add Assignments (Batch)
**POST** `/api/v1/campaigns/{id}/assignments`

Assign multiple evaluators to evaluatees in a single batch.

**Request Body:**
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

### 3. Get Campaign Overview
**GET** `/api/v1/campaigns/{id}`

Retrieves campaign details including progress stats.

**Response (200 OK):**
```json
{
  "id": "camp_550e8400-e29b",
  "status": "ACTIVE",
  "totalAssignments": 150,
  "completedAssignments": 45,
  "completionPercentage": 30.0,
  "startDate": "2023-10-01T09:00:00Z"
}
```

---

## 📝 Evaluations
Submission and retrieval of evaluation forms.

### 1. Submit Evaluation
**POST** `/api/v1/evaluations`

Submit a completed evaluation. This operation is idempotent.

**Request Body:**
```json
{
  "campaignId": "camp_550e8400-e29b",
  "assignmentId": "assign_778899",
  "evaluatorId": "user_101",
  "evaluateeId": "user_200",
  "templateId": "tmpl_2893f9a-112",
  "answers": [
    {
      "questionId": "q_technical_01",
      "value": 4.5,
      "textResponse": "Excellent grasp of Java 25 features.",
      "selectedOptions": [],
      "metadata": { "confidence": "high" }
    },
    {
      "questionId": "q_leadership_02",
      "value": null,
      "textResponse": "Needs to delegate more.",
      "selectedOptions": ["opt_needs_improvement"]
    }
  ]
}
```

### 2. List Evaluations
**GET** `/api/v1/evaluations/campaign/{campaignId}`

List all submissions for a campaign.

**Query Parameters:**
*   `page`: (int) Page number (0-indexed). Default: 0.
*   `size`: (int) Page size. Default: 20.

---

## 📊 Reports
Data extraction and analysis.

### 1. Get Campaign Report
**GET** `/api/v1/reports/campaign/{campaignId}`

Returns aggregated statistics for the entire campaign.

**Response (200 OK):**
```json
{
  "campaignId": "camp_550e8400-e29b",
  "totalEvaluations": 145,
  "averageScore": 84.5,
  "participationRate": 96.6,
  "distribution": {
    "90-100": 15,
    "80-89": 85,
    "70-79": 40,
    "0-69": 5
  }
}
```

### 2. Download CSV Export
**GET** `/api/v1/reports/export/csv/{campaignId}`

Downloads a `.csv` file containing raw evaluation data for offline analysis.

---

## 📋 Templates
Form scaffolding and versioning.

### 1. Create Template
**POST** `/api/v1/templates`

**Request Body:**
```json
{
  "name": "Manager Review 2024",
  "description": "Standard template for management track",
  "category": "PERFORMANCE",
  "scoringMethod": "WEIGHTED_AVERAGE"
}
```
*   *Note*: Questions and sections are added via separate updates (not shown here for brevity, usually part of a larger PUT).

### 2. Publish Template
**POST** `/api/v1/templates/{id}/publish`

Locks the template version. It can now be used in campaigns but cannot be modified.

---

## ⚙️ System Settings (Admin)
Dynamic configuration management.

### 1. Update Global Setting
**PUT** `/api/v1/admin/settings/{key}`

**Request Body:**
```json
{
  "value": "true"
}
```
**Example Keys:**
*   `evaluation.service.features.enable-reports`
*   `evaluation.service.pagination.default-page-size`

### 2. Override for Campaign
**PUT** `/api/v1/admin/settings/campaigns/{campaignId}/{key}`

Apply a setting strictly to one campaign (e.g., enable PDF export for executives only).

---

## ⚠️ Error Handling
The API uses [RFC 9457 Problem Details](https://datatracker.ietf.org/doc/html/rfc9457) for error responses.

**Example Error (422 Unprocessable Entity):**
```json
{
  "type": "https://api.evaluationservice.com/errors/campaign-not-active",
  "title": "Campaign Not Active",
  "status": 422,
  "detail": "Campaign 'camp_550e8400' is currently CLOSED. Submissions are not accepted.",
  "instance": "/api/v1/evaluations",
  "errorCode": "CAMPAIGN_CLOSED"
}
```

**Common Status Codes:**
*   `200 OK`: Success.
*   `201 Created`: Resource successfully created.
*   `400 Bad Request`: Validation failure (missing fields).
*   `401 Unauthorized`: Invalid or missing token.
*   `403 Forbidden`: Insufficient permissions.
*   `404 Not Found`: Resource ID does not exist.
*   `409 Conflict`: State conflict (e.g., modifying a published template).
*   `422 Unprocessable Entity`: Business logic violation (e.g., campaign closed).
*   `500 Internal Server Error`: Server-side failure.
