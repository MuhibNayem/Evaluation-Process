package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.EvaluatorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for adding campaign assignments.
 */
public record AddAssignmentsRequest(
        @NotNull(message = "Assignments list is required") List<AssignmentEntry> assignments) {
    public record AssignmentEntry(
            @NotBlank String evaluatorId,
            @NotBlank String evaluateeId,
            @NotNull EvaluatorRole evaluatorRole) {
    }
}
