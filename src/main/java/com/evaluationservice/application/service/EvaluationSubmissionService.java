package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase;
import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.entity.SectionScore;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.EvaluationStatus;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.domain.value.Score;
import com.evaluationservice.domain.value.Timestamp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application service implementing evaluation submission use cases.
 * Supports idempotent submission, draft saving, and scoring.
 */
@Service
@Transactional
public class EvaluationSubmissionService implements EvaluationSubmissionUseCase {

    private final EvaluationPersistencePort evaluationPersistencePort;
    private final CampaignPersistencePort campaignPersistencePort;
    private final AssignmentPersistencePort assignmentPersistencePort;
    private final TemplatePersistencePort templatePersistencePort;
    private final ScoringService scoringService;
    private final ApplicationEventPublisher eventPublisher;

    public EvaluationSubmissionService(
            EvaluationPersistencePort evaluationPersistencePort,
            CampaignPersistencePort campaignPersistencePort,
            AssignmentPersistencePort assignmentPersistencePort,
            TemplatePersistencePort templatePersistencePort,
            ScoringService scoringService,
            ApplicationEventPublisher eventPublisher) {
        this.evaluationPersistencePort = Objects.requireNonNull(evaluationPersistencePort);
        this.campaignPersistencePort = Objects.requireNonNull(campaignPersistencePort);
        this.assignmentPersistencePort = Objects.requireNonNull(assignmentPersistencePort);
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
        this.scoringService = Objects.requireNonNull(scoringService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public Evaluation submitEvaluation(SubmitEvaluationCommand command) {
        // Idempotency check — if already submitted, return existing
        var existing = evaluationPersistencePort.findByAssignmentId(command.assignmentId());
        if (existing.isPresent() && existing.get().isCompleted()) {
            return existing.get();
        }

        // Verify campaign is active
        var campaign = campaignPersistencePort.findById(command.campaignId())
                .orElseThrow(() -> new EntityNotFoundException("Campaign", command.campaignId().value()));
        campaign.ensureActive();
        validateAssignmentOwnership(command, campaign);

        // Get template for scoring
        var template = templatePersistencePort.findById(
                com.evaluationservice.domain.value.TemplateId.of(command.templateId()))
                .orElseThrow(() -> new EntityNotFoundException("Template", command.templateId()));

        // Create or update evaluation
        Evaluation evaluation;
        if (existing.isPresent()) {
            evaluation = existing.get();
            evaluation.saveDraft(command.answers());
        } else {
            evaluation = new Evaluation(
                    EvaluationId.generate(),
                    command.campaignId(),
                    command.assignmentId(),
                    command.evaluatorId(),
                    command.evaluateeId(),
                    command.templateId(),
                    EvaluationStatus.DRAFT,
                    command.answers(),
                    null,
                    null,
                    Timestamp.now(),
                    Timestamp.now(),
                    null);
        }

        // Submit and score
        evaluation.submit();
        List<SectionScore> sectionScores = scoringService.computeSectionScores(evaluation, template);
        Score totalScore = scoringService.computeTotalScore(sectionScores, template);
        evaluation.complete(totalScore, sectionScores);

        Evaluation saved = evaluationPersistencePort.save(evaluation);
        assignmentPersistencePort.markCompleted(command.assignmentId(), saved.getId().value());
        campaign.getAssignments().stream()
                .filter(a -> a.getId().equals(command.assignmentId()))
                .findFirst()
                .ifPresent(a -> a.markCompleted(saved.getId().value()));
        campaignPersistencePort.save(campaign);

        // Publish domain event
        eventPublisher.publishEvent(
                com.evaluationservice.domain.event.EvaluationSubmittedEvent.of(
                        saved.getId(), command.campaignId(), command.evaluatorId(), command.evaluateeId()));

        return saved;
    }

    @Override
    public Evaluation saveDraft(SaveDraftCommand command) {
        var evaluation = findEvaluationOrThrow(command.evaluationId());
        evaluation.saveDraft(command.answers());
        return evaluationPersistencePort.save(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public Evaluation getEvaluation(EvaluationId evaluationId) {
        return findEvaluationOrThrow(evaluationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evaluation> listEvaluationsForCampaign(CampaignId campaignId, int page, int size) {
        return evaluationPersistencePort.findByCampaignId(campaignId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evaluation> listEvaluationsForEvaluatee(String evaluateeId, int page, int size) {
        return evaluationPersistencePort.findByEvaluateeId(evaluateeId, page, size);
    }

    @Override
    public void flagEvaluation(EvaluationId evaluationId) {
        var evaluation = findEvaluationOrThrow(evaluationId);
        evaluation.flag();
        evaluationPersistencePort.save(evaluation);
    }

    @Override
    public void invalidateEvaluation(EvaluationId evaluationId) {
        var evaluation = findEvaluationOrThrow(evaluationId);
        evaluation.invalidate();
        evaluationPersistencePort.save(evaluation);
    }

    private Evaluation findEvaluationOrThrow(EvaluationId evaluationId) {
        return evaluationPersistencePort.findById(evaluationId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation", evaluationId.value()));
    }

    private void validateAssignmentOwnership(SubmitEvaluationCommand command, com.evaluationservice.domain.entity.Campaign campaign) {
        boolean valid = assignmentPersistencePort.findById(command.assignmentId())
                .map(assignment -> assignment.getCampaignId().equals(command.campaignId())
                        && assignment.getEvaluatorId().equals(command.evaluatorId())
                        && assignment.getEvaluateeId().equals(command.evaluateeId()))
                .orElseGet(() -> campaign.getAssignments().stream()
                        .anyMatch(assignment -> assignment.getId().equals(command.assignmentId())
                                && assignment.getEvaluatorId().equals(command.evaluatorId())
                                && assignment.getEvaluateeId().equals(command.evaluateeId())));

        if (!valid) {
            throw new IllegalArgumentException("Assignment does not match campaign/evaluator/evaluatee");
        }
    }
}
