package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.service.SettingsResolverService;
import com.evaluationservice.domain.event.CampaignClosedEvent;
import com.evaluationservice.domain.event.EvaluationSubmittedEvent;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRuleEngineServiceTest {

    @Mock
    private NotificationModuleService notificationModuleService;

    @Mock
    private SettingsResolverService settingsResolverService;

    @Test
    void skipsEvaluationSubmittedWhenFeatureDisabled() {
        when(settingsResolverService.resolveBoolean("features.enable-notification-rule-engine")).thenReturn(false);

        NotificationRuleEngineService service = new NotificationRuleEngineService(
                notificationModuleService,
                settingsResolverService);
        service.onEvaluationSubmitted(new EvaluationSubmittedEvent(
                EvaluationId.of("eval-1"),
                CampaignId.of("camp-1"),
                "evaluator-1",
                "evaluatee-1",
                Instant.now()));

        verify(notificationModuleService, never()).processEvent(any(), any(), any());
    }

    @Test
    void processesEvaluationSubmittedWhenFeatureEnabled() {
        when(settingsResolverService.resolveBoolean("features.enable-notification-rule-engine")).thenReturn(true);

        NotificationRuleEngineService service = new NotificationRuleEngineService(
                notificationModuleService,
                settingsResolverService);
        service.onEvaluationSubmitted(new EvaluationSubmittedEvent(
                EvaluationId.of("eval-1"),
                CampaignId.of("camp-1"),
                "evaluator-1",
                "evaluatee-1",
                Instant.now()));

        verify(notificationModuleService).processEvent(eq("EVALUATION_SUBMITTED"), eq("camp-1"), any());
    }

    @Test
    void processesCampaignClosedWhenFeatureEnabled() {
        when(settingsResolverService.resolveBoolean("features.enable-notification-rule-engine")).thenReturn(true);

        NotificationRuleEngineService service = new NotificationRuleEngineService(
                notificationModuleService,
                settingsResolverService);
        service.onCampaignClosed(new CampaignClosedEvent(
                CampaignId.of("camp-2"),
                90.0,
                100,
                90,
                Instant.now()));

        verify(notificationModuleService).processEvent(eq("CAMPAIGN_CLOSED"), eq("camp-2"), any());
    }

    @Test
    void skipsScheduledRunWhenFeatureDisabled() {
        when(settingsResolverService.resolveBoolean("features.enable-notification-rule-engine")).thenReturn(false);

        NotificationRuleEngineService service = new NotificationRuleEngineService(
                notificationModuleService,
                settingsResolverService);
        service.runScheduledRules();

        verify(notificationModuleService, never()).runScheduledRules(any());
    }
}
