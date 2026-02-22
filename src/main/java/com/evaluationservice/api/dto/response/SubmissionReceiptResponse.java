package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.EvaluationStatus;

import java.time.Instant;

public record SubmissionReceiptResponse(
        String evaluationId,
        String campaignId,
        String assignmentId,
        String evaluatorId,
        String evaluateeId,
        EvaluationStatus status,
        Instant submittedAt,
        Double totalScore) {
}
