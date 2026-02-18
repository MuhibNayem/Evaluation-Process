package com.evaluationservice.infrastructure.service;

import com.evaluationservice.domain.event.AudienceMappingProfileLifecycleEvent;
import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class MappingProfileLifecycleOutboxService {

    private static final String AGGREGATE_TYPE = "AUDIENCE_MAPPING_PROFILE";

    private final IntegrationOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public MappingProfileLifecycleOutboxService(
            IntegrationOutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @EventListener
    public void onLifecycleEvent(AudienceMappingProfileLifecycleEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("profileId", event.profileId());
        payload.put("tenantId", event.tenantId());
        payload.put("eventType", event.eventType());
        payload.put("actor", event.actor());
        payload.put("occurredAt", event.occurredAt() == null ? null : event.occurredAt().toString());
        payload.put("payload", event.payload());

        IntegrationOutboxEventEntity outbox = new IntegrationOutboxEventEntity();
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(String.valueOf(event.profileId()));
        outbox.setEventType("AUDIENCE_MAPPING_PROFILE_" + event.eventType());
        outbox.setPayloadJson(toJson(payload));
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize outbox payload", ex);
        }
    }
}
