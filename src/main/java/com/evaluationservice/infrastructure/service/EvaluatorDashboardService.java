package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.EvaluatorDashboardResponse;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.EvaluationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class EvaluatorDashboardService {

    private final CampaignAssignmentRepository assignmentRepository;
    private final EvaluationJpaRepository evaluationRepository;

    public EvaluatorDashboardService(
            CampaignAssignmentRepository assignmentRepository,
            EvaluationJpaRepository evaluationRepository) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
        this.evaluationRepository = Objects.requireNonNull(evaluationRepository);
    }

    @Transactional(readOnly = true)
    public EvaluatorDashboardResponse getSummary(String evaluatorId) {
        long assigned = assignmentRepository.countByEvaluatorId(evaluatorId);
        long completed = assignmentRepository.countByEvaluatorIdAndCompletedTrue(evaluatorId);
        long pending = Math.max(0, assigned - completed);

        long draft = evaluationRepository.findByEvaluatorId(evaluatorId).stream()
                .filter(e -> "DRAFT".equalsIgnoreCase(e.getStatus()))
                .count();
        long submitted = evaluationRepository.findByEvaluatorId(evaluatorId).stream()
                .filter(e -> "SUBMITTED".equalsIgnoreCase(e.getStatus())
                        || "SCORING".equalsIgnoreCase(e.getStatus())
                        || "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .count();

        double completionPercentage = assigned == 0 ? 0.0 : ((double) completed / (double) assigned) * 100.0;
        return new EvaluatorDashboardResponse(
                evaluatorId,
                assigned,
                completed,
                pending,
                draft,
                submitted,
                completionPercentage);
    }
}
