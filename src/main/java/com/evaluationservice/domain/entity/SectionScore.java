package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.value.Score;

import java.util.Objects;

/**
 * Score for a single section within an evaluation.
 */
public record SectionScore(
        String sectionId,
        String sectionTitle,
        Score score,
        Score maxPossibleScore,
        int answeredQuestions,
        int totalQuestions) {
    public SectionScore {
        Objects.requireNonNull(sectionId, "Section ID cannot be null");
        Objects.requireNonNull(sectionTitle, "Section title cannot be null");
        Objects.requireNonNull(score, "Score cannot be null");
        Objects.requireNonNull(maxPossibleScore, "Max possible score cannot be null");
    }

    public double percentage() {
        return score.toPercentage(maxPossibleScore);
    }
}
