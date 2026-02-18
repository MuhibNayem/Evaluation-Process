package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record IngestAudienceRequest(
        @NotBlank(message = "tenantId is required") String tenantId,
        @NotBlank(message = "sourceType is required") String sourceType,
        Map<String, Object> sourceConfig,
        Long mappingProfileId,
        boolean dryRun) {
}
