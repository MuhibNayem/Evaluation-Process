package com.evaluationservice.api.dto.response;

public record EvaluatorDashboardResponse(
        String evaluatorId,
        long assignedCount,
        long completedCount,
        long pendingCount,
        long draftCount,
        long submittedCount,
        double completionPercentage) {
}
