package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;

import java.time.Instant;

/**
 * Response DTO for template data.
 */
public record TemplateResponse(
        String id,
        String name,
        String description,
        String category,
        TemplateStatus status,
        int currentVersion,
        ScoringMethod scoringMethod,
        int totalQuestions,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
