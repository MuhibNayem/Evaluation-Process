package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record CreateAudienceMappingProfileRequest(
        @NotBlank(message = "tenantId is required") String tenantId,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "sourceType is required") String sourceType,
        @NotEmpty(message = "fieldMappings is required") Map<String, String> fieldMappings,
        Boolean active) {
}
