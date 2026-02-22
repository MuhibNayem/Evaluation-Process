# FES Migration and Rollback Guide

Last updated: 2026-02-22

## 1. Purpose

Provide a safe rollout and rollback process for FES parity migrations and feature activation.

## 2. Rollout Model

1. Apply schema migrations first (no behavior change).
2. Deploy application with new code paths disabled by feature flags.
3. Enable features gradually:
4. local
5. test
6. staging
7. production (incremental).

## 3. Pre-Deployment Checklist

1. Database backup/snapshot completed.
2. Flyway migration dry-run validated in lower environment.
3. OpenAPI and docs updated.
4. Alerting dashboards prepared for new modules.
5. Rollback owner assigned for deployment window.

## 4. Migration Safety Requirements

1. All migration scripts must be idempotent.
2. Avoid destructive schema changes in same release as behavior cutover.
3. Use additive columns/tables first.
4. Backfills must be repeatable and bounded.
5. Large backfills must support dry-run mode.

## 5. Rollback Strategy

### 5.1 Application Rollback

1. First response is feature flag disable:
2. `features.enable-step-windows=false`
3. `features.enable-pdf-lifecycle=false`
4. `features.enable-question-bank=false`
5. `features.enable-notification-rule-engine=false`
6. Re-deploy last known good application artifact if needed.

### 5.2 Database Rollback

1. Prefer forward-fix for additive migrations.
2. Do not drop new tables/columns in emergency response.
3. If hard rollback is required, restore from pre-release snapshot.

## 6. Incident Playbook

If deployment causes functional regression:
1. Disable newly enabled feature flags.
2. Capture failing endpoints and representative payloads.
3. Check migration status (`flyway_schema_history`).
4. Review application logs and error rates.
5. Execute rollback artifact if service stability is affected.
6. Open post-incident action items before reattempt.

## 7. Post-Deployment Validation

1. Health endpoints green.
2. Core legacy flows still pass smoke tests.
3. New module endpoints return expected responses in enabled environments.
4. Error budget and latency remain within normal range.

## 8. Ownership

For each parity phase release define:
1. Engineering owner.
2. Database owner.
3. On-call escalation owner.
4. Product sign-off owner.

