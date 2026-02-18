package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record AudienceMappingProfileResponse(
        long id,
        String tenantId,
        String name,
        String sourceType,
        Map<String, String> fieldMappings,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
