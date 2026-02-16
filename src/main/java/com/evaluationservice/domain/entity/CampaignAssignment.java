package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.value.CampaignId;

import java.util.Objects;

/**
 * Represents an evaluator↔evaluatee assignment within a campaign.
 * Tracks whether the assignment has been completed.
 */
public class CampaignAssignment {

    private final String id;
    private final CampaignId campaignId;
    private final String evaluatorId;
    private final String evaluateeId;
    private final EvaluatorRole evaluatorRole;
    private boolean completed;
    private String evaluationId;

    public CampaignAssignment(
            String id,
            CampaignId campaignId,
            String evaluatorId,
            String evaluateeId,
            EvaluatorRole evaluatorRole,
            boolean completed,
            String evaluationId) {
        this.id = Objects.requireNonNull(id, "Assignment ID cannot be null");
        this.campaignId = Objects.requireNonNull(campaignId, "Campaign ID cannot be null");
        this.evaluatorId = Objects.requireNonNull(evaluatorId, "Evaluator ID cannot be null");
        this.evaluateeId = Objects.requireNonNull(evaluateeId, "Evaluatee ID cannot be null");
        this.evaluatorRole = Objects.requireNonNull(evaluatorRole, "Evaluator role cannot be null");
        this.completed = completed;
        this.evaluationId = evaluationId;
    }

    public void markCompleted(String evaluationId) {
        this.completed = true;
        this.evaluationId = Objects.requireNonNull(evaluationId);
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public CampaignId getCampaignId() {
        return campaignId;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public String getEvaluateeId() {
        return evaluateeId;
    }

    public EvaluatorRole getEvaluatorRole() {
        return evaluatorRole;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getEvaluationId() {
        return evaluationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CampaignAssignment that = (CampaignAssignment) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
