package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record RuleDefinitionResponse(
        Long id,
        String tenantId,
        String name,
        String description,
        String semanticVersion,
        String status,
        String ruleType,
        Map<String, Object> ruleConfig,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
}
