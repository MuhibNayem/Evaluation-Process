package com.evaluationservice.api.dto.request;

import java.util.List;

public record UpdateNotificationTemplateRequest(
        String name,
        String channel,
        String subject,
        String body,
        List<String> requiredVariables,
        String status) {
}
