package com.evaluationservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for first-class campaign assignments.
 */
@Entity
@Table(name = "campaign_assignments")
public class CampaignAssignmentEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "campaign_id", nullable = false, length = 36)
    private String campaignId;

    @Column(name = "evaluator_id", nullable = false)
    private String evaluatorId;

    @Column(name = "evaluatee_id", nullable = false)
    private String evaluateeId;

    @Column(name = "evaluator_role", nullable = false, length = 30)
    private String evaluatorRole;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "evaluation_id", length = 36)
    private String evaluationId;

    @Column(name = "step_type", length = 30)
    private String stepType;

    @Column(name = "section_id", length = 120)
    private String sectionId;

    @Column(name = "faculty_id", length = 120)
    private String facultyId;

    @Column(name = "anonymity_mode", length = 30)
    private String anonymityMode;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CampaignAssignmentEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public void setEvaluatorId(String evaluatorId) {
        this.evaluatorId = evaluatorId;
    }

    public String getEvaluateeId() {
        return evaluateeId;
    }

    public void setEvaluateeId(String evaluateeId) {
        this.evaluateeId = evaluateeId;
    }

    public String getEvaluatorRole() {
        return evaluatorRole;
    }

    public void setEvaluatorRole(String evaluatorRole) {
        this.evaluatorRole = evaluatorRole;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(String evaluationId) {
        this.evaluationId = evaluationId;
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

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getAnonymityMode() {
        return anonymityMode;
    }

    public void setAnonymityMode(String anonymityMode) {
        this.anonymityMode = anonymityMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (status == null || status.isBlank()) {
            status = completed ? "COMPLETED" : "ACTIVE";
        }
    }
}
