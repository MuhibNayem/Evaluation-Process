package com.evaluationservice.domain.event;

import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.domain.value.Score;
import com.evaluationservice.domain.value.TemplateId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when scoring is computed for an evaluation.
 */
public record ScoreComputedEvent(
        EvaluationId evaluationId,
        TemplateId templateId,
        Score totalScore,
        Instant occurredAt) {
    public ScoreComputedEvent {
        Objects.requireNonNull(evaluationId);
        Objects.requireNonNull(templateId);
        Objects.requireNonNull(totalScore);
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }
}
