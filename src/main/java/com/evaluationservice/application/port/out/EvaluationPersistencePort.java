package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for evaluation persistence operations.
 */
public interface EvaluationPersistencePort {

    Evaluation save(Evaluation evaluation);

    Optional<Evaluation> findById(EvaluationId evaluationId);

    Optional<Evaluation> findByAssignmentId(String assignmentId);

    List<Evaluation> findByCampaignId(CampaignId campaignId, int page, int size);

    List<Evaluation> findByEvaluateeId(String evaluateeId, int page, int size);

    List<Evaluation> findCompletedByCampaignAndEvaluatee(CampaignId campaignId, String evaluateeId);

    boolean existsByAssignmentId(String assignmentId);

    long count();

    long countByStatus(String status);

    List<Evaluation> findRecentUpdated(int limit);
}
