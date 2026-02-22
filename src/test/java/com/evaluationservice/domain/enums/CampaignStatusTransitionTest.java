package com.evaluationservice.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignStatusTransitionTest {

    @Test
    void supportsPdfLifecycleTransitions() {
        assertTrue(CampaignStatus.DRAFT.canTransitionTo(CampaignStatus.PUBLISHED_OPEN));
        assertTrue(CampaignStatus.SCHEDULED.canTransitionTo(CampaignStatus.PUBLISHED_OPEN));
        assertTrue(CampaignStatus.PUBLISHED_OPEN.canTransitionTo(CampaignStatus.CLOSED));
        assertTrue(CampaignStatus.CLOSED.canTransitionTo(CampaignStatus.PUBLISHED_OPEN));
        assertTrue(CampaignStatus.CLOSED.canTransitionTo(CampaignStatus.RESULTS_PUBLISHED));
        assertTrue(CampaignStatus.RESULTS_PUBLISHED.canTransitionTo(CampaignStatus.ARCHIVED));
    }

    @Test
    void stillBlocksInvalidTransitions() {
        assertFalse(CampaignStatus.ARCHIVED.canTransitionTo(CampaignStatus.DRAFT));
        assertFalse(CampaignStatus.RESULTS_PUBLISHED.canTransitionTo(CampaignStatus.PUBLISHED_OPEN));
        assertFalse(CampaignStatus.ACTIVE.canTransitionTo(CampaignStatus.PUBLISHED_OPEN));
    }
}

