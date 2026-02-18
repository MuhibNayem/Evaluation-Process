package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record AudienceIngestionRejectionResponse(
        long id,
        String runId,
        String tenantId,
        int rowNumber,
        String reason,
        String rowData,
        Instant createdAt) {
}
