package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record CampaignStepResponse(
        Long id,
        String campaignId,
        String stepType,
        boolean enabled,
        int displayOrder,
        Instant openAt,
        Instant closeAt,
        boolean lateAllowed,
        int lateDays,
        String instructions,
        String notes) {
}

