package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.List;

public record NotificationTemplateResponse(
        Long id,
        String campaignId,
        String templateCode,
        String name,
        String channel,
        String subject,
        String body,
        List<String> requiredVariables,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
