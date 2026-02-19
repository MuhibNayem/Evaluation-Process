package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.entity.AdminActionAuditLogEntity;
import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.evaluationservice.infrastructure.repository.AdminActionAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminAuditLogService {

    private final AdminActionAuditLogRepository auditLogRepository;
    private final IntegrationOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditLogService(
            AdminActionAuditLogRepository auditLogRepository,
            IntegrationOutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository);
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void log(
            String tenantId,
            String actor,
            String action,
            String aggregateType,
            String aggregateId,
            String reasonCode,
            String comment,
            Map<String, Object> payload) {
        AdminActionAuditLogEntity entity = new AdminActionAuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setActor(actor);
        entity.setAction(action);
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setReasonCode(reasonCode);
        entity.setComment(comment);
        entity.setPayloadJson(toJson(payload));
        entity.setCreatedAt(Instant.now());
        AdminActionAuditLogEntity saved = auditLogRepository.save(entity);

        IntegrationOutboxEventEntity outbox = new IntegrationOutboxEventEntity();
        outbox.setAggregateType("ADMIN_ACTION_AUDIT");
        outbox.setAggregateId(String.valueOf(saved.getId()));
        outbox.setEventType("ADMIN_ACTION_" + action);
        Map<String, Object> outboxPayload = new LinkedHashMap<>();
        outboxPayload.put("id", saved.getId());
        outboxPayload.put("tenantId", tenantId);
        outboxPayload.put("actor", actor);
        outboxPayload.put("action", action);
        outboxPayload.put("aggregateType", aggregateType);
        outboxPayload.put("aggregateId", aggregateId);
        outboxPayload.put("reasonCode", reasonCode);
        outboxPayload.put("comment", comment);
        outboxPayload.put("payload", payload == null ? Map.of() : payload);
        outbox.setPayloadJson(toJson(outboxPayload));
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outbox.setCreatedAt(Instant.now());
        outboxEventRepository.save(outbox);
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize admin audit payload", ex);
        }
    }
}
