package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.CampaignManagementUseCase;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.Timestamp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final ApplicationEventPublisher eventPublisher;

    public CampaignManagementService(
            CampaignPersistencePort campaignPersistencePort,
            TemplatePersistencePort templatePersistencePort,
            ApplicationEventPublisher eventPublisher) {
        this.campaignPersistencePort = Objects.requireNonNull(campaignPersistencePort);
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
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
                null,
                command.createdBy(),
                Timestamp.now(),
                Timestamp.now());
        return campaignPersistencePort.save(campaign);
    }

    @Override
    public Campaign activateCampaign(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        campaign.activate();
        return campaignPersistencePort.save(campaign);
    }

    @Override
    public Campaign closeCampaign(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        campaign.close();
        Campaign saved = campaignPersistencePort.save(campaign);

        eventPublisher.publishEvent(new com.evaluationservice.domain.event.CampaignClosedEvent(
                campaignId,
                saved.getCompletionPercentage(),
                saved.getAssignments().size(),
                saved.getCompletedAssignmentCount(),
                Instant.now()));
        return saved;
    }

    @Override
    public Campaign archiveCampaign(CampaignId campaignId) {
        var campaign = findCampaignOrThrow(campaignId);
        campaign.archive();
        return campaignPersistencePort.save(campaign);
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
        return campaignPersistencePort.save(campaign);
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

    private Campaign findCampaignOrThrow(CampaignId campaignId) {
        return campaignPersistencePort.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign", campaignId.value()));
    }
}
