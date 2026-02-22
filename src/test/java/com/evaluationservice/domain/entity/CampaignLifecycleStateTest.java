package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignLifecycleStateTest {

    @Test
    void publishOpenSetsPublishedTimestampAndLock() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        campaign.publishOpen();

        assertNotNull(campaign.getPublishedAt());
        assertTrue(campaign.isLocked());
    }

    @Test
    void reopenSetsReopenedTimestamp() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        campaign.publishOpen();
        campaign.close();
        campaign.reopen();

        assertNotNull(campaign.getReopenedAt());
        assertTrue(campaign.isLocked());
    }

    @Test
    void publishResultsSetsResultsTimestamp() {
        Campaign campaign = campaign(CampaignStatus.DRAFT);
        campaign.publishOpen();
        campaign.close();
        campaign.publishResults();

        assertNotNull(campaign.getResultsPublishedAt());
        assertTrue(campaign.isLocked());
    }

    private Campaign campaign(CampaignStatus status) {
        Instant now = Instant.now();
        return new Campaign(
                CampaignId.generate(),
                "C",
                "D",
                TemplateId.of("00000000-0000-0000-0000-000000000001"),
                1,
                status,
                DateRange.of(now.minus(1, ChronoUnit.DAYS), now.plus(7, ChronoUnit.DAYS)),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                null,
                1,
                "INLINE",
                Map.of(),
                "ALL_TO_ALL",
                Map.of(),
                null,
                "tester",
                Timestamp.now(),
                Timestamp.now());
    }
}
