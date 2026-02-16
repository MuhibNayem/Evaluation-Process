package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;

import java.time.Instant;

/**
 * Response DTO for campaign data.
 */
public record CampaignResponse(
        String id,
        String name,
        String description,
        String templateId,
        int templateVersion,
        CampaignStatus status,
        Instant startDate,
        Instant endDate,
        ScoringMethod scoringMethod,
        boolean anonymousMode,
        int totalAssignments,
        long completedAssignments,
        double completionPercentage,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
