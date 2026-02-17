package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.exception.CampaignNotActiveException;
import com.evaluationservice.domain.exception.InvalidStateTransitionException;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root for evaluation campaigns.
 * A campaign links a published template to a set of evaluator↔evaluatee
 * assignments
 * with a specific date range and configuration.
 */
public class Campaign {

    private final CampaignId id;
    private String name;
    private String description;
    private TemplateId templateId;
    private int templateVersion;
    private CampaignStatus status;
    private DateRange dateRange;
    private ScoringMethod scoringMethod;
    private boolean anonymousMode;
    private Set<EvaluatorRole> anonymousRoles;
    private int minimumRespondents;
    private List<CampaignAssignment> assignments;
    private final String createdBy;
    private final Timestamp createdAt;
    private Timestamp updatedAt;

    public Campaign(
            CampaignId id,
            String name,
            String description,
            TemplateId templateId,
            int templateVersion,
            CampaignStatus status,
            DateRange dateRange,
            ScoringMethod scoringMethod,
            boolean anonymousMode,
            Set<EvaluatorRole> anonymousRoles,
            int minimumRespondents,
            List<CampaignAssignment> assignments,
            String createdBy,
            Timestamp createdAt,
            Timestamp updatedAt) {
        this.id = Objects.requireNonNull(id, "Campaign ID cannot be null");
        this.name = Objects.requireNonNull(name, "Campaign name cannot be null");
        this.description = description;
        this.templateId = Objects.requireNonNull(templateId, "Template ID cannot be null");
        this.templateVersion = templateVersion;
        this.status = status != null ? status : CampaignStatus.DRAFT;
        this.dateRange = Objects.requireNonNull(dateRange, "Date range cannot be null");
        this.scoringMethod = scoringMethod != null ? scoringMethod : ScoringMethod.WEIGHTED_AVERAGE;
        this.anonymousMode = anonymousMode;
        this.anonymousRoles = anonymousRoles != null ? EnumSet.copyOf(anonymousRoles)
                : EnumSet.noneOf(EvaluatorRole.class);
        this.minimumRespondents = Math.max(minimumRespondents, 1);
        this.assignments = assignments != null ? new ArrayList<>(assignments) : new ArrayList<>();
        this.createdBy = Objects.requireNonNull(createdBy, "CreatedBy cannot be null");
        this.createdAt = createdAt != null ? createdAt : Timestamp.now();
        this.updatedAt = updatedAt != null ? updatedAt : Timestamp.now();

        if (name.isBlank()) {
            throw new IllegalArgumentException("Campaign name cannot be blank");
        }
    }

    // --- Domain Behavior ---

    public void activate() {
        transitionTo(CampaignStatus.ACTIVE);
    }

    public void schedule() {
        transitionTo(CampaignStatus.SCHEDULED);
    }

    public void close() {
        transitionTo(CampaignStatus.CLOSED);
    }

    public void archive() {
        transitionTo(CampaignStatus.ARCHIVED);
    }

    private void transitionTo(CampaignStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw InvalidStateTransitionException.forCampaign(this.status, target);
        }
        this.status = target;
        this.updatedAt = Timestamp.now();
    }

    public void ensureActive() {
        if (this.status != CampaignStatus.ACTIVE) {
            throw new CampaignNotActiveException(this.id);
        }
    }

    public void addAssignment(CampaignAssignment assignment) {
        if (this.status != CampaignStatus.DRAFT && this.status != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot add assignments to a campaign in status: " + this.status);
        }
        this.assignments.add(Objects.requireNonNull(assignment));
        this.updatedAt = Timestamp.now();
    }

    public void addAssignments(List<CampaignAssignment> newAssignments) {
        newAssignments.forEach(this::addAssignment);
    }

    public void extendDeadline(Instant newEndDate) {
        if (this.status != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("Can only extend deadline of active campaigns");
        }
        this.dateRange = this.dateRange.extendEndDate(newEndDate);
        this.updatedAt = Timestamp.now();
    }

    public void update(String name, String description, DateRange dateRange, ScoringMethod scoringMethod,
            boolean anonymousMode, Set<EvaluatorRole> anonymousRoles, int minimumRespondents) {
        if (this.status != CampaignStatus.DRAFT && this.status != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot update details of campaign in status: " + this.status);
        }
        // If SCHEDULED, maybe restricted? For now allow all updates if not ACTIVE.

        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (dateRange != null) {
            this.dateRange = dateRange;
        }
        if (scoringMethod != null) {
            this.scoringMethod = scoringMethod;
        }
        if (anonymousRoles != null) {
            this.anonymousRoles = new HashSet<>(anonymousRoles);
        }
        this.anonymousMode = anonymousMode;
        this.minimumRespondents = Math.max(minimumRespondents, 1);

        this.updatedAt = Timestamp.now();
    }

    // --- Query Methods ---

    public boolean isActiveAt(Instant instant) {
        return this.status == CampaignStatus.ACTIVE && this.dateRange.isActiveAt(instant);
    }

    public boolean isRoleAnonymous(EvaluatorRole role) {
        return this.anonymousMode && this.anonymousRoles.contains(role);
    }

    public long getCompletedAssignmentCount() {
        return assignments.stream().filter(CampaignAssignment::isCompleted).count();
    }

    public double getCompletionPercentage() {
        if (assignments.isEmpty())
            return 0.0;
        return (double) getCompletedAssignmentCount() / assignments.size() * 100;
    }

    // --- Getters ---

    public CampaignId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TemplateId getTemplateId() {
        return templateId;
    }

    public int getTemplateVersion() {
        return templateVersion;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public ScoringMethod getScoringMethod() {
        return scoringMethod;
    }

    public boolean isAnonymousMode() {
        return anonymousMode;
    }

    public Set<EvaluatorRole> getAnonymousRoles() {
        return Collections.unmodifiableSet(anonymousRoles);
    }

    public int getMinimumRespondents() {
        return minimumRespondents;
    }

    public List<CampaignAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Campaign campaign = (Campaign) o;
        return id.equals(campaign.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
