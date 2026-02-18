package com.evaluationservice.domain.event;

import java.time.Instant;
import java.util.Map;

public record AudienceMappingProfileLifecycleEvent(
        Long profileId,
        String tenantId,
        String eventType,
        String actor,
        Map<String, Object> payload,
        Instant occurredAt) {
}
