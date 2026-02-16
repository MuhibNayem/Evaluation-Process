package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.EvaluationStatus;

import java.time.Instant;

/**
 * Response DTO for evaluation data.
 */
public record EvaluationResponse(
        String id,
        String campaignId,
        String assignmentId,
        String evaluatorId,
        String evaluateeId,
        EvaluationStatus status,
        Double totalScore,
        int answerCount,
        Instant createdAt,
        Instant submittedAt) {
}
