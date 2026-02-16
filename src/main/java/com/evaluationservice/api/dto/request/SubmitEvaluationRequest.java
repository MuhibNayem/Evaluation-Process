package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for submitting an evaluation.
 */
public record SubmitEvaluationRequest(
        @NotBlank(message = "Campaign ID is required") String campaignId,
        @NotBlank(message = "Assignment ID is required") String assignmentId,
        @NotBlank(message = "Evaluator ID is required") String evaluatorId,
        @NotBlank(message = "Evaluatee ID is required") String evaluateeId,
        @NotBlank(message = "Template ID is required") String templateId,
        @NotNull(message = "Answers are required") List<AnswerRequest> answers) {
    public record AnswerRequest(
            String questionId,
            Object value,
            List<String> selectedOptions,
            String textResponse,
            Map<String, Object> metadata) {
    }
}
