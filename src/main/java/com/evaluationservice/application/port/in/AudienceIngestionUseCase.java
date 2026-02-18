package com.evaluationservice.application.port.in;

import java.util.Map;

/**
 * Inbound use case scaffold for canonical audience ingestion.
 * Full connector execution is implemented in later phases.
 */
public interface AudienceIngestionUseCase {

    record IngestionRequest(
            String tenantId,
            String sourceType,
            Map<String, Object> sourceConfig,
            Long mappingProfileId,
            boolean dryRun) {
    }

    record IngestionResult(
            String tenantId,
            String runId,
            boolean dryRun,
            int processedRecords,
            int rejectedRecords) {
    }

    IngestionResult ingest(IngestionRequest request);

    IngestionResult replay(String runId, Boolean dryRunOverride);
}
