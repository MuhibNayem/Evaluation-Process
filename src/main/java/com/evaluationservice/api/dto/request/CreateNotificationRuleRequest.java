package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateNotificationRuleRequest(
        @NotBlank String campaignId,
        @NotBlank String ruleCode,
        @NotBlank String triggerType,
        @NotBlank String audience,
        @NotBlank String channel,
        String scheduleExpr,
        boolean enabled,
        Map<String, Object> config) {
}
