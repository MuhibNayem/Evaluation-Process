package com.evaluationservice.api.dto.response;

import java.time.Instant;

public record AssignmentResponse(
        String id,
        String campaignId,
        String evaluatorId,
        String evaluateeId,
        String evaluatorRole,
        boolean completed,
        String evaluationId,
        String stepType,
        String sectionId,
        String facultyId,
        String anonymityMode,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
