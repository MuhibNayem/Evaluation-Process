package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.service.audience.AudienceSourceConnector.SourceRecord;
import com.evaluationservice.infrastructure.entity.AudienceIngestionSnapshotEntity;
import com.evaluationservice.infrastructure.repository.AudienceIngestionSnapshotRepository;
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
public class AudienceIngestionSnapshotService {

    private final AudienceIngestionSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public AudienceIngestionSnapshotService(
            AudienceIngestionSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public void save(
            String runId,
            String tenantId,
            String sourceType,
            Long mappingProfileId,
            Map<String, Object> sourceConfig,
            List<SourceRecord> records) {
        AudienceIngestionSnapshotEntity entity = new AudienceIngestionSnapshotEntity();
        entity.setRunId(runId);
        entity.setTenantId(tenantId);
        entity.setSourceType(sourceType);
        entity.setMappingProfileId(mappingProfileId);
        entity.setSourceConfigJson(toJson(sourceConfig == null ? Map.of() : sourceConfig));
        entity.setSourceRecordsJson(toJson(records == null ? List.of() : records));
        entity.setCreatedAt(Instant.now());
        snapshotRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Snapshot load(String runId) {
        AudienceIngestionSnapshotEntity entity = snapshotRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Ingestion snapshot not found for run: " + runId));
        return new Snapshot(
                entity.getRunId(),
                entity.getTenantId(),
                entity.getSourceType(),
                entity.getMappingProfileId(),
                fromConfigJson(entity.getSourceConfigJson()),
                fromRecordsJson(entity.getSourceRecordsJson()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize ingestion snapshot payload", ex);
        }
    }

    private Map<String, Object> fromConfigJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid ingestion snapshot sourceConfig payload", ex);
        }
    }

    private List<SourceRecord> fromRecordsJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<SourceRecord>>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid ingestion snapshot records payload", ex);
        }
    }

    public record Snapshot(
            String runId,
            String tenantId,
            String sourceType,
            Long mappingProfileId,
            Map<String, Object> sourceConfig,
            List<SourceRecord> records) {
    }
}
