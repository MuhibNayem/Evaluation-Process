package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.NotificationPort;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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

    private final RestTemplate restTemplate;
    private final EvaluationServiceProperties.Notification config;

    public WebhookNotificationAdapter(EvaluationServiceProperties properties) {
        this.config = properties.getNotification();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendReminder(String recipientId, String campaignName, String message) {
        postWebhook(Map.of(
                "type", "REMINDER",
                "recipientId", recipientId,
                "campaignName", campaignName,
                "message", message));
    }

    @Override
    public void sendCompletionNotification(String recipientId, String campaignName) {
        postWebhook(Map.of(
                "type", "COMPLETION",
                "recipientId", recipientId,
                "campaignName", campaignName));
    }

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
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(config.getWebhookUrl(), request, Void.class);
            log.debug("Webhook notification sent: {}", payload.get("type"));
        } catch (Exception e) {
            log.error("Failed to send webhook notification to {}: {}", config.getWebhookUrl(), e.getMessage());
        }
    }
}
