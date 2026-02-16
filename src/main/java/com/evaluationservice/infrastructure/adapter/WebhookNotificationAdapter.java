package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.NotificationPort;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import com.evaluationservice.infrastructure.client.WebhookClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Webhook-based notification adapter.
 * Sends notification payloads to a configurable external URL.
 * Active when {@code evaluation.service.notification.webhook-enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "evaluation.service.notification.webhook-enabled", havingValue = "true")
public class WebhookNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationAdapter.class);

    private final WebhookClient webhookClient;
    private final EvaluationServiceProperties.Notification config;

    public WebhookNotificationAdapter(WebhookClient webhookClient, EvaluationServiceProperties properties) {
        this.webhookClient = webhookClient;
        this.config = properties.getNotification();
    }

    @Async
    @Override
    public void sendReminder(String recipientId, String campaignName, String message) {
        postWebhook(Map.of(
                "type", "REMINDER",
                "recipientId", recipientId,
                "campaignName", campaignName,
                "message", message));
    }

    @Async
    @Override
    public void sendCompletionNotification(String recipientId, String campaignName) {
        postWebhook(Map.of(
                "type", "COMPLETION",
                "recipientId", recipientId,
                "campaignName", campaignName));
    }

    @Async
    @Override
    public void sendDeadlineExtensionNotification(String recipientId, String campaignName, String newDeadline) {
        postWebhook(Map.of(
                "type", "DEADLINE_EXTENSION",
                "recipientId", recipientId,
                "campaignName", campaignName,
                "newDeadline", newDeadline));
    }

    private void postWebhook(Map<String, String> payload) {
        try {
            webhookClient.postWebhook(java.net.URI.create(config.getWebhookUrl()), payload);
            log.debug("Webhook notification sent: {}", payload.get("type"));
        } catch (Exception e) {
            log.error("Failed to send webhook notification to {}: {}", config.getWebhookUrl(), e.getMessage());
        }
    }
}
