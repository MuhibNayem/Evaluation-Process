package com.evaluationservice.api.dto.response;

import java.util.Map;

public record AudienceMappingValidationResponse(
        String sourceType,
        Map<String, String> normalizedMappings,
        boolean valid) {
}
