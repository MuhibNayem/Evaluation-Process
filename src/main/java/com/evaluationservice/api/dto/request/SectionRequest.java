package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record SectionRequest(
        String id,
        @NotBlank(message = "Section title is required") String title,
        String description,
        int orderIndex,
        BigDecimal weight,
        List<QuestionRequest> questions) {
}
