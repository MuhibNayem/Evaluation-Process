# FES API Evolution Policy

Last updated: 2026-02-22

Implementation snapshot:
1. Phase 2 lifecycle and step-window endpoints are now implemented under `/api/v1` with additive contracts and feature-flag gating.
2. Existing campaign endpoints remain intact for backward compatibility.

## 1. Purpose

Define how new FES parity APIs are introduced without breaking existing consumers.

## 2. Versioning Strategy

1. Default strategy: keep backward compatibility under `/api/v1`.
2. Introduce additive endpoints/resources first.
3. Do not remove or repurpose existing fields in-place during parity rollout.
4. Use feature flags for behavior changes tied to existing endpoints.
5. Use `/api/v2` only if a change cannot be represented additively.

## 3. Compatibility Rules

1. Existing request/response contracts remain valid through the full rollout.
2. New required behavior must be introduced via:
3. new endpoint
4. optional field
5. or flag-gated branch.
6. Existing enum values cannot be removed.
7. New enum values require:
8. OpenAPI update
9. release note
10. integration notice.

## 4. Endpoint Introduction Pattern

For each new module:
1. Publish OpenAPI schema first.
2. Implement endpoint behind feature flag where applicable.
3. Add integration tests.
4. Release with default-safe configuration.
5. Enable progressively per environment.

## 5. Error Contract

1. Continue using RFC 9457 `ProblemDetail`.
2. Preserve current status code semantics.
3. New validation paths must return machine-readable fields when possible.
4. Error type URIs must remain stable.

## 6. Deprecation Policy

1. Mark deprecations in OpenAPI and docs.
2. Provide replacement endpoint and mapping guidance.
3. Keep deprecated endpoint active for at least one full rollout cycle.
4. Remove only after explicit stakeholder sign-off.

## 7. Documentation Requirements

Every new or changed API requires:
1. `docs/openapi.yaml` update.
2. `docs/API_DOCUMENTATION.md` update.
3. Change note in implementation plan docs.

## 8. Release Gates

A parity API release is allowed only when:
1. Contract tests pass.
2. Migration scripts are verified.
3. Feature flag defaults are safe.
4. Rollback notes are documented.
