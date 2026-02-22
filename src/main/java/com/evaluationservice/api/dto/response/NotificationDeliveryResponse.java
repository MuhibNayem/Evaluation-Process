package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record NotificationDeliveryResponse(
        Long id,
        String campaignId,
        Long ruleId,
        Long templateId,
        String recipient,
        String channel,
        String status,
        String errorCode,
        String errorMessage,
        Map<String, Object> payload,
        Instant sentAt,
        Instant deliveredAt,
        Instant createdAt) {
}
