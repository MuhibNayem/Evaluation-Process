package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record NotificationRuleResponse(
        Long id,
        String campaignId,
        String ruleCode,
        String triggerType,
        String audience,
        String channel,
        String scheduleExpr,
        boolean enabled,
        Map<String, Object> config,
        Instant createdAt,
        Instant updatedAt) {
}
