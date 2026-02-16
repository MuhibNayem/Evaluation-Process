package com.evaluationservice.domain.event;

import com.evaluationservice.domain.value.CampaignId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when a campaign is closed.
 */
public record CampaignClosedEvent(
        CampaignId campaignId,
        double completionPercentage,
        long totalAssignments,
        long completedAssignments,
        Instant occurredAt) {
    public CampaignClosedEvent {
        Objects.requireNonNull(campaignId);
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }
}
