package com.evaluationservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audience_ingestion_snapshots")
public class AudienceIngestionSnapshotEntity {

    @Id
    @Column(name = "run_id", length = 80)
    private String runId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "mapping_profile_id")
    private Long mappingProfileId;

    @Column(name = "source_config_json", nullable = false, columnDefinition = "TEXT")
    private String sourceConfigJson;

    @Column(name = "source_records_json", nullable = false, columnDefinition = "TEXT")
    private String sourceRecordsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getMappingProfileId() {
        return mappingProfileId;
    }

    public void setMappingProfileId(Long mappingProfileId) {
        this.mappingProfileId = mappingProfileId;
    }

    public String getSourceConfigJson() {
        return sourceConfigJson;
    }

    public void setSourceConfigJson(String sourceConfigJson) {
        this.sourceConfigJson = sourceConfigJson;
    }

    public String getSourceRecordsJson() {
        return sourceRecordsJson;
    }

    public void setSourceRecordsJson(String sourceRecordsJson) {
        this.sourceRecordsJson = sourceRecordsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
