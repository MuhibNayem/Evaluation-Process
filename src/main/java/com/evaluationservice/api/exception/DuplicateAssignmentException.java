package com.evaluationservice.api.exception;

public class DuplicateAssignmentException extends RuntimeException {

    private final String campaignId;
    private final String evaluatorId;
    private final String evaluateeId;
    private final String evaluatorRole;
    private final String existingAssignmentId;

    public DuplicateAssignmentException(
            String campaignId,
            String evaluatorId,
            String evaluateeId,
            String evaluatorRole,
            String existingAssignmentId) {
        super("Duplicate assignment tuple for campaign/evaluator/evaluatee/role");
        this.campaignId = campaignId;
        this.evaluatorId = evaluatorId;
        this.evaluateeId = evaluateeId;
        this.evaluatorRole = evaluatorRole;
        this.existingAssignmentId = existingAssignmentId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public String getEvaluateeId() {
        return evaluateeId;
    }

    public String getEvaluatorRole() {
        return evaluatorRole;
    }

    public String getExistingAssignmentId() {
        return existingAssignmentId;
    }
}
