package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record RulePublishRequestResponse(
        Long id,
        Long ruleDefinitionId,
        String tenantId,
        String status,
        String reasonCode,
        String comment,
        String requestedBy,
        Instant requestedAt,
        String decidedBy,
        Instant decidedAt,
        String decisionComment) {
}
