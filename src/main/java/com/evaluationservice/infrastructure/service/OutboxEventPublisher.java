package com.evaluationservice.infrastructure.service;

import java.util.Map;

public interface OutboxEventPublisher {

    void publish(OutboxEvent event) throws Exception;

    record OutboxEvent(
            long id,
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, Object> payload) {
    }
}
