package com.evaluationservice.api.dto.response;

public record LifecycleImpactPreviewResponse(
        String campaignId,
        String action,
        long totalAssignments,
        long completedAssignments,
        long pendingAssignments,
        String summary) {
}

