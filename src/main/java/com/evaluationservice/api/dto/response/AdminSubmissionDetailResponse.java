package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.EvaluationStatus;

import java.time.Instant;

public record AdminSubmissionDetailResponse(
        String evaluationId,
        EvaluationStatus evaluationStatus,
        Instant submittedAt,
        Double totalScore,
        String campaignId,
        String campaignName,
        CampaignStatus campaignStatus,
        String assignmentId,
        String evaluatorId,
        String evaluateeId,
        String evaluatorRole,
        String stepType,
        String sectionId,
        String facultyId,
        String anonymityMode,
        String assignmentStatus) {
}
