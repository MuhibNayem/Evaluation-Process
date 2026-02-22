package com.evaluationservice.api.dto.request;

import java.util.Map;

public record UpdateNotificationRuleRequest(
        String triggerType,
        String audience,
        String channel,
        String scheduleExpr,
        Boolean enabled,
        Map<String, Object> config) {
}
