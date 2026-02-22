package com.evaluationservice.api.dto.request;

public record UpdateAssignmentRequest(
        String stepType,
        String sectionId,
        String facultyId,
        String anonymityMode,
        String status) {
}
