package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class CampaignAssignmentAdapter implements AssignmentPersistencePort {

    private final CampaignAssignmentRepository repository;

    public CampaignAssignmentAdapter(CampaignAssignmentRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void upsertAssignments(CampaignId campaignId, List<CampaignAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        List<String> assignmentIds = assignments.stream()
                .map(CampaignAssignment::getId)
                .toList();
        Map<String, Instant> existingCreatedAt = new HashMap<>();
        repository.findAllById(assignmentIds).forEach(existing -> existingCreatedAt.put(existing.getId(), existing.getCreatedAt()));

        Instant now = Instant.now();
        List<CampaignAssignmentEntity> entities = assignments.stream()
                .map(assignment -> toEntity(campaignId, assignment, now, existingCreatedAt.get(assignment.getId())))
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public void replaceAssignments(CampaignId campaignId, List<CampaignAssignment> assignments) {
        repository.deleteByCampaignId(campaignId.value());
        upsertAssignments(campaignId, assignments);
    }

    @Override
    public void markCompleted(String assignmentId, String evaluationId) {
        int updated = repository.markCompleted(assignmentId, evaluationId, Instant.now());
        if (updated == 0) {
            throw new IllegalStateException("Assignment not found for completion update: " + assignmentId);
        }
    }

    @Override
    public Optional<CampaignAssignment> findById(String assignmentId) {
        return repository.findById(assignmentId)
                .map(this::toDomain);
    }

    private CampaignAssignmentEntity toEntity(
            CampaignId campaignId,
            CampaignAssignment assignment,
            Instant now,
            Instant existingCreatedAt) {
        CampaignAssignmentEntity entity = new CampaignAssignmentEntity();
        entity.setId(assignment.getId());
        entity.setCampaignId(campaignId.value());
        entity.setEvaluatorId(assignment.getEvaluatorId());
        entity.setEvaluateeId(assignment.getEvaluateeId());
        entity.setEvaluatorRole(assignment.getEvaluatorRole().name());
        entity.setCompleted(assignment.isCompleted());
        entity.setEvaluationId(assignment.getEvaluationId());
        entity.setCreatedAt(existingCreatedAt != null ? existingCreatedAt : now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private CampaignAssignment toDomain(CampaignAssignmentEntity entity) {
        return new CampaignAssignment(
                entity.getId(),
                CampaignId.of(entity.getCampaignId()),
                entity.getEvaluatorId(),
                entity.getEvaluateeId(),
                EvaluatorRole.valueOf(entity.getEvaluatorRole()),
                entity.isCompleted(),
                entity.getEvaluationId());
    }
}
