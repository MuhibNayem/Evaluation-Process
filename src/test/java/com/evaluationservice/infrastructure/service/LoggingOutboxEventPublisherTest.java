package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LoggingOutboxEventPublisher")
class LoggingOutboxEventPublisherTest {

    @Test
    @DisplayName("publishes by logging when webhook is disabled")
    void publishesByLoggingWhenWebhookDisabled() {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getOutbox().setTransport("LOG");
        LoggingOutboxEventPublisher publisher = new LoggingOutboxEventPublisher(
                props,
                new ObjectMapper(),
                (KafkaTemplate<String, String>) null,
                (RabbitTemplate) null);

        assertThatCode(() -> publisher.publish(new OutboxEventPublisher.OutboxEvent(
                1L,
                "AUDIENCE_MAPPING_PROFILE",
                "42",
                "UPDATED",
                Map.of("x", 1))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publishes to webhook when enabled")
    void publishesToWebhookWhenEnabled() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/outbox", exchange -> respond(exchange, 202, "{\"ok\":true}"));
        server.start();
        try {
            EvaluationServiceProperties props = new EvaluationServiceProperties();
            props.getAudience().getOutbox().setTransport("WEBHOOK");
            props.getAudience().getOutbox().setWebhookEnabled(true);
            props.getAudience().getOutbox().setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/outbox");
            props.getAudience().getOutbox().setWebhookTimeoutMs(2000);

            LoggingOutboxEventPublisher publisher = new LoggingOutboxEventPublisher(
                    props,
                    new ObjectMapper(),
                    (KafkaTemplate<String, String>) null,
                    (RabbitTemplate) null);
            assertThatCode(() -> publisher.publish(new OutboxEventPublisher.OutboxEvent(
                    2L,
                    "AUDIENCE_MAPPING_PROFILE",
                    "43",
                    "DEACTIVATED",
                    Map.of("active", false))))
                    .doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("throws when webhook returns non success status")
    void throwsWhenWebhookNonSuccess() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/outbox", exchange -> respond(exchange, 500, "{\"error\":true}"));
        server.start();
        try {
            EvaluationServiceProperties props = new EvaluationServiceProperties();
            props.getAudience().getOutbox().setTransport("WEBHOOK");
            props.getAudience().getOutbox().setWebhookEnabled(true);
            props.getAudience().getOutbox().setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/outbox");
            props.getAudience().getOutbox().setWebhookTimeoutMs(2000);

            LoggingOutboxEventPublisher publisher = new LoggingOutboxEventPublisher(
                    props,
                    new ObjectMapper(),
                    (KafkaTemplate<String, String>) null,
                    (RabbitTemplate) null);
            assertThatThrownBy(() -> publisher.publish(new OutboxEventPublisher.OutboxEvent(
                    3L,
                    "AUDIENCE_MAPPING_PROFILE",
                    "44",
                    "CREATED",
                    Map.of("a", "b"))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Webhook publish failed");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("publishes to kafka when configured")
    void publishesToKafkaWhenConfigured() throws Exception {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getOutbox().setTransport("KAFKA");
        props.getAudience().getOutbox().getKafka().setEnabled(true);
        props.getAudience().getOutbox().getKafka().setTopic("outbox.topic");
        props.getAudience().getOutbox().getKafka().setSendTimeoutMs(2000);

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq("outbox.topic"), eq("42"), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        LoggingOutboxEventPublisher publisher = new LoggingOutboxEventPublisher(
                props, new ObjectMapper(), kafkaTemplate, null);

        assertThatCode(() -> publisher.publish(new OutboxEventPublisher.OutboxEvent(
                4L,
                "AUDIENCE_MAPPING_PROFILE",
                "42",
                "UPDATED",
                Map.of("k", "v")))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publishes to rabbitmq when configured")
    void publishesToRabbitmqWhenConfigured() throws Exception {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getOutbox().setTransport("RABBITMQ");
        props.getAudience().getOutbox().getRabbitmq().setEnabled(true);
        props.getAudience().getOutbox().getRabbitmq().setExchange("outbox.exchange");
        props.getAudience().getOutbox().getRabbitmq().setRoutingKey("outbox.key");

        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        LoggingOutboxEventPublisher publisher = new LoggingOutboxEventPublisher(
                props, new ObjectMapper(), null, rabbitTemplate);

        assertThatCode(() -> publisher.publish(new OutboxEventPublisher.OutboxEvent(
                5L,
                "AUDIENCE_MAPPING_PROFILE",
                "43",
                "CREATED",
                Map.of("x", 1)))).doesNotThrowAnyException();

        verify(rabbitTemplate).convertAndSend(eq("outbox.exchange"), eq("outbox.key"), any(String.class));
    }

    private static void respond(HttpExchange exchange, int statusCode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}
