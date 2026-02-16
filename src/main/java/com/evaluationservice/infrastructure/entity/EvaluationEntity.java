package com.evaluationservice.infrastructure.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity for individual evaluations (submissions).
 */
@Entity
@Table(name = "evaluations", uniqueConstraints = @UniqueConstraint(name = "uk_evaluation_assignment", columnNames = "assignment_id"))
public class EvaluationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "campaign_id", nullable = false, length = 36)
    private String campaignId;

    @Column(name = "assignment_id", nullable = false, length = 36)
    private String assignmentId;

    @Column(name = "evaluator_id", nullable = false)
    private String evaluatorId;

    @Column(name = "evaluatee_id", nullable = false)
    private String evaluateeId;

    @Column(name = "template_id", nullable = false, length = 36)
    private String templateId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "section_scores_json", columnDefinition = "TEXT")
    private String sectionScoresJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    public EvaluationEntity() {
    }

    // --- Getters and Setters ---

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

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public String getSectionScoresJson() {
        return sectionScoresJson;
    }

    public void setSectionScoresJson(String sectionScoresJson) {
        this.sectionScoresJson = sectionScoresJson;
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}