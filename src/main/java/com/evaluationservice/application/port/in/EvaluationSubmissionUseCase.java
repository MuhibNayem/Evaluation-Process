package com.evaluationservice.application.port.in;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;

import java.util.List;

/**
 * Inbound port for evaluation submission operations.
 */
public interface EvaluationSubmissionUseCase {

    record SubmitEvaluationCommand(
            CampaignId campaignId,
            String assignmentId,
            String evaluatorId,
            String evaluateeId,
            String templateId,
            List<Answer> answers) {
    }

    record SaveDraftCommand(
            EvaluationId evaluationId,
            List<Answer> answers) {
    }

    /**
     * Submits and scores an evaluation (idempotent).
     */
    Evaluation submitEvaluation(SubmitEvaluationCommand command);

    /**
     * Saves evaluation progress as a draft.
     */
    Evaluation saveDraft(SaveDraftCommand command);

    Evaluation getEvaluation(EvaluationId evaluationId);

    List<Evaluation> listEvaluationsForCampaign(CampaignId campaignId, int page, int size);

    List<Evaluation> listEvaluationsForEvaluatee(String evaluateeId, int page, int size);

    void flagEvaluation(EvaluationId evaluationId);

    void invalidateEvaluation(EvaluationId evaluationId);
}
