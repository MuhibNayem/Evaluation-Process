# Features Guide

This document provides a comprehensive deep-dive into the Evaluation Service's capabilities, technical rules, and automation behaviors.

---

## 1. Evaluation Campaigns

A **Campaign** is the central orchestrator of an evaluation cycle.

### Lifecycle Management
The system enforces a strict state machine to ensure data integrity.
*   **`DRAFT`**: Configuration phase.
    *   *Allowed*: Update details, Add assignments, Delete.
    *   *Prohibited*: Submitting evaluations.
*   **`SCHEDULED`**: Locked and waiting for start date.
    *   *Automation*: Automatically transitions to `ACTIVE` when `startDate` arrives.
*   **`ACTIVE`**: Open for submissions.
    *   *Allowed*: extend deadline, submit evaluations.
    *   *Prohibited*: modifying template, adding new assignments (rules may vary by config).
    *   *Automation*: Automatically transitions to `CLOSED` when `endDate` passed.
*   **`CLOSED`**: Cycle finished.
    *   *Allowed*: Reporting, Archiving.
    *   *Prohibited*: New submissions, Modifications.
*   **`ARCHIVED`**: Read-only historical record.

### Configuration Options
*   **Anonymous Mode**: Hides evaluator identities in reports.
    *   *Granular Control*: Can apply to specific roles only (e.g., anonymize `PEER` but show `SUPERVISOR`).
*   **Minimum Respondents**: Threshold (default: 1) to protect anonymity. If fewer than $N$ evaluators respond, the report section is withheld.

---

## 2. Automation & Scheduling

The `CampaignScheduler` runs background tasks (default: hourly) to automate the lifecycle.

### Auto-Activation
*   Scans for `SCHEDULED` campaigns where `StartDate <= NOW`.
*   Activates them instantly, making them visible to users.

### Auto-Closure
*   Scans for `ACTIVE` campaigns where `EndDate < NOW`.
*   Closes them to prevent late submissions.
*   Triggers `CampaignClosedEvent` (for analytics/integration).

### Deadline Reminders
*   Scans for `ACTIVE` campaigns ending within `X` days (default: 3).
*   Identifies users with **incomplete assignments**.
*   Triggers a `REMINDER` notification to nudge the evaluator.

---

## 3. Notification System

The service uses an asynchronous **Webhook** model to integrate with external communication tools (Slack, Email Service, PagerDuty).

**Configuration**: `evaluation.service.notification.webhook-url`

### Supported Events
1.  **`REMINDER`**: Sent when a deadline is approaching.
    *   *Payload*: `{ "type": "REMINDER", "recipientId": "...", "campaign": "...", "message": "..." }`
2.  **`COMPLETION`**: Sent when an evaluator finishes all their tasks.
    *   *Payload*: `{ "type": "COMPLETION", "recipientId": "..." }`
3.  **`DEADLINE_EXTENSION`**: Sent when an admin extends the campaign.

---

## 4. Templates & Scoring

### Template Versioning
*   Templates are versioned (v1, v2, v3).
*   Campaigns are linked to a specific **snapshot** version.
*   *Benefit*: You can update a template for next year without breaking the reports for previous years.

### Scoring Engines
The system includes 5 distinct scoring algorithms:

1.  **`WEIGHTED_AVERAGE`**:
    *   *Logic*: $\sum (SectionScore \times SectionWeight) / \sum Weights$
    *   *Best for*: Standard performance reviews (e.g. Goals 50%, Values 50%).
2.  **`SIMPLE_AVERAGE`**:
    *   *Logic*: Arithmetic mean of all numerical answers.
    *   *Best for*: Simple feedback forms.
3.  **`MEDIAN`**:
    *   *Logic*: Middle value of sorted scores.
    *   *Best for*: Removing the effect of outliers (extremely harsh/lenient graders).
4.  **`PERCENTILE_RANK`**:
    *   *Logic*: Scores are converted to percentiles (0-100) relative to the campaign population.
    *   *Best for*: Stack ranking.
5.  **`CUSTOM_FORMULA`**: (Advanced)
    *   Execute domain-specific logic strings defined in the template.

---

## 5. Reporting Module

### Individual Reports
Generates a comprehensive profile for a single evaluatee.
*   **Strengths & Weaknesses**: Automatically identifies top 3 and bottom 3 sections based on scores.
*   **Gap Analysis**: (If Self-Evaluation is enabled) Compares `SELF` score vs `OTHERS` score to highlight perception gaps.

### Campaign Reports
Aggregates data for the entire cohort.
*   **Participation Rate**: % of assigned evaluations completed.
*   **Score Distribution**: Histograms of scores (e.g., how many people got > 90?).
*   **Section Averages**: Which competencies is the organization strongest/weakest in?

### Export Formats
*   **CSV**: Raw data dump for Excel/Tableau. Includes all answers, timestamps, and metadata.
*   **PDF**: (Feature Flagged) Printable executive summary.

---

## 6. Dynamic System Settings

Admins can tune system behavior at runtime via the API.

### Override Hierarchy
The system resolves settings in this order (highest priority first):
1.  **Campaign Override**: "Campaign A allows PDF export".
2.  **System Global**: "PDF export is disabled system-wide".
3.  **Code Default**: "False".

This allows you to safety-test new features (like PDF export) on a pilot campaign before rolling it out to everyone.

---

## 7. Security & Architecture

### Authorization
*   **RBAC**: Access is strictly controlled by roles (`ADMIN`, `USER`).
*   **Data Isolation**: Users can only fetch evaluations where they are the *Evaluator* or *Evaluatee*.
*   **Token-Based**: Stateless JWT authentication.

### Resilience
*   **Circuit Breakers**: Calls to the Notification Webhook are wrapped in a Circuit Breaker. If the webhook server goes down, the system stops trying for a cooldown period to prevent resource exhaustion.
*   **Virtual Threads**: Uses Java 21+ Virtual Threads for high-throughput concurrency, allowing thousands of concurrent reporting requests without blocking OS threads.
