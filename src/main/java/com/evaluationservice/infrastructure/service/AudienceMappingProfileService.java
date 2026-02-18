package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AudienceMappingProfileResponse;
import com.evaluationservice.api.dto.response.AudienceMappingProfileEventResponse;
import com.evaluationservice.domain.event.AudienceMappingProfileLifecycleEvent;
import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEventEntity;
import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEntity;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileEventRepository;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AudienceMappingProfileService {

    private static final Set<String> ALLOWED_CANONICAL_FIELDS =
            Set.of(
                    "person_id",
                    "external_ref",
                    "display_name",
                    "email",
                    "active",
                    "group_id",
                    "group_type",
                    "name",
                    "membership_role",
                    "valid_from",
                    "valid_to");

    private final AudienceMappingProfileRepository mappingProfileRepository;
    private final AudienceMappingProfileEventRepository mappingProfileEventRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AudienceMappingProfileService(
            AudienceMappingProfileRepository mappingProfileRepository,
            AudienceMappingProfileEventRepository mappingProfileEventRepository,
            TenantRepository tenantRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.mappingProfileRepository = Objects.requireNonNull(mappingProfileRepository);
        this.mappingProfileEventRepository = Objects.requireNonNull(mappingProfileEventRepository);
        this.tenantRepository = Objects.requireNonNull(tenantRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public AudienceMappingProfileResponse create(
            String tenantId,
            String name,
            String sourceType,
            Map<String, String> fieldMappings,
            boolean active) {
        return create(tenantId, name, sourceType, fieldMappings, active, "system");
    }

    @Transactional
    public AudienceMappingProfileResponse create(
            String tenantId,
            String name,
            String sourceType,
            Map<String, String> fieldMappings,
            boolean active,
            String actor) {
        requireTenant(tenantId);
        Map<String, String> normalizedMappings = normalizeAndValidate(sourceType, fieldMappings);

        AudienceMappingProfileEntity entity = new AudienceMappingProfileEntity();
        entity.setTenantId(tenantId.trim());
        entity.setName(requireText(name, "name is required"));
        entity.setSourceType(normalizeSourceType(sourceType));
        entity.setMappingsJson(toJson(normalizedMappings));
        entity.setActive(active);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        AudienceMappingProfileEntity saved = mappingProfileRepository.save(entity);
        emitLifecycleEvent(saved, "CREATED", actor, Map.of(
                "name", saved.getName(),
                "sourceType", saved.getSourceType(),
                "active", saved.isActive(),
                "fieldMappings", normalizedMappings));
        return toResponse(saved);
    }

    @Transactional
    public AudienceMappingProfileResponse update(
            String tenantId,
            long profileId,
            String name,
            Map<String, String> fieldMappings,
            boolean active,
            String actor) {
        requireTenant(tenantId);
        AudienceMappingProfileEntity entity = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping profile not found: " + profileId));
        ensureTenantMatch(entity, tenantId);

        Map<String, String> normalizedMappings = normalizeAndValidate(entity.getSourceType(), fieldMappings);
        entity.setName(requireText(name, "name is required"));
        entity.setMappingsJson(toJson(normalizedMappings));
        entity.setActive(active);
        entity.setUpdatedAt(Instant.now());

        AudienceMappingProfileEntity saved = mappingProfileRepository.save(entity);
        emitLifecycleEvent(saved, "UPDATED", actor, Map.of(
                "name", saved.getName(),
                "active", saved.isActive(),
                "fieldMappings", normalizedMappings));
        return toResponse(saved);
    }

    @Transactional
    public AudienceMappingProfileResponse deactivate(
            String tenantId,
            long profileId,
            String actor) {
        requireTenant(tenantId);
        AudienceMappingProfileEntity entity = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping profile not found: " + profileId));
        ensureTenantMatch(entity, tenantId);
        if (!entity.isActive()) {
            return toResponse(entity);
        }
        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        AudienceMappingProfileEntity saved = mappingProfileRepository.save(entity);
        emitLifecycleEvent(saved, "DEACTIVATED", actor, Map.of(
                "active", false));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AudienceMappingProfileResponse> listByTenant(String tenantId) {
        requireTenant(tenantId);
        return mappingProfileRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AudienceMappingProfileResponse getById(String tenantId, long profileId) {
        requireTenant(tenantId);
        AudienceMappingProfileEntity entity = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping profile not found: " + profileId));
        ensureTenantMatch(entity, tenantId);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AudienceMappingProfileEventResponse> listEvents(String tenantId, long profileId, int limit) {
        requireTenant(tenantId);
        int size = Math.max(1, Math.min(limit, 200));
        return mappingProfileEventRepository
                .findByTenantIdAndProfileIdOrderByCreatedAtDesc(tenantId.trim(), profileId, PageRequest.of(0, size))
                .map(event -> new AudienceMappingProfileEventResponse(
                        event.getId(),
                        event.getProfileId(),
                        event.getTenantId(),
                        event.getEventType(),
                        event.getActor(),
                        fromPayloadJson(event.getEventPayloadJson()),
                        event.getCreatedAt()))
                .getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, String> resolveActiveMappings(String tenantId, Long profileId, String sourceType) {
        if (profileId == null) {
            return Map.of();
        }
        requireTenant(tenantId);
        String normalizedSourceType = normalizeSourceType(sourceType);
        AudienceMappingProfileEntity entity = mappingProfileRepository
                .findByIdAndTenantIdAndActiveTrue(profileId, tenantId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Active mapping profile not found: " + profileId));
        if (!normalizedSourceType.equals(entity.getSourceType())) {
            throw new IllegalArgumentException("Mapping profile sourceType does not match request sourceType");
        }
        return fromJson(entity.getMappingsJson());
    }

    public Map<String, String> normalizeAndValidate(String sourceType, Map<String, String> fieldMappings) {
        normalizeSourceType(sourceType);
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            throw new IllegalArgumentException("fieldMappings is required");
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String canonicalField = normalizeField(entry.getKey(), "Canonical field key cannot be blank");
            String sourceField = normalizeField(entry.getValue(), "Source field name cannot be blank");

            if (!ALLOWED_CANONICAL_FIELDS.contains(canonicalField)) {
                throw new IllegalArgumentException("Unsupported canonical field: " + canonicalField);
            }
            if (normalized.containsKey(canonicalField)) {
                throw new IllegalArgumentException("Duplicate canonical field mapping: " + canonicalField);
            }
            if (normalized.containsValue(sourceField)) {
                throw new IllegalArgumentException("Duplicate source field mapping: " + sourceField);
            }
            normalized.put(canonicalField, sourceField);
        }

        if (!normalized.containsKey("person_id")) {
            throw new IllegalArgumentException("fieldMappings must include person_id");
        }
        return normalized;
    }

    private AudienceMappingProfileResponse toResponse(AudienceMappingProfileEntity entity) {
        return new AudienceMappingProfileResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getSourceType(),
                fromJson(entity.getMappingsJson()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private void requireTenant(String tenantId) {
        String normalized = requireText(tenantId, "tenantId is required");
        if (!tenantRepository.existsById(normalized)) {
            throw new IllegalArgumentException("Unknown tenantId: " + normalized);
        }
    }

    private void ensureTenantMatch(AudienceMappingProfileEntity entity, String tenantId) {
        if (!tenantId.trim().equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Mapping profile does not belong to tenant: " + tenantId);
        }
    }

    private String normalizeSourceType(String sourceType) {
        return requireText(sourceType, "sourceType is required").toUpperCase(Locale.ROOT);
    }

    private String normalizeField(String value, String message) {
        return requireText(value, message).toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String toJson(Map<String, String> mappings) {
        try {
            return objectMapper.writeValueAsString(mappings);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize fieldMappings", ex);
        }
    }

    private Map<String, String> fromJson(String mappingsJson) {
        try {
            return objectMapper.readValue(mappingsJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid mapping profile payload", ex);
        }
    }

    private Map<String, Object> fromPayloadJson(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            return new HashMap<>();
        }
    }

    private void emitLifecycleEvent(
            AudienceMappingProfileEntity profile,
            String eventType,
            String actor,
            Map<String, Object> payload) {
        String normalizedActor = (actor == null || actor.isBlank()) ? "system" : actor.trim();
        String payloadJson = toPayloadJson(payload);

        AudienceMappingProfileEventEntity eventEntity = new AudienceMappingProfileEventEntity();
        eventEntity.setProfileId(profile.getId());
        eventEntity.setTenantId(profile.getTenantId());
        eventEntity.setEventType(eventType);
        eventEntity.setActor(normalizedActor);
        eventEntity.setEventPayloadJson(payloadJson);
        eventEntity.setCreatedAt(Instant.now());
        mappingProfileEventRepository.save(eventEntity);

        eventPublisher.publishEvent(new AudienceMappingProfileLifecycleEvent(
                profile.getId(),
                profile.getTenantId(),
                eventType,
                normalizedActor,
                payload,
                eventEntity.getCreatedAt()));
    }

    private String toPayloadJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize mapping profile lifecycle payload", ex);
        }
    }
}
