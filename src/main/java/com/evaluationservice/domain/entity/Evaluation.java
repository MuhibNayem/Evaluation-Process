package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.EvaluationStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.domain.value.Score;
import com.evaluationservice.domain.value.Timestamp;

import java.util.*;

/**
 * Aggregate root for an individual evaluation submission.
 * Represents one evaluator's assessment of one evaluatee.
 */
public class Evaluation {

    private final EvaluationId id;
    private final CampaignId campaignId;
    private final String assignmentId;
    private final String evaluatorId;
    private final String evaluateeId;
    private final String templateId;
    private EvaluationStatus status;
    private List<Answer> answers;
    private Score totalScore;
    private List<SectionScore> sectionScores;
    private final Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp submittedAt;

    public Evaluation(
            EvaluationId id,
            CampaignId campaignId,
            String assignmentId,
            String evaluatorId,
            String evaluateeId,
            String templateId,
            EvaluationStatus status,
            List<Answer> answers,
            Score totalScore,
            List<SectionScore> sectionScores,
            Timestamp createdAt,
            Timestamp updatedAt,
            Timestamp submittedAt) {
        this.id = Objects.requireNonNull(id, "Evaluation ID cannot be null");
        this.campaignId = Objects.requireNonNull(campaignId, "Campaign ID cannot be null");
        this.assignmentId = Objects.requireNonNull(assignmentId, "Assignment ID cannot be null");
        this.evaluatorId = Objects.requireNonNull(evaluatorId, "Evaluator ID cannot be null");
        this.evaluateeId = Objects.requireNonNull(evaluateeId, "Evaluatee ID cannot be null");
        this.templateId = Objects.requireNonNull(templateId, "Template ID cannot be null");
        this.status = status != null ? status : EvaluationStatus.DRAFT;
        this.answers = answers != null ? new ArrayList<>(answers) : new ArrayList<>();
        this.totalScore = totalScore;
        this.sectionScores = sectionScores != null ? new ArrayList<>(sectionScores) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Timestamp.now();
        this.updatedAt = updatedAt != null ? updatedAt : Timestamp.now();
        this.submittedAt = submittedAt;
    }

    // --- Domain Behavior ---

    /**
     * Saves answers as a draft (partial completion).
     */
    public void saveDraft(List<Answer> newAnswers) {
        if (this.status.isTerminal()) {
            throw new IllegalStateException("Cannot modify a completed or invalidated evaluation");
        }
        this.answers = new ArrayList<>(newAnswers);
        this.status = EvaluationStatus.DRAFT;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Submits the evaluation for scoring.
     */
    public void submit() {
        if (!this.status.canSubmit()) {
            throw new IllegalStateException(
                    "Evaluation can only be submitted from DRAFT status, current: " + this.status);
        }
        if (this.answers.isEmpty()) {
            throw new IllegalStateException("Cannot submit an evaluation with no answers");
        }
        this.status = EvaluationStatus.SUBMITTED;
        this.submittedAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    /**
     * Records the scoring result after processing.
     */
    public void complete(Score totalScore, List<SectionScore> sectionScores) {
        this.totalScore = Objects.requireNonNull(totalScore);
        this.sectionScores = new ArrayList<>(sectionScores);
        this.status = EvaluationStatus.COMPLETED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Flags the evaluation for admin review.
     */
    public void flag() {
        this.status = EvaluationStatus.FLAGGED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Invalidates the evaluation (admin action).
     */
    public void invalidate() {
        this.status = EvaluationStatus.INVALIDATED;
        this.updatedAt = Timestamp.now();
    }

    public void reopenForRevision() {
        if (this.status != EvaluationStatus.COMPLETED && this.status != EvaluationStatus.FLAGGED) {
            throw new IllegalStateException("Only COMPLETED or FLAGGED evaluations can be reopened");
        }
        this.status = EvaluationStatus.DRAFT;
        this.submittedAt = null;
        this.updatedAt = Timestamp.now();
    }

    // --- Query Methods ---

    public List<Answer> answersForSection(Section section) {
        Set<String> questionIds = new HashSet<>();
        for (Question q : section.getQuestions()) {
            questionIds.add(q.getId());
        }
        return answers.stream()
                .filter(a -> questionIds.contains(a.questionId()))
                .toList();
    }

    public boolean isCompleted() {
        return this.status == EvaluationStatus.COMPLETED;
    }

    // --- Getters ---

    public EvaluationId getId() {
        return id;
    }

    public CampaignId getCampaignId() {
        return campaignId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public String getEvaluateeId() {
        return evaluateeId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    public Score getTotalScore() {
        return totalScore;
    }

    public List<SectionScore> getSectionScores() {
        return Collections.unmodifiableList(sectionScores);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Evaluation that = (Evaluation) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
