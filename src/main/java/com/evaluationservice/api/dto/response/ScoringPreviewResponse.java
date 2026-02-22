package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.ScoringMethod;

import java.math.BigDecimal;
import java.util.List;

public record ScoringPreviewResponse(
        String templateId,
        ScoringMethod scoringMethod,
        BigDecimal totalScore,
        List<SectionPreview> sections) {

    public record SectionPreview(
            String sectionId,
            String sectionTitle,
            BigDecimal score,
            BigDecimal maxPossible,
            int answeredQuestions,
            int totalQuestions) {
    }
}
