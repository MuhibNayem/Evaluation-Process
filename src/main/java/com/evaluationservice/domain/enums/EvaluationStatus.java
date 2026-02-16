package com.evaluationservice.domain.enums;

/**
 * Status of an individual evaluation submission.
 */
public enum EvaluationStatus {
    /** Evaluation has been started but not yet submitted */
    DRAFT,
    /** Evaluation has been submitted and is pending scoring */
    SUBMITTED,
    /** Evaluation is currently being scored */
    SCORING,
    /** Evaluation has been scored and completed */
    COMPLETED,
    /** Evaluation was flagged for review (anomaly detected) */
    FLAGGED,
    /** Evaluation was invalidated by admin */
    INVALIDATED;

    public boolean isTerminal() {
        return this == COMPLETED || this == INVALIDATED;
    }

    public boolean canSubmit() {
        return this == DRAFT;
    }
}
