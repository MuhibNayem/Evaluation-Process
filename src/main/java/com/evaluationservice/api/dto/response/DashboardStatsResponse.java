package com.evaluationservice.api.dto.response;

public record DashboardStatsResponse(
        long totalCampaigns,
        long activeCampaigns,
        long totalTemplates,
        long activeEvaluations, // Pending
        long completedEvaluations,
        double completionRate,
        java.util.List<ActivityResponse> recentActivity) {
    public record ActivityResponse(
            String title,
            String description,
            java.time.Instant timestamp,
            String type) {
    }
}
