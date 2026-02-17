package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record MyAssignmentResponse(
        String id,
        String campaignId,
        String campaignName,
        Instant endDate,
        String evaluateeId,
        String status,
        String evaluationId) {
}
