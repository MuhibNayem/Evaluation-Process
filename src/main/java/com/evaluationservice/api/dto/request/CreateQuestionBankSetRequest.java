package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateQuestionBankSetRequest(
        String tenantId,
        @NotBlank String name,
        String versionTag,
        String owner) {
}
