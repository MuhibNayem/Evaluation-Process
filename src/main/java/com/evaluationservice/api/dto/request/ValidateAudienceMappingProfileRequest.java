package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ValidateAudienceMappingProfileRequest(
        @NotBlank(message = "sourceType is required") String sourceType,
        @NotEmpty(message = "fieldMappings is required") Map<String, String> fieldMappings) {
}
