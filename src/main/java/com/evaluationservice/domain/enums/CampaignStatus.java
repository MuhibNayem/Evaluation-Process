package com.evaluationservice.domain.enums;

/**
 * Lifecycle status of an evaluation campaign.
 * Legacy transitions: DRAFT → SCHEDULED → ACTIVE → CLOSED → ARCHIVED
 * Extended transitions also support:
 * DRAFT/SCHEDULED -> PUBLISHED_OPEN -> CLOSED -> RESULTS_PUBLISHED -> ARCHIVED
 * and CLOSED -> PUBLISHED_OPEN (reopen)
 */
public enum CampaignStatus {
    /** Campaign is being configured, not yet visible to evaluators */
    DRAFT,
    /** Campaign is scheduled to open at a future date */
    SCHEDULED,
    /** Campaign is active and accepting evaluations */
    ACTIVE,
    /** Campaign is published and open (PDF-aligned lifecycle mode) */
    PUBLISHED_OPEN,
    /** Campaign is closed, no more submissions accepted */
    CLOSED,
    /** Campaign results are published to downstream viewer roles */
    RESULTS_PUBLISHED,
    /** Campaign is archived for historical reference */
    ARCHIVED;

    public boolean canTransitionTo(CampaignStatus target) {
        return switch (this) {
            case DRAFT -> target == SCHEDULED || target == ACTIVE || target == PUBLISHED_OPEN;
            case SCHEDULED -> target == ACTIVE || target == PUBLISHED_OPEN || target == DRAFT;
            case ACTIVE -> target == CLOSED;
            case PUBLISHED_OPEN -> target == CLOSED;
            case CLOSED -> target == ARCHIVED || target == ACTIVE || target == PUBLISHED_OPEN || target == RESULTS_PUBLISHED;
            case RESULTS_PUBLISHED -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
