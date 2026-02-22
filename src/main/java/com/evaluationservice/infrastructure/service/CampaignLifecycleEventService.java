package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.CampaignLifecycleEventResponse;
import com.evaluationservice.infrastructure.entity.CampaignLifecycleEventEntity;
import com.evaluationservice.infrastructure.repository.CampaignLifecycleEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CampaignLifecycleEventService {

    private final CampaignLifecycleEventRepository repository;
    private final ObjectMapper objectMapper;

    public CampaignLifecycleEventService(CampaignLifecycleEventRepository repository, ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public void logTransition(
            String campaignId,
            String fromStatus,
            String toStatus,
            String action,
            String actor,
            String reason,
            Map<String, Object> metadata) {
        CampaignLifecycleEventEntity entity = new CampaignLifecycleEventEntity();
        entity.setCampaignId(campaignId);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setAction(action);
        entity.setActor(actor == null || actor.isBlank() ? "system" : actor);
        entity.setReason(reason);
        entity.setMetadataJson(toJson(metadata));
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<CampaignLifecycleEventResponse> listRecent(String campaignId) {
        return repository.findTop100ByCampaignIdOrderByCreatedAtDesc(campaignId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CampaignLifecycleEventResponse toResponse(CampaignLifecycleEventEntity entity) {
        return new CampaignLifecycleEventResponse(
                entity.getId(),
                entity.getCampaignId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getAction(),
                entity.getActor(),
                entity.getReason(),
                fromJson(entity.getMetadataJson()),
                entity.getCreatedAt());
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize lifecycle metadata", ex);
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of("raw", value);
        }
    }
}

