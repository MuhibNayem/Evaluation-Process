package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TestNotificationTemplateRequest(
        @NotBlank String recipient,
        Map<String, Object> variables) {
}
