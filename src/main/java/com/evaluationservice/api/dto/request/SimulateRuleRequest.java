package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SimulateRuleRequest(
        @NotBlank String tenantId,
        @NotBlank String audienceSourceType,
        @NotNull Map<String, Object> audienceSourceConfig,
        boolean diagnosticMode) {
}
