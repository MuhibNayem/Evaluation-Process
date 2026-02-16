package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.domain.exception.TemplateAlreadyPublishedException;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for evaluation templates.
 * Templates are versioned — publishing freezes the current version and creates
 * an immutable snapshot.
 */
public class Template {

    private final TemplateId id;
    private String name;
    private String description;
    private String category;
    private TemplateStatus status;
    private int currentVersion;
    private ScoringMethod scoringMethod;
    private List<Section> sections;
    private final String createdBy;
    private final Timestamp createdAt;
    private Timestamp updatedAt;
    private String customFormula;

    public Template(
            TemplateId id,
            String name,
            String description,
            String category,
            TemplateStatus status,
            int currentVersion,
            ScoringMethod scoringMethod,
            List<Section> sections,
            String createdBy,
            Timestamp createdAt,
            Timestamp updatedAt,
            String customFormula) {
        this.id = Objects.requireNonNull(id, "Template ID cannot be null");
        this.name = Objects.requireNonNull(name, "Template name cannot be null");
        this.description = description;
        this.category = category;
        this.status = status != null ? status : TemplateStatus.DRAFT;
        this.currentVersion = currentVersion;
        this.scoringMethod = scoringMethod != null ? scoringMethod : ScoringMethod.WEIGHTED_AVERAGE;
        this.sections = sections != null ? new ArrayList<>(sections) : new ArrayList<>();
        this.createdBy = Objects.requireNonNull(createdBy, "CreatedBy cannot be null");
        this.createdAt = createdAt != null ? createdAt : Timestamp.now();
        this.updatedAt = updatedAt != null ? updatedAt : Timestamp.now();
        this.customFormula = customFormula;

        if (name.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be blank");
        }
    }

    // --- Domain Behavior ---

    /**
     * Publishes the template, making it available for campaigns.
     * Once published, the template version is frozen.
     */
    public void publish() {
        if (this.status == TemplateStatus.PUBLISHED) {
            throw new TemplateAlreadyPublishedException(this.id);
        }
        if (this.sections.isEmpty()) {
            throw new IllegalStateException("Cannot publish a template with no sections");
        }
        boolean hasQuestions = this.sections.stream()
                .anyMatch(section -> !section.getQuestions().isEmpty());
        if (!hasQuestions) {
            throw new IllegalStateException("Cannot publish a template with no questions");
        }
        this.status = TemplateStatus.PUBLISHED;
        this.currentVersion++;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Creates a new draft from this published template for editing.
     */
    public Template createNewDraft() {
        return new Template(
                TemplateId.generate(),
                this.name + " (Draft)",
                this.description,
                this.category,
                TemplateStatus.DRAFT,
                this.currentVersion,
                this.scoringMethod,
                new ArrayList<>(this.sections),
                this.createdBy,
                Timestamp.now(),
                Timestamp.now(),
                this.customFormula);
    }

    public void deprecate() {
        if (this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Only published templates can be deprecated");
        }
        this.status = TemplateStatus.DEPRECATED;
        this.updatedAt = Timestamp.now();
    }

    public void updateDetails(String name, String description, String category) {
        ensureDraft();
        if (name != null && !name.isBlank())
            this.name = name;
        if (description != null)
            this.description = description;
        if (category != null)
            this.category = category;
        this.updatedAt = Timestamp.now();
    }

    public void setScoringMethod(ScoringMethod method, String customFormula) {
        ensureDraft();
        this.scoringMethod = Objects.requireNonNull(method);
        this.customFormula = (method == ScoringMethod.CUSTOM_FORMULA) ? customFormula : null;
        this.updatedAt = Timestamp.now();
    }

    public void addSection(Section section) {
        ensureDraft();
        this.sections.add(Objects.requireNonNull(section));
        this.updatedAt = Timestamp.now();
    }

    public void removeSection(String sectionId) {
        ensureDraft();
        this.sections.removeIf(s -> s.getId().equals(sectionId));
        this.updatedAt = Timestamp.now();
    }

    public void replaceSections(List<Section> sections) {
        ensureDraft();
        this.sections = new ArrayList<>(Objects.requireNonNull(sections));
        this.updatedAt = Timestamp.now();
    }

    private void ensureDraft() {
        if (this.status != TemplateStatus.DRAFT) {
            throw new TemplateAlreadyPublishedException(this.id);
        }
    }

    // --- Query Methods ---

    public int getTotalQuestionCount() {
        return sections.stream().mapToInt(Section::getQuestionCount).sum();
    }

    public boolean isUsableForCampaigns() {
        return this.status.isUsableForNewCampaigns();
    }

    // --- Getters ---

    public TemplateId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public ScoringMethod getScoringMethod() {
        return scoringMethod;
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
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

    public String getCustomFormula() {
        return customFormula;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Template template = (Template) o;
        return id.equals(template.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
