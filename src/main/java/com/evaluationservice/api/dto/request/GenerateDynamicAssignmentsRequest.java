package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for dynamic evaluator-evaluatee assignment generation.
 */
public record GenerateDynamicAssignmentsRequest(
        @NotBlank(message = "audienceSourceType is required") String audienceSourceType,
        @NotNull(message = "audienceSourceConfig is required") Map<String, Object> audienceSourceConfig,
        @NotBlank(message = "assignmentRuleType is required") String assignmentRuleType,
        @NotNull(message = "assignmentRuleConfig is required") Map<String, Object> assignmentRuleConfig,
        boolean replaceExistingAssignments,
        boolean dryRun) {
}
