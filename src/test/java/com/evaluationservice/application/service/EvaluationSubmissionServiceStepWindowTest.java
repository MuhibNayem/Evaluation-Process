package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase.SubmitEvaluationCommand;
import com.evaluationservice.application.port.out.AssignmentPersistencePort;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Answer;
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
import com.evaluationservice.infrastructure.entity.CampaignStepEntity;
import com.evaluationservice.infrastructure.repository.CampaignStepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EvaluationSubmissionService step-window enforcement")
class EvaluationSubmissionServiceStepWindowTest {

    @Test
    void blocksBeforeStepOpen() {
        var fixture = fixture(step(true, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS), false, 0));
        assertThatThrownBy(() -> fixture.service.submitEvaluation(fixture.command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not opened");
    }

    @Test
    void blocksAfterCloseWithoutLate() {
        var fixture = fixture(step(true, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), false, 0));
        assertThatThrownBy(() -> fixture.service.submitEvaluation(fixture.command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("window is closed");
    }

    @Test
    void allowsDuringLateWindow() {
        var fixture = fixture(step(true, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), true, 2));
        fixture.service.submitEvaluation(fixture.command);
        verify(fixture.assignmentPort).markCompleted(eq("assign-1"), anyString());
    }

    @Test
    void blocksAfterLateWindowExpired() {
        var fixture = fixture(step(true, Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS), true, 2));
        assertThatThrownBy(() -> fixture.service.submitEvaluation(fixture.command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("late submission window is closed");
    }

    @Test
    void blocksWhenStepDisabled() {
        var fixture = fixture(step(false, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), false, 0));
        assertThatThrownBy(() -> fixture.service.submitEvaluation(fixture.command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("step is disabled");
    }

    private Fixture fixture(CampaignStepEntity step) {
        EvaluationPersistencePort evaluationPort = mock(EvaluationPersistencePort.class);
        CampaignPersistencePort campaignPort = mock(CampaignPersistencePort.class);
        AssignmentPersistencePort assignmentPort = mock(AssignmentPersistencePort.class);
        TemplatePersistencePort templatePort = mock(TemplatePersistencePort.class);
        ScoringService scoringService = mock(ScoringService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        SettingsResolverService settingsResolverService = mock(SettingsResolverService.class);
        CampaignStepRepository campaignStepRepository = mock(CampaignStepRepository.class);

        EvaluationSubmissionService service = new EvaluationSubmissionService(
                evaluationPort,
                campaignPort,
                assignmentPort,
                templatePort,
                scoringService,
                eventPublisher,
                settingsResolverService,
                campaignStepRepository);

        CampaignId campaignId = CampaignId.of("camp-step");
        CampaignAssignment assignment = new CampaignAssignment(
                "assign-1",
                campaignId,
                "evaluator-1",
                "evaluatee-1",
                EvaluatorRole.PEER,
                false,
                null,
                "PEER",
                null,
                null,
                null,
                "ACTIVE");

        when(settingsResolverService.resolveBoolean("features.enable-step-windows")).thenReturn(true);
        when(evaluationPort.findByAssignmentId("assign-1")).thenReturn(Optional.empty());
        when(campaignPort.findById(campaignId)).thenReturn(Optional.of(activeCampaign(campaignId)));
        when(assignmentPort.findById("assign-1")).thenReturn(Optional.of(assignment));
        when(campaignStepRepository.findByCampaignIdAndStepType("camp-step", "PEER")).thenReturn(Optional.of(step));
        when(templatePort.findById(TemplateId.of("tmpl-1"))).thenReturn(Optional.of(template()));
        when(scoringService.computeSectionScores(any(Evaluation.class), any(Template.class))).thenReturn(List.of());
        when(scoringService.computeTotalScore(anyList(), any(Template.class)))
                .thenReturn(com.evaluationservice.domain.value.Score.ZERO);
        when(evaluationPort.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitEvaluationCommand command = new SubmitEvaluationCommand(
                campaignId,
                "assign-1",
                "evaluator-1",
                "evaluatee-1",
                "tmpl-1",
                List.of(new Answer("a1", "q1", 5, List.of(), null, Map.of())));
        return new Fixture(service, command, assignmentPort);
    }

    private Campaign activeCampaign(CampaignId id) {
        return new Campaign(
                id,
                "Active Campaign",
                null,
                TemplateId.of("tmpl-1"),
                1,
                CampaignStatus.ACTIVE,
                DateRange.of(Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS)),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                null,
                1,
                "INLINE",
                Map.of(),
                "ALL_TO_ALL",
                Map.of(),
                List.of(),
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

    private record Fixture(
            EvaluationSubmissionService service,
            SubmitEvaluationCommand command,
            AssignmentPersistencePort assignmentPort) {
    }

    private CampaignStepEntity step(
            boolean enabled,
            Instant openAt,
            Instant closeAt,
            boolean lateAllowed,
            int lateDays) {
        CampaignStepEntity entity = new CampaignStepEntity();
        entity.setCampaignId("camp-step");
        entity.setStepType("PEER");
        entity.setEnabled(enabled);
        entity.setOpenAt(openAt);
        entity.setCloseAt(closeAt);
        entity.setLateAllowed(lateAllowed);
        entity.setLateDays(lateDays);
        return entity;
    }
}
