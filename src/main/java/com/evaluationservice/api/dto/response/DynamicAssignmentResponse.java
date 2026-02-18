package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.EvaluatorRole;

import java.util.List;

/**
 * Response DTO for dynamic assignment generation results.
 */
public record DynamicAssignmentResponse(
        String campaignId,
        String audienceSourceType,
        String assignmentRuleType,
        boolean replaceExistingAssignments,
        boolean dryRun,
        int generatedCount,
        List<GeneratedAssignmentItem> assignments) {

    public record GeneratedAssignmentItem(
            String assignmentId,
            String evaluatorId,
            String evaluateeId,
            EvaluatorRole evaluatorRole) {
    }
}
