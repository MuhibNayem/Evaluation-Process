package com.evaluationservice.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for evaluation campaigns.
 */
@Entity
@Table(name = "campaigns")
public class CampaignEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;

    @Column(name = "template_version")
    private int templateVersion;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "scoring_method", length = 30)
    private String scoringMethod;

    @Column(name = "anonymous_mode")
    private boolean anonymousMode;

    @Column(name = "anonymous_roles_json")
    private String anonymousRolesJson;

    @Column(name = "minimum_respondents")
    private int minimumRespondents;

    @Column(name = "assignments_json", columnDefinition = "TEXT")
    private String assignmentsJson;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CampaignEntity() {
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public int getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(int templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getScoringMethod() {
        return scoringMethod;
    }

    public void setScoringMethod(String scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    public boolean isAnonymousMode() {
        return anonymousMode;
    }

    public void setAnonymousMode(boolean anonymousMode) {
        this.anonymousMode = anonymousMode;
    }

    public String getAnonymousRolesJson() {
        return anonymousRolesJson;
    }

    public void setAnonymousRolesJson(String anonymousRolesJson) {
        this.anonymousRolesJson = anonymousRolesJson;
    }

    public int getMinimumRespondents() {
        return minimumRespondents;
    }

    public void setMinimumRespondents(int minimumRespondents) {
        this.minimumRespondents = minimumRespondents;
    }

    public String getAssignmentsJson() {
        return assignmentsJson;
    }

    public void setAssignmentsJson(String assignmentsJson) {
        this.assignmentsJson = assignmentsJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
