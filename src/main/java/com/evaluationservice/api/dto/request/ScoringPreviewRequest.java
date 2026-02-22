package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.ScoringMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ScoringPreviewRequest(
        @NotBlank String templateId,
        ScoringMethod scoringMethodOverride,
        String customFormulaOverride,
        @NotNull @Valid List<AnswerRequest> answers) {
}
