package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRulePublishRequest(
        @NotBlank String tenantId,
        String reasonCode,
        String comment) {
}
