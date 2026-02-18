package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record CreateRuleDefinitionRequest(
        @NotBlank String tenantId,
        @NotBlank String name,
        String description,
        @NotBlank String semanticVersion,
        @NotBlank String ruleType,
        @NotEmpty Map<String, Object> ruleConfig) {
}
