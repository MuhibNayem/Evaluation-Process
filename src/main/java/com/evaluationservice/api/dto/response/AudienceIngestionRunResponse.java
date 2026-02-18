package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record AudienceIngestionRunResponse(
        String id,
        String tenantId,
        String sourceType,
        String status,
        boolean dryRun,
        int processedRecords,
        int rejectedRecords,
        String errorMessage,
        Instant startedAt,
        Instant endedAt) {
}
