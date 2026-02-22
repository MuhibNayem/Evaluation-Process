package com.evaluationservice.application.service;

import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.service.CampaignLifecycleEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignManagementServiceTest {

    @Mock
    private CampaignPersistencePort campaignPersistencePort;
    @Mock
    private TemplatePersistencePort templatePersistencePort;
    @Mock
    private AssignmentPersistencePort assignmentPersistencePort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DynamicAssignmentEngine dynamicAssignmentEngine;
    @Mock
    private CampaignLifecycleEventService campaignLifecycleEventService;

    private CampaignManagementService service;

    @BeforeEach
    void setUp() {
        service = new CampaignManagementService(
                campaignPersistencePort,
                templatePersistencePort,
                assignmentPersistencePort,
                eventPublisher,
                dynamicAssignmentEngine,
                campaignLifecycleEventService);
    }

    @Test
    void publishCampaignLogsActorAndReason() {
        Campaign campaign = draftCampaign();
        when(campaignPersistencePort.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(campaignPersistencePort.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.publishCampaign(campaign.getId(), "admin-user", "go-live");

        verify(campaignLifecycleEventService).logTransition(
                eq(campaign.getId().value()),
                eq("DRAFT"),
                eq("PUBLISHED_OPEN"),
                eq("PUBLISH"),
                eq("admin-user"),
                eq("go-live"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any());
    }

    private Campaign draftCampaign() {
        Instant now = Instant.now();
        return new Campaign(
                CampaignId.generate(),
                "Campaign",
                "Desc",
                TemplateId.of("template-1"),
                1,
                CampaignStatus.DRAFT,
                DateRange.of(now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS)),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                null,
                1,
                "INLINE",
                Map.of(),
                "ALL_TO_ALL",
                Map.of(),
                null,
                "creator",
                Timestamp.now(),
                Timestamp.now());
    }
}
