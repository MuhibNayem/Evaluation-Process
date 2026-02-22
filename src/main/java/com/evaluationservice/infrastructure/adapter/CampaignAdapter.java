package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.mapper.DomainEntityMapper;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class CampaignAdapter implements CampaignPersistencePort {

    private final CampaignRepository repository;
    private final CampaignAssignmentRepository assignmentRepository;
    private final DomainEntityMapper mapper;
    private final EvaluationServiceProperties.AssignmentStorageMode assignmentStorageMode;

    public CampaignAdapter(
            CampaignRepository repository,
            CampaignAssignmentRepository assignmentRepository,
            DomainEntityMapper mapper,
            EvaluationServiceProperties properties) {
        this.repository = repository;
        this.assignmentRepository = assignmentRepository;
        this.mapper = mapper;
        this.assignmentStorageMode = properties.getAssignment().getStorageMode();
    }

    @Override
    public Campaign save(Campaign campaign) {
        var entity = mapper.toJpaEntity(campaign);
        var saved = repository.save(entity);
        return toDomainCampaign(saved);
    }

    @Override
    public Optional<Campaign> findById(CampaignId campaignId) {
        return repository.findById(campaignId.value())
                .map(this::toDomainCampaign);
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status, int page, int size) {
        return repository.findByStatus(status.name(), PageRequest.of(page, size))
                .map(this::toDomainCampaign)
                .getContent();
    }

    @Override
    public List<Campaign> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .map(this::toDomainCampaign)
                .getContent();
    }

    @Override
    public boolean existsById(CampaignId campaignId) {
        return repository.existsById(campaignId.value());
    }

    @Override
    public List<Campaign> findByEvaluatorId(String evaluatorId) {
        if (usesRelationalAssignments()) {
            List<String> campaignIds = assignmentRepository.findDistinctCampaignIdsByEvaluatorId(evaluatorId);
            if (campaignIds.isEmpty()) {
                return List.of();
            }
            return repository.findByIdIn(campaignIds).stream()
                    .map(this::toDomainCampaign)
                    .toList();
        }

        return repository.findByEvaluatorId(evaluatorId).stream()
                .map(this::toDomainCampaign)
                .toList();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByStatus(CampaignStatus status) {
        return repository.countByStatus(status.name());
    }

    @Override
    public long countTotalAssignments() {
        if (usesRelationalAssignments()) {
            return assignmentRepository.count();
        }
        return repository.countTotalAssignments();
    }

    @Override
    public long countCompletedAssignments() {
        if (usesRelationalAssignments()) {
            return assignmentRepository.countByCompletedTrue();
        }
        return repository.countCompletedAssignments();
    }

    @Override
    public List<Campaign> findRecentUpdated(int limit) {
        return repository.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, Math.max(limit, 1))).stream()
                .map(this::toDomainCampaign)
                .toList();
    }

    private boolean usesRelationalAssignments() {
        return assignmentStorageMode != EvaluationServiceProperties.AssignmentStorageMode.JSON;
    }

    private Campaign toDomainCampaign(CampaignEntity entity) {
        Campaign mapped = mapper.toDomainCampaign(entity);
        if (!usesRelationalAssignments()) {
            return mapped;
        }

        List<CampaignAssignment> assignments = assignmentRepository.findByCampaignId(entity.getId()).stream()
                .map(a -> new CampaignAssignment(
                        a.getId(),
                        CampaignId.of(a.getCampaignId()),
                        a.getEvaluatorId(),
                        a.getEvaluateeId(),
                        EvaluatorRole.valueOf(a.getEvaluatorRole()),
                        a.isCompleted(),
                        a.getEvaluationId()))
                .toList();

        return new Campaign(
                mapped.getId(),
                mapped.getName(),
                mapped.getDescription(),
                mapped.getTemplateId(),
                mapped.getTemplateVersion(),
                mapped.getStatus(),
                mapped.getDateRange(),
                mapped.getScoringMethod(),
                mapped.isAnonymousMode(),
                mapped.getAnonymousRoles().isEmpty() ? null : mapped.getAnonymousRoles(),
                mapped.getMinimumRespondents(),
                mapped.getAudienceSourceType(),
                mapped.getAudienceSourceConfig(),
                mapped.getAssignmentRuleType(),
                mapped.getAssignmentRuleConfig(),
                assignments,
                mapped.getPublishedAt(),
                mapped.getReopenedAt(),
                mapped.getResultsPublishedAt(),
                mapped.isLocked(),
                mapped.getCreatedBy(),
                mapped.getCreatedAt(),
                mapped.getUpdatedAt());
    }
}
