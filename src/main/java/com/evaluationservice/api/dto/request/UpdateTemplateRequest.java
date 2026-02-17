package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.ScoringMethod;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateTemplateRequest(
        @NotBlank(message = "Template name is required") String name,
        String description,
        String category,
        ScoringMethod scoringMethod,
        List<SectionRequest> sections,
        String customFormula) {
}
