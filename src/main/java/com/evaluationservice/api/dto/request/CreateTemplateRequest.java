package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.ScoringMethod;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating evaluation templates.
 */
public record CreateTemplateRequest(
                @NotBlank(message = "Template name is required") String name,
                String description,
                String category,
                ScoringMethod scoringMethod,
                java.util.List<SectionRequest> sections,
                String customFormula) {
}
