package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PublishRuleAssignmentsRequest(
        @NotBlank String tenantId,
        @NotBlank String campaignId,
        @NotBlank String audienceSourceType,
        @NotNull Map<String, Object> audienceSourceConfig,
        boolean replaceExistingAssignments,
        boolean dryRun) {
}
