package com.evaluationservice.domain.event;

import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when an evaluation is submitted.
 */
public record EvaluationSubmittedEvent(
        EvaluationId evaluationId,
        CampaignId campaignId,
        String evaluatorId,
        String evaluateeId,
        Instant occurredAt) {
    public EvaluationSubmittedEvent {
        Objects.requireNonNull(evaluationId);
        Objects.requireNonNull(campaignId);
        Objects.requireNonNull(evaluatorId);
        Objects.requireNonNull(evaluateeId);
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static EvaluationSubmittedEvent of(EvaluationId evalId, CampaignId campId, String evaluatorId,
            String evaluateeId) {
        return new EvaluationSubmittedEvent(evalId, campId, evaluatorId, evaluateeId, Instant.now());
    }
}
