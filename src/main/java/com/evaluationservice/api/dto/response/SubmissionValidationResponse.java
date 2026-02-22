package com.evaluationservice.api.dto.response;

import java.util.List;

public record SubmissionValidationResponse(
        boolean valid,
        int issueCount,
        List<ValidationIssue> issues) {

    public record ValidationIssue(
            String questionId,
            String code,
            String message) {
    }
}
