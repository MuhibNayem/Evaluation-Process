package com.evaluationservice.domain.enums;

/**
 * Status of a template version.
 */
public enum TemplateStatus {
    /** Template is being edited */
    DRAFT,
    /** Template is published and available for campaigns */
    PUBLISHED,
    /**
     * Template is deprecated, not available for new campaigns but existing
     * campaigns continue
     */
    DEPRECATED,
    /** Template is archived */
    ARCHIVED;

    public boolean isUsableForNewCampaigns() {
        return this == PUBLISHED;
    }
}
