package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public record UpdateCampaignRequest(
        @NotBlank(message = "Campaign name is required") String name,
        String description,
        @NotNull(message = "Start date is required") Instant startDate,
        @NotNull(message = "End date is required") @FutureOrPresent(message = "End date must be in the future or present") Instant endDate,
        ScoringMethod scoringMethod,
        Boolean anonymousMode,
        Set<EvaluatorRole> anonymousRoles,
        Integer minimumRespondents) {
}
