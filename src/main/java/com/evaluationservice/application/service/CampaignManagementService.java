package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.CampaignManagementUseCase;
import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.service.CampaignLifecycleEventService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing campaign management use cases.
 */
@Service
@Transactional
public class CampaignManagementService implements CampaignManagementUseCase {

    private final CampaignPersistencePort campaignPersistencePort;
    private final TemplatePersistencePort templatePersistencePort;
    private final AssignmentPersistencePort assignmentPersistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final DynamicAssignmentEngine dynamicAssignmentEngine;
    private final CampaignLifecycleEventService campaignLifecycleEventService;

    public CampaignManagementService(
            CampaignPersistencePort campaignPersistencePort,
            TemplatePersistencePort templatePersistencePort,
            AssignmentPersistencePort assignmentPersistencePort,
            ApplicationEventPublisher eventPublisher,
            DynamicAssignmentEngine dynamicAssignmentEngine,
            CampaignLifecycleEventService campaignLifecycleEventService) {
        this.campaignPersistencePort = Objects.requireNonNull(campaignPersistencePort);
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
        this.assignmentPersistencePort = Objects.requireNonNull(assignmentPersistencePort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.dynamicAssignmentEngine = Objects.requireNonNull(dynamicAssignmentEngine);
        this.campaignLifecycleEventService = Objects.requireNonNull(campaignLifecycleEventService);
    }

    @Override
    public Campaign createCampaign(CreateCampaignCommand command) {
        // Verify template exists and is published
        var template = templatePersistencePort.findById(command.templateId())
                .orElseThrow(() -> new EntityNotFoundException("Template", command.templateId().value()));
        if (!template.isUsableForCampaigns()) {
            throw new IllegalStateException("Template must be published to create a campaign");
        }

        var campaign = new Campaign(
                CampaignId.generate(),
                command.name(),
                command.description(),
                command.templateId(),
                command.templateVersion(),
                CampaignStatus.DRAFT,
                command.dateRange(),
                command.scoringMethod(),
                command.anonymousMode(),
                command.anonymousRoles(),
                command.minimumRespondents(),
                command.audienceSourceType(),
                command.audienceSourceConfig(),
                command.assignmentRuleType(),
                command.assignmentRuleConfig(),
                null,
                command.createdBy(),
                Timestamp.now(),
                Timestamp.now());
        return campaignPersistencePort.save(campaign);
    }

    @Override
    public Campaign updateCampaign(UpdateCampaignCommand command) {
        Campaign campaign = findCampaignOrThrow(command.campaignId());

        campaign.update(
                command.name(),
                command.description(),
                command.dateRange(),
                command.scoringMethod(),
                command.anonymousMode(),
                command.anonymousRoles(),
                command.minimumRespondents(),
                command.audienceSourceType(),
                command.audienceSourceConfig(),
                command.assignmentRuleType(),
                command.assignmentRuleConfig());

        return campaignPersistencePort.save(campaign);
    }

    @Override
    public Campaign activateCampaign(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.activate();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "ACTIVATE", null, null);
        return saved;
    }

    @Override
    public Campaign closeCampaign(CampaignId campaignId) {
        return closeCampaign(campaignId, null, null);
    }

    @Override
    public Campaign closeCampaign(CampaignId campaignId, String actor, String reason) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.close();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "CLOSE", actor, reason);

        eventPublisher.publishEvent(new com.evaluationservice.domain.event.CampaignClosedEvent(
                campaignId,
                saved.getCompletionPercentage(),
                saved.getAssignments().size(),
                saved.getCompletedAssignmentCount(),
                Instant.now()));
        return saved;
    }

    @Override
    public Campaign publishCampaign(CampaignId campaignId) {
        return publishCampaign(campaignId, null, null);
    }

    @Override
    public Campaign publishCampaign(CampaignId campaignId, String actor, String reason) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.publishOpen();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "PUBLISH", actor, reason);
        return saved;
    }

    @Override
    public Campaign reopenCampaign(CampaignId campaignId) {
        return reopenCampaign(campaignId, null, null);
    }

    @Override
    public Campaign reopenCampaign(CampaignId campaignId, String actor, String reason) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.reopen();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "REOPEN", actor, reason);
        return saved;
    }

    @Override
    public Campaign publishResults(CampaignId campaignId) {
        return publishResults(campaignId, null, null);
    }

    @Override
    public Campaign publishResults(CampaignId campaignId, String actor, String reason) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.publishResults();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "PUBLISH_RESULTS", actor, reason);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public LifecycleImpactPreview previewLifecycleImpact(CampaignId campaignId, String action) {
        var campaign = findCampaignOrThrow(campaignId);
        long total = campaign.getAssignments().size();
        long completed = campaign.getCompletedAssignmentCount();
        long pending = Math.max(0, total - completed);
        String normalizedAction = action == null ? "UNKNOWN" : action.trim().toUpperCase();
        String summary = switch (normalizedAction) {
            case "PUBLISH" -> "Publishing opens evaluator access and locks configuration paths.";
            case "CLOSE" -> pending + " pending assignments may be blocked from further submission.";
            case "REOPEN" -> "Reopen allows submission flows to continue for eligible assignments.";
            case "PUBLISH_RESULTS" -> "Results become visible to configured viewer roles.";
            default -> "Impact preview generated for requested action.";
        };
        return new LifecycleImpactPreview(campaignId, normalizedAction, total, completed, pending, summary);
    }

    @Override
    public Campaign archiveCampaign(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        String from = campaign.getStatus().name();
        campaign.archive();
        Campaign saved = campaignPersistencePort.save(campaign);
        logLifecycle(saved, from, saved.getStatus().name(), "ARCHIVE", null, null);
        return saved;
    }

    @Override
    public Campaign extendDeadline(CampaignId campaignId, Instant newEndDate) {
        var campaign = findCampaignOrThrow(campaignId);
        campaign.extendDeadline(newEndDate);
        return campaignPersistencePort.save(campaign);
    }

    @Override
    public Campaign addAssignments(CampaignId campaignId, List<AssignmentEntry> entries) {
        var campaign = findCampaignOrThrow(campaignId);

        List<CampaignAssignment> assignments = entries.stream()
                .map(entry -> new CampaignAssignment(
                        UUID.randomUUID().toString(),
                        campaignId,
                        entry.evaluatorId(),
                        entry.evaluateeId(),
                        entry.evaluatorRole(),
                        false,
                        null))
                .toList();

        campaign.addAssignments(assignments);
        Campaign saved = campaignPersistencePort.save(campaign);
        assignmentPersistencePort.upsertAssignments(campaignId, assignments);
        return saved;
    }

    @Override
    public DynamicAssignmentResult generateDynamicAssignments(CampaignId campaignId, DynamicAssignmentCommand command) {
        var campaign = findCampaignOrThrow(campaignId);
        List<CampaignAssignment> generated = dynamicAssignmentEngine.generate(
                campaignId,
                command.audienceSourceType(),
                command.audienceSourceConfig(),
                command.assignmentRuleType(),
                command.assignmentRuleConfig(),
                campaign.getAssignments(),
                command.replaceExistingAssignments());

        if (!command.dryRun()) {
            campaign.configureDynamicAssignments(
                    command.audienceSourceType(),
                    command.audienceSourceConfig(),
                    command.assignmentRuleType(),
                    command.assignmentRuleConfig());

            if (command.replaceExistingAssignments()) {
                campaign.replaceAssignments(generated);
                assignmentPersistencePort.replaceAssignments(campaignId, generated);
            } else if (!generated.isEmpty()) {
                campaign.addAssignments(generated);
                assignmentPersistencePort.upsertAssignments(campaignId, generated);
            }
            campaign = campaignPersistencePort.save(campaign);
        }

        return new DynamicAssignmentResult(
                campaign,
                generated,
                command.audienceSourceType(),
                command.assignmentRuleType(),
                command.replaceExistingAssignments(),
                command.dryRun());
    }

    @Override
    @Transactional(readOnly = true)
    public Campaign getCampaign(CampaignId campaignId) {
        return findCampaignOrThrow(campaignId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> listCampaigns(String status, int page, int size) {
        if (status != null && !status.isBlank()) {
            CampaignStatus campaignStatus = CampaignStatus.valueOf(status.toUpperCase());
            return campaignPersistencePort.findByStatus(campaignStatus, page, size);
        }
        return campaignPersistencePort.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public double getCampaignProgress(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        return campaign.getCompletionPercentage();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> listCampaignsForEvaluator(String evaluatorId) {
        return campaignPersistencePort.findByEvaluatorId(evaluatorId);
    }

    private Campaign findCampaignOrThrow(CampaignId campaignId) {
        return campaignPersistencePort.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign", campaignId.value()));
    }

    private void logLifecycle(
            Campaign campaign,
            String fromStatus,
            String toStatus,
            String action,
            String actor,
            String reason) {
        campaignLifecycleEventService.logTransition(
                campaign.getId().value(),
                fromStatus,
                toStatus,
                action,
                actor,
                reason,
                Map.of(
                        "campaignName", campaign.getName(),
                        "completionPercentage", campaign.getCompletionPercentage()));
    }
}
