package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.enums.EvaluationStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationAdminServiceTest {

    @Mock
    private EvaluationSubmissionUseCase evaluationUseCase;
    @Mock
    private CampaignAssignmentRepository assignmentRepository;
    @Mock
    private CampaignRepository campaignRepository;

    private EvaluationAdminService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationAdminService(evaluationUseCase, assignmentRepository, campaignRepository);
    }

    @Test
    void buildsDetailProjection() {
        Evaluation evaluation = new Evaluation(
                EvaluationId.of("ev1"),
                CampaignId.of("c1"),
                "a1",
                "e1",
                "u1",
                "t1",
                EvaluationStatus.COMPLETED,
                List.of(),
                null,
                List.of(),
                Timestamp.now(),
                Timestamp.now(),
                Timestamp.now());
        CampaignAssignmentEntity assignment = new CampaignAssignmentEntity();
        assignment.setId("a1");
        assignment.setEvaluatorId("e1");
        assignment.setEvaluateeId("u1");
        assignment.setEvaluatorRole("PEER");
        assignment.setStatus("COMPLETED");
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("c1");
        campaign.setName("Campaign");
        campaign.setStatus("CLOSED");

        when(evaluationUseCase.getEvaluation(EvaluationId.of("ev1"))).thenReturn(evaluation);
        when(assignmentRepository.findById("a1")).thenReturn(Optional.of(assignment));
        when(campaignRepository.findById("c1")).thenReturn(Optional.of(campaign));

        var detail = service.detail("ev1");

        assertEquals("ev1", detail.evaluationId());
        assertEquals("c1", detail.campaignId());
        assertEquals("a1", detail.assignmentId());
    }

    @Test
    void reopenAlsoMarksAssignmentReopened() {
        Evaluation evaluation = new Evaluation(
                EvaluationId.of("ev1"),
                CampaignId.of("c1"),
                "a1",
                "e1",
                "u1",
                "t1",
                EvaluationStatus.DRAFT,
                List.of(),
                null,
                List.of(),
                Timestamp.now(),
                Timestamp.now(),
                null);
        when(evaluationUseCase.reopenEvaluation(EvaluationId.of("ev1"))).thenReturn(evaluation);
        when(assignmentRepository.markReopened(
                org.mockito.ArgumentMatchers.eq("a1"),
                org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(1);

        service.reopenSubmission("ev1");

        verify(assignmentRepository).markReopened(
                org.mockito.ArgumentMatchers.eq("a1"),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }
}
