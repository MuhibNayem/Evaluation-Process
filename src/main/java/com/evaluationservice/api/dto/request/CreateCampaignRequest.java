package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Request DTO for creating evaluation campaigns.
 */
public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required") String name,
        String description,
        @NotBlank(message = "Template ID is required") String templateId,
        int templateVersion,
        @NotNull(message = "Start date is required") Instant startDate,
        @NotNull(message = "End date is required") Instant endDate,
        ScoringMethod scoringMethod,
        boolean anonymousMode,
        Set<EvaluatorRole> anonymousRoles,
        String audienceSourceType,
        Map<String, Object> audienceSourceConfig,
        String assignmentRuleType,
        Map<String, Object> assignmentRuleConfig,
        int minimumRespondents) {
}
