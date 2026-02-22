package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAssignmentRequest(
        @NotBlank String campaignId,
        @NotBlank String evaluatorId,
        @NotBlank String evaluateeId,
        @NotBlank String evaluatorRole,
        String stepType,
        String sectionId,
        String facultyId,
        String anonymityMode,
        String status) {
}
