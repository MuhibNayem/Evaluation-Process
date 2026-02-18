package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record AudienceMappingProfileEventResponse(
        long id,
        long profileId,
        String tenantId,
        String eventType,
        String actor,
        Map<String, Object> payload,
        Instant createdAt) {
}
