package com.evaluationservice.api.dto.response;

public record AudienceIngestionResponse(
        String tenantId,
        String runId,
        boolean dryRun,
        int processedRecords,
        int rejectedRecords) {
}
