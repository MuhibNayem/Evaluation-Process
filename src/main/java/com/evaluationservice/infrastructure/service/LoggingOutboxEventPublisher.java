package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

    private final EvaluationServiceProperties.Outbox outboxConfig;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public LoggingOutboxEventPublisher(
            EvaluationServiceProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.outboxConfig = Objects.requireNonNull(properties).getAudience().getOutbox();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
    }

    LoggingOutboxEventPublisher(
            EvaluationServiceProperties properties,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            RabbitTemplate rabbitTemplate) {
        this.outboxConfig = Objects.requireNonNull(properties).getAudience().getOutbox();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(OutboxEvent event) throws Exception {
        String transport = normalizeTransport(outboxConfig.getTransport());
        switch (transport) {
            case "WEBHOOK" -> {
                if (!outboxConfig.isWebhookEnabled()) {
                    throw new IllegalStateException("Outbox transport WEBHOOK selected but webhook is disabled");
                }
                publishToWebhook(event);
            }
            case "KAFKA" -> publishToKafka(event);
            case "RABBITMQ" -> publishToRabbit(event);
            case "LOG" -> publishToLog(event);
            default -> throw new IllegalStateException("Unsupported outbox transport: " + transport);
        }
    }

    private void publishToLog(OutboxEvent event) {
        log.info("Outbox publish log transport: id={}, aggregateType={}, aggregateId={}, eventType={}",
                event.id(), event.aggregateType(), event.aggregateId(), event.eventType());
    }

    private void publishToKafka(OutboxEvent event) throws Exception {
        if (!outboxConfig.getKafka().isEnabled()) {
            throw new IllegalStateException("Outbox transport KAFKA selected but kafka is disabled");
        }
        if (kafkaTemplate == null) {
            throw new IllegalStateException("KafkaTemplate bean is not available for outbox publisher");
        }
        String topic = outboxConfig.getKafka().getTopic();
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Outbox kafka topic is not configured");
        }
        String payload = toEventPayload(event);
        kafkaTemplate
                .send(topic, event.aggregateId(), payload)
                .get(Math.max(1, outboxConfig.getKafka().getSendTimeoutMs()), TimeUnit.MILLISECONDS);
    }

    private void publishToRabbit(OutboxEvent event) throws Exception {
        if (!outboxConfig.getRabbitmq().isEnabled()) {
            throw new IllegalStateException("Outbox transport RABBITMQ selected but rabbitmq is disabled");
        }
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitTemplate bean is not available for outbox publisher");
        }
        String exchange = outboxConfig.getRabbitmq().getExchange();
        String routingKey = outboxConfig.getRabbitmq().getRoutingKey();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("Outbox rabbitmq exchange is not configured");
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalStateException("Outbox rabbitmq routingKey is not configured");
        }
        String payload = toEventPayload(event);
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }

    private void publishToWebhook(OutboxEvent event) throws Exception {
        if (outboxConfig.getWebhookUrl() == null || outboxConfig.getWebhookUrl().isBlank()) {
            throw new IllegalStateException("Outbox webhookUrl is not configured");
        }
        String payload = toEventPayload(event);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, outboxConfig.getWebhookTimeoutMs())))
                .build();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(outboxConfig.getWebhookUrl()))
                .timeout(Duration.ofMillis(Math.max(500, outboxConfig.getWebhookTimeoutMs())))
                .header("Content-Type", "application/json");

        if (outboxConfig.getWebhookAuthToken() != null && !outboxConfig.getWebhookAuthToken().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + outboxConfig.getWebhookAuthToken().trim());
        }
        if (outboxConfig.getWebhookHeaders() != null) {
            for (Map.Entry<String, String> header : outboxConfig.getWebhookHeaders().entrySet()) {
                if (header.getKey() != null && !header.getKey().isBlank() && header.getValue() != null) {
                    requestBuilder.header(header.getKey(), header.getValue());
                }
            }
        }

        HttpResponse<String> response = client.send(
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload)).build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Webhook publish failed with status " + response.statusCode());
        }
    }

    private String toEventPayload(OutboxEvent event) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "id", event.id(),
                "aggregateType", event.aggregateType(),
                "aggregateId", event.aggregateId(),
                "eventType", event.eventType(),
                "payload", event.payload()));
    }

    private String normalizeTransport(String transport) {
        if (transport == null || transport.isBlank()) {
            return "LOG";
        }
        return transport.trim().toUpperCase();
    }
}
