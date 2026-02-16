package com.evaluationservice.domain.enums;

/**
 * Lifecycle status of an evaluation campaign.
 * Transitions: DRAFT → SCHEDULED → ACTIVE → CLOSED → ARCHIVED
 */
public enum CampaignStatus {
    /** Campaign is being configured, not yet visible to evaluators */
    DRAFT,
    /** Campaign is scheduled to open at a future date */
    SCHEDULED,
    /** Campaign is active and accepting evaluations */
    ACTIVE,
    /** Campaign is closed, no more submissions accepted */
    CLOSED,
    /** Campaign is archived for historical reference */
    ARCHIVED;

    public boolean canTransitionTo(CampaignStatus target) {
        return switch (this) {
            case DRAFT -> target == SCHEDULED || target == ACTIVE;
            case SCHEDULED -> target == ACTIVE || target == DRAFT;
            case ACTIVE -> target == CLOSED;
            case CLOSED -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
