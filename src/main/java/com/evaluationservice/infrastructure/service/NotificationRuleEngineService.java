package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.service.SettingsResolverService;
import com.evaluationservice.domain.event.CampaignClosedEvent;
import com.evaluationservice.domain.event.EvaluationSubmittedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
public class NotificationRuleEngineService {

    private static final String FEATURE_FLAG = "features.enable-notification-rule-engine";

    private final NotificationModuleService notificationModuleService;
    private final SettingsResolverService settingsResolverService;

    public NotificationRuleEngineService(
            NotificationModuleService notificationModuleService,
            SettingsResolverService settingsResolverService) {
        this.notificationModuleService = Objects.requireNonNull(notificationModuleService);
        this.settingsResolverService = Objects.requireNonNull(settingsResolverService);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvaluationSubmitted(EvaluationSubmittedEvent event) {
        if (!isEnabled()) {
            return;
        }
        notificationModuleService.processEvent(
                "EVALUATION_SUBMITTED",
                event.campaignId().value(),
                Map.of(
                        "campaignId", event.campaignId().value(),
                        "evaluatorId", event.evaluatorId(),
                        "evaluateeId", event.evaluateeId(),
                        "message", "Evaluation submitted"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCampaignClosed(CampaignClosedEvent event) {
        if (!isEnabled()) {
            return;
        }
        notificationModuleService.processEvent(
                "CAMPAIGN_CLOSED",
                event.campaignId().value(),
                Map.of(
                        "campaignId", event.campaignId().value(),
                        "completionPercentage", event.completionPercentage(),
                        "message", "Campaign closed"));
    }

    @Scheduled(cron = "${evaluation.service.notification.scheduler-cron:0 * * * * *}")
    public void runScheduledRules() {
        if (!isEnabled()) {
            return;
        }
        notificationModuleService.runScheduledRules(Instant.now());
    }

    private boolean isEnabled() {
        return settingsResolverService.resolveBoolean(FEATURE_FLAG);
    }
}
