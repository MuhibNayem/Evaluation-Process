package com.evaluationservice.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record CampaignLifecycleEventResponse(
        Long id,
        String campaignId,
        String fromStatus,
        String toStatus,
        String action,
        String actor,
        String reason,
        Map<String, Object> metadata,
        Instant createdAt) {
}

