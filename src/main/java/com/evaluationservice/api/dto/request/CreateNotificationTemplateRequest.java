package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateNotificationTemplateRequest(
        String campaignId,
        @NotBlank String templateCode,
        @NotBlank String name,
        @NotBlank String channel,
        String subject,
        @NotBlank String body,
        List<String> requiredVariables,
        String status) {
}
