package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AdminSubmissionDetailResponse;
import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class EvaluationAdminService {

    private final EvaluationSubmissionUseCase evaluationUseCase;
    private final CampaignAssignmentRepository assignmentRepository;
    private final CampaignRepository campaignRepository;

    public EvaluationAdminService(
            EvaluationSubmissionUseCase evaluationUseCase,
            CampaignAssignmentRepository assignmentRepository,
            CampaignRepository campaignRepository) {
        this.evaluationUseCase = Objects.requireNonNull(evaluationUseCase);
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
        this.campaignRepository = Objects.requireNonNull(campaignRepository);
    }

    @Transactional(readOnly = true)
    public AdminSubmissionDetailResponse detail(String evaluationId) {
        var evaluation = evaluationUseCase.getEvaluation(EvaluationId.of(evaluationId));
        var assignment = assignmentRepository.findById(evaluation.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + evaluation.getAssignmentId()));
        var campaign = campaignRepository.findById(evaluation.getCampaignId().value())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + evaluation.getCampaignId().value()));

        return new AdminSubmissionDetailResponse(
                evaluation.getId().value(),
                evaluation.getStatus(),
                evaluation.getSubmittedAt() != null ? evaluation.getSubmittedAt().value() : null,
                evaluation.getTotalScore() != null ? evaluation.getTotalScore().value().doubleValue() : null,
                campaign.getId(),
                campaign.getName(),
                com.evaluationservice.domain.enums.CampaignStatus.valueOf(campaign.getStatus()),
                assignment.getId(),
                assignment.getEvaluatorId(),
                assignment.getEvaluateeId(),
                assignment.getEvaluatorRole(),
                assignment.getStepType(),
                assignment.getSectionId(),
                assignment.getFacultyId(),
                assignment.getAnonymityMode(),
                assignment.getStatus());
    }

    @Transactional
    public void reopenSubmission(String evaluationId) {
        var evaluation = evaluationUseCase.reopenEvaluation(EvaluationId.of(evaluationId));
        int updated = assignmentRepository.markReopened(evaluation.getAssignmentId(), Instant.now());
        if (updated == 0) {
            throw new IllegalStateException("Assignment not found for reopen: " + evaluation.getAssignmentId());
        }
    }
}
