package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record QuestionBankSetResponse(
        Long id,
        String tenantId,
        String name,
        String versionTag,
        String owner,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
