package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.NotificationPort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled component for campaign lifecycle automation.
 * Auto-activates campaigns at their start date and auto-closes at their end
 * date,
 * based on configuration properties.
 */
@Component
public class CampaignScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);

    private final CampaignPersistencePort campaignPersistencePort;
    private final NotificationPort notificationPort;
    private final EvaluationServiceProperties.Campaign campaignConfig;
    private final boolean sendReminders;
    private final int reminderDays;

    public CampaignScheduler(
            CampaignPersistencePort campaignPersistencePort,
            NotificationPort notificationPort,
            EvaluationServiceProperties properties) {
        this.campaignPersistencePort = campaignPersistencePort;
        this.notificationPort = notificationPort;
        this.campaignConfig = properties.getCampaign();
        this.sendReminders = campaignConfig.isSendDeadlineReminders();
        this.reminderDays = campaignConfig.getReminderDaysBeforeDeadline();
    }

    /**
     * Runs periodically based on configured cron expression.
     * Auto-activates SCHEDULED campaigns whose start date has arrived,
     * and auto-closes ACTIVE campaigns whose end date has passed.
     */
    @Scheduled(cron = "${evaluation.service.campaign.scheduler-cron:0 0 * * * *}")
    public void processCampaignLifecycle() {
        Instant now = Instant.now();

        if (campaignConfig.isAutoActivate()) {
            autoActivateScheduledCampaigns(now);
        }

        if (campaignConfig.isAutoClose()) {
            autoCloseExpiredCampaigns(now);
        }

        if (sendReminders) {
            sendDeadlineReminders(now);
        }
    }

    private void autoActivateScheduledCampaigns(Instant now) {
        List<Campaign> scheduled = campaignPersistencePort
                .findByStatus(CampaignStatus.SCHEDULED, 0, 1000);

        int activated = 0;
        for (Campaign campaign : scheduled) {
            if (!campaign.getDateRange().startDate().isAfter(now)) {
                try {
                    campaign.activate();
                    campaignPersistencePort.save(campaign);
                    activated++;
                    log.info("Auto-activated campaign: {} ({})", campaign.getName(), campaign.getId().value());
                } catch (Exception e) {
                    log.error("Failed to auto-activate campaign {}: {}", campaign.getId().value(), e.getMessage());
                }
            }
        }

        if (activated > 0) {
            log.info("Auto-activated {} campaigns", activated);
        }
    }

    private void autoCloseExpiredCampaigns(Instant now) {
        List<Campaign> active = campaignPersistencePort
                .findByStatus(CampaignStatus.ACTIVE, 0, 1000);

        int closed = 0;
        for (Campaign campaign : active) {
            if (campaign.getDateRange().endDate().isBefore(now)) {
                try {
                    campaign.close();
                    campaignPersistencePort.save(campaign);
                    closed++;
                    log.info("Auto-closed campaign: {} ({})", campaign.getName(), campaign.getId().value());
                } catch (Exception e) {
                    log.error("Failed to auto-close campaign {}: {}", campaign.getId().value(), e.getMessage());
                }
            }
        }

        if (closed > 0) {
            log.info("Auto-closed {} campaigns", closed);
        }
    }

    private void sendDeadlineReminders(Instant now) {
        List<Campaign> active = campaignPersistencePort
                .findByStatus(CampaignStatus.ACTIVE, 0, 1000);

        Instant reminderThreshold = now.plus(reminderDays, ChronoUnit.DAYS);

        for (Campaign campaign : active) {
            if (campaign.getDateRange().endDate().isBefore(reminderThreshold)
                    && campaign.getDateRange().endDate().isAfter(now)) {
                // Send reminders to incomplete assignment evaluators
                campaign.getAssignments().stream()
                        .filter(a -> !a.isCompleted())
                        .forEach(a -> {
                            try {
                                notificationPort.sendReminder(
                                        a.getEvaluatorId(),
                                        campaign.getName(),
                                        "Your evaluation is due in " + reminderDays + " days");
                            } catch (Exception e) {
                                log.warn("Failed to send reminder for assignment {}: {}",
                                        a.getId(), e.getMessage());
                            }
                        });
            }
        }
    }
}
