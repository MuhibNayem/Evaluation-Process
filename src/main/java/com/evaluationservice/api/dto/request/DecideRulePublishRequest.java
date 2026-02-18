package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DecideRulePublishRequest(
        @NotBlank String tenantId,
        String decisionComment) {
}
