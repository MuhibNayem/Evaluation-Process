package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.AnswerRequest;
import com.evaluationservice.api.dto.request.ScoringPreviewRequest;
import com.evaluationservice.api.dto.response.ScoringPreviewResponse;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.application.service.ScoringService;
import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.EvaluationStatus;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ScoringPreviewService {

    private final TemplatePersistencePort templatePersistencePort;
    private final ScoringService scoringService;
    private final EvaluationValidationService validationService;

    public ScoringPreviewService(
            TemplatePersistencePort templatePersistencePort,
            ScoringService scoringService,
            EvaluationValidationService validationService) {
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
        this.scoringService = Objects.requireNonNull(scoringService);
        this.validationService = Objects.requireNonNull(validationService);
    }

    @Transactional(readOnly = true)
    public ScoringPreviewResponse preview(ScoringPreviewRequest request) {
        Template template = templatePersistencePort.findById(TemplateId.of(request.templateId()))
                .orElseThrow(() -> new EntityNotFoundException("Template", request.templateId()));
        Template effectiveTemplate = applyOverrides(template, request);
        List<Answer> answers = toAnswers(request.answers());
        var validation = validationService.validate(request.templateId(), answers);
        if (!validation.valid()) {
            throw new IllegalArgumentException("Scoring preview validation failed with " + validation.issueCount() + " issue(s)");
        }

        Evaluation evaluation = new Evaluation(
                EvaluationId.generate(),
                CampaignId.generate(),
                UUID.randomUUID().toString(),
                "preview-evaluator",
                "preview-evaluatee",
                request.templateId(),
                EvaluationStatus.DRAFT,
                answers,
                null,
                List.of(),
                Timestamp.now(),
                Timestamp.now(),
                null);

        var sectionScores = scoringService.computeSectionScores(evaluation, effectiveTemplate);
        var total = scoringService.computeTotalScore(sectionScores, effectiveTemplate);
        List<ScoringPreviewResponse.SectionPreview> sections = sectionScores.stream()
                .map(s -> new ScoringPreviewResponse.SectionPreview(
                        s.sectionId(),
                        s.sectionTitle(),
                        s.score().value(),
                        s.maxPossibleScore().value(),
                        s.answeredQuestions(),
                        s.totalQuestions()))
                .toList();
        return new ScoringPreviewResponse(
                request.templateId(),
                effectiveTemplate.getScoringMethod(),
                total.value(),
                sections);
    }

    private Template applyOverrides(Template template, ScoringPreviewRequest request) {
        if (request.scoringMethodOverride() == null && (request.customFormulaOverride() == null || request.customFormulaOverride().isBlank())) {
            return template;
        }
        return new Template(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getCategory(),
                template.getStatus(),
                template.getCurrentVersion(),
                request.scoringMethodOverride() != null ? request.scoringMethodOverride() : template.getScoringMethod(),
                template.getSections(),
                template.getCreatedBy(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                request.customFormulaOverride() != null && !request.customFormulaOverride().isBlank()
                        ? request.customFormulaOverride()
                        : template.getCustomFormula());
    }

    private List<Answer> toAnswers(List<AnswerRequest> answerRequests) {
        return answerRequests.stream()
                .map(a -> new Answer(
                        UUID.randomUUID().toString(),
                        a.questionId(),
                        a.value(),
                        a.selectedOptions(),
                        a.textResponse(),
                        a.metadata()))
                .toList();
    }
}
