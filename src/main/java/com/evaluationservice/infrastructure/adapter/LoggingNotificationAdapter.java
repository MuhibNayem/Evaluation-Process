package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.NotificationPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default notification adapter that logs all notifications.
 * Active when no external notification channels are configured,
 * or as a fallback when
 * {@code evaluation.service.notification.webhook-enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "evaluation.service.notification.webhook-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void sendReminder(String recipientId, String campaignName, String message) {
        log.info("[NOTIFICATION] Reminder to '{}' for campaign '{}': {}",
                recipientId, campaignName, message);
    }

    @Override
    public void sendCompletionNotification(String recipientId, String campaignName) {
        log.info("[NOTIFICATION] Completion notification to '{}' for campaign '{}'",
                recipientId, campaignName);
    }

    @Override
    public void sendDeadlineExtensionNotification(String recipientId, String campaignName, String newDeadline) {
        log.info("[NOTIFICATION] Deadline extension to '{}' for campaign '{}': new deadline {}",
                recipientId, campaignName, newDeadline);
    }
}
