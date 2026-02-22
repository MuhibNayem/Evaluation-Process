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
    private String audienceSourceType;
    private Map<String, Object> audienceSourceConfig;
    private String assignmentRuleType;
    private Map<String, Object> assignmentRuleConfig;
    private List<CampaignAssignment> assignments;
    private Instant publishedAt;
    private Instant reopenedAt;
    private Instant resultsPublishedAt;
    private boolean locked;
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
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            String assignmentRuleType,
            Map<String, Object> assignmentRuleConfig,
            List<CampaignAssignment> assignments,
            String createdBy,
            Timestamp createdAt,
            Timestamp updatedAt) {
        this(
                id,
                name,
                description,
                templateId,
                templateVersion,
                status,
                dateRange,
                scoringMethod,
                anonymousMode,
                anonymousRoles,
                minimumRespondents,
                audienceSourceType,
                audienceSourceConfig,
                assignmentRuleType,
                assignmentRuleConfig,
                assignments,
                null,
                null,
                null,
                false,
                createdBy,
                createdAt,
                updatedAt);
    }

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
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            String assignmentRuleType,
            Map<String, Object> assignmentRuleConfig,
            List<CampaignAssignment> assignments,
            Instant publishedAt,
            Instant reopenedAt,
            Instant resultsPublishedAt,
            boolean locked,
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
        this.audienceSourceType = normalizeType(audienceSourceType);
        this.audienceSourceConfig = copyConfig(audienceSourceConfig);
        this.assignmentRuleType = normalizeType(assignmentRuleType);
        this.assignmentRuleConfig = copyConfig(assignmentRuleConfig);
        this.assignments = assignments != null ? new ArrayList<>(assignments) : new ArrayList<>();
        this.publishedAt = publishedAt;
        this.reopenedAt = reopenedAt;
        this.resultsPublishedAt = resultsPublishedAt;
        this.locked = locked;
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
        this.locked = true;
    }

    public void publishOpen() {
        transitionTo(CampaignStatus.PUBLISHED_OPEN);
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
        this.locked = true;
    }

    public void schedule() {
        transitionTo(CampaignStatus.SCHEDULED);
    }

    public void close() {
        transitionTo(CampaignStatus.CLOSED);
        this.locked = true;
    }

    public void archive() {
        transitionTo(CampaignStatus.ARCHIVED);
        this.locked = true;
    }

    public void reopen() {
        transitionTo(CampaignStatus.PUBLISHED_OPEN);
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
        this.reopenedAt = Instant.now();
        this.locked = true;
    }

    public void publishResults() {
        transitionTo(CampaignStatus.RESULTS_PUBLISHED);
        this.resultsPublishedAt = Instant.now();
        this.locked = true;
    }

    private void transitionTo(CampaignStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw InvalidStateTransitionException.forCampaign(this.status, target);
        }
        this.status = target;
        this.updatedAt = Timestamp.now();
    }

    public void ensureActive() {
        if (this.status != CampaignStatus.ACTIVE && this.status != CampaignStatus.PUBLISHED_OPEN) {
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

    public void replaceAssignments(List<CampaignAssignment> newAssignments) {
        if (this.status != CampaignStatus.DRAFT && this.status != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot replace assignments for campaign in status: " + this.status);
        }
        this.assignments = new ArrayList<>(Objects.requireNonNull(newAssignments));
        this.updatedAt = Timestamp.now();
    }

    public void extendDeadline(Instant newEndDate) {
        if (this.status != CampaignStatus.ACTIVE && this.status != CampaignStatus.PUBLISHED_OPEN) {
            throw new IllegalStateException("Can only extend deadline of open campaigns");
        }
        this.dateRange = this.dateRange.extendEndDate(newEndDate);
        this.updatedAt = Timestamp.now();
    }

    public void update(String name, String description, DateRange dateRange, ScoringMethod scoringMethod,
            boolean anonymousMode, Set<EvaluatorRole> anonymousRoles, int minimumRespondents,
            String audienceSourceType, Map<String, Object> audienceSourceConfig,
            String assignmentRuleType, Map<String, Object> assignmentRuleConfig) {
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
        if (audienceSourceType != null) {
            this.audienceSourceType = normalizeType(audienceSourceType);
        }
        if (audienceSourceConfig != null) {
            this.audienceSourceConfig = copyConfig(audienceSourceConfig);
        }
        if (assignmentRuleType != null) {
            this.assignmentRuleType = normalizeType(assignmentRuleType);
        }
        if (assignmentRuleConfig != null) {
            this.assignmentRuleConfig = copyConfig(assignmentRuleConfig);
        }

        this.updatedAt = Timestamp.now();
    }

    public void configureDynamicAssignments(
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            String assignmentRuleType,
            Map<String, Object> assignmentRuleConfig) {
        if (this.status != CampaignStatus.DRAFT && this.status != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot configure assignments for campaign in status: " + this.status);
        }
        this.audienceSourceType = normalizeType(audienceSourceType);
        this.audienceSourceConfig = copyConfig(audienceSourceConfig);
        this.assignmentRuleType = normalizeType(assignmentRuleType);
        this.assignmentRuleConfig = copyConfig(assignmentRuleConfig);
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

    public String getAudienceSourceType() {
        return audienceSourceType;
    }

    public Map<String, Object> getAudienceSourceConfig() {
        return Collections.unmodifiableMap(audienceSourceConfig);
    }

    public String getAssignmentRuleType() {
        return assignmentRuleType;
    }

    public Map<String, Object> getAssignmentRuleConfig() {
        return Collections.unmodifiableMap(assignmentRuleConfig);
    }

    public List<CampaignAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getReopenedAt() {
        return reopenedAt;
    }

    public Instant getResultsPublishedAt() {
        return resultsPublishedAt;
    }

    public boolean isLocked() {
        return locked;
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

    private static String normalizeType(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> copyConfig(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
