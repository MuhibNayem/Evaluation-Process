package com.evaluationservice.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.amqp.core.TopicExchange;

@Configuration
public class MessagingProvisioningConfig {

    private final EvaluationServiceProperties properties;

    public MessagingProvisioningConfig(EvaluationServiceProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "evaluation.service.audience.outbox.rabbitmq",
            name = "enabled",
            havingValue = "true")
    TopicExchange outboxTopicExchange() {
        String exchange = properties.getAudience().getOutbox().getRabbitmq().getExchange();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("Outbox rabbitmq exchange is not configured");
        }
        return new TopicExchange(exchange.trim(), true, false);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "evaluation.service.audience.outbox.kafka",
            name = "enabled",
            havingValue = "true")
    KafkaAdmin.NewTopics outboxKafkaTopic() {
        String topic = properties.getAudience().getOutbox().getKafka().getTopic();
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Outbox kafka topic is not configured");
        }
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(topic.trim()).partitions(1).replicas(1).build());
    }
}
