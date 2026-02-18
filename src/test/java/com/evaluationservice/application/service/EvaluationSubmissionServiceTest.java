package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase.SubmitEvaluationCommand;
import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EvaluationSubmissionService")
class EvaluationSubmissionServiceTest {

    @Test
    @DisplayName("rejects submit when assignment tuple does not match evaluator/evaluatee")
    void rejectsSubmitWhenAssignmentMismatched() {
        EvaluationPersistencePort evaluationPort = mock(EvaluationPersistencePort.class);
        CampaignPersistencePort campaignPort = mock(CampaignPersistencePort.class);
        AssignmentPersistencePort assignmentPort = mock(AssignmentPersistencePort.class);
        TemplatePersistencePort templatePort = mock(TemplatePersistencePort.class);
        ScoringService scoringService = mock(ScoringService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        EvaluationSubmissionService service = new EvaluationSubmissionService(
                evaluationPort, campaignPort, assignmentPort, templatePort, scoringService, eventPublisher);

        CampaignId campaignId = CampaignId.of("camp-1");
        when(evaluationPort.findByAssignmentId("assign-1")).thenReturn(Optional.empty());
        when(campaignPort.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(assignmentPort.findById("assign-1")).thenReturn(Optional.of(new CampaignAssignment(
                "assign-1",
                campaignId,
                "another-evaluator",
                "evaluatee-1",
                EvaluatorRole.PEER,
                false,
                null)));

        SubmitEvaluationCommand command = new SubmitEvaluationCommand(
                campaignId,
                "assign-1",
                "evaluator-1",
                "evaluatee-1",
                "tmpl-1",
                List.of());

        assertThatThrownBy(() -> service.submitEvaluation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assignment does not match");
    }

    @Test
    @DisplayName("accepts submit when assignment tuple matches relational assignment")
    void acceptsSubmitWhenAssignmentMatches() {
        EvaluationPersistencePort evaluationPort = mock(EvaluationPersistencePort.class);
        CampaignPersistencePort campaignPort = mock(CampaignPersistencePort.class);
        AssignmentPersistencePort assignmentPort = mock(AssignmentPersistencePort.class);
        TemplatePersistencePort templatePort = mock(TemplatePersistencePort.class);
        ScoringService scoringService = mock(ScoringService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        EvaluationSubmissionService service = new EvaluationSubmissionService(
                evaluationPort, campaignPort, assignmentPort, templatePort, scoringService, eventPublisher);

        CampaignId campaignId = CampaignId.of("camp-2");
        Campaign campaign = activeCampaign(campaignId);
        when(evaluationPort.findByAssignmentId("assign-2")).thenReturn(Optional.empty());
        when(campaignPort.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(assignmentPort.findById("assign-2")).thenReturn(Optional.of(new CampaignAssignment(
                "assign-2",
                campaignId,
                "evaluator-2",
                "evaluatee-2",
                EvaluatorRole.PEER,
                false,
                null)));
        when(templatePort.findById(TemplateId.of("tmpl-1"))).thenReturn(Optional.of(template()));
        when(scoringService.computeSectionScores(any(Evaluation.class), any(Template.class))).thenReturn(List.of());
        when(scoringService.computeTotalScore(anyList(), any(Template.class)))
                .thenReturn(com.evaluationservice.domain.value.Score.ZERO);
        when(evaluationPort.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitEvaluationCommand command = new SubmitEvaluationCommand(
                campaignId,
                "assign-2",
                "evaluator-2",
                "evaluatee-2",
                "tmpl-1",
                List.of(new com.evaluationservice.domain.entity.Answer(
                        "a1",
                        "q1",
                        5,
                        List.of(),
                        null,
                        Map.of())));

        service.submitEvaluation(command);

        verify(assignmentPort).markCompleted(eq("assign-2"), anyString());
    }

    private Campaign activeCampaign(CampaignId id) {
        return new Campaign(
                id,
                "Active Campaign",
                null,
                TemplateId.of("tmpl-1"),
                1,
                CampaignStatus.ACTIVE,
                DateRange.of(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z")),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                null,
                1,
                "INLINE",
                Map.of(),
                "ALL_TO_ALL",
                Map.of(),
                List.of(new CampaignAssignment(
                        "assign-2",
                        id,
                        "evaluator-2",
                        "evaluatee-2",
                        EvaluatorRole.PEER,
                        false,
                        null)),
                "tester",
                Timestamp.now(),
                Timestamp.now());
    }

    private Template template() {
        return new Template(
                TemplateId.of("tmpl-1"),
                "Template",
                null,
                null,
                TemplateStatus.PUBLISHED,
                1,
                ScoringMethod.WEIGHTED_AVERAGE,
                List.of(),
                "tester",
                Timestamp.now(),
                Timestamp.now(),
                null);
    }
}
