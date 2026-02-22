package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase;
import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.entity.TemplateEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.EvaluationJpaRepository;
import com.evaluationservice.infrastructure.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "evaluation.service.security.dev-mode=true",
        "spring.datasource.url=jdbc:h2:mem:submission-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "eureka.client.enabled=false",
        "evaluation.service.assignment.storage-mode=JSON"
})
@DisplayName("EvaluationSubmissionService Integration")
class EvaluationSubmissionServiceIntegrationTest {

    @Autowired
    private EvaluationSubmissionUseCase submissionUseCase;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAssignmentRepository assignmentRepository;

    @Autowired
    private EvaluationJpaRepository evaluationRepository;

    @BeforeEach
    void setup() {
        evaluationRepository.deleteAll();
        assignmentRepository.deleteAll();
        campaignRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    @DisplayName("enforces single-write integrity during duplicate submit race")
    void handlesDuplicateSubmitRaceIdempotently() throws Exception {
        String templateId = "tmpl-race-1";
        String campaignId = "camp-race-1";
        String assignmentId = "assign-race-1";
        seedTemplate(templateId);
        seedCampaign(campaignId, templateId, assignmentId, "evaluator-race", "evaluatee-race");
        seedAssignment(campaignId, assignmentId, "evaluator-race", "evaluatee-race");

        EvaluationSubmissionUseCase.SubmitEvaluationCommand command = new EvaluationSubmissionUseCase.SubmitEvaluationCommand(
                CampaignId.of(campaignId),
                assignmentId,
                "evaluator-race",
                "evaluatee-race",
                templateId,
                List.of(new Answer(UUID.randomUUID().toString(), "q1", 8, List.of(), null, Map.of())));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return submissionUseCase.submitEvaluation(command).getId().value();
            });
            Future<String> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return submissionUseCase.submitEvaluation(command).getId().value();
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successCount = 0;
            int failureCount = 0;
            String successId = null;
            for (Future<String> future : List.of(first, second)) {
                try {
                    String id = future.get(10, TimeUnit.SECONDS);
                    successCount++;
                    successId = id;
                } catch (java.util.concurrent.ExecutionException ex) {
                    failureCount++;
                    Throwable root = rootCause(ex);
                    assertThat(root)
                            .isInstanceOfAny(
                                    org.springframework.dao.DataIntegrityViolationException.class,
                                    org.springframework.transaction.UnexpectedRollbackException.class,
                                    org.hibernate.exception.ConstraintViolationException.class,
                                    java.sql.SQLException.class);
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);
            assertThat(evaluationRepository.findByAssignmentId(assignmentId)).isPresent();
            assertThat(evaluationRepository.count()).isEqualTo(1);
            var assignment = assignmentRepository.findById(assignmentId).orElseThrow();
            assertThat(assignment.isCompleted()).isTrue();
            assertThat(assignment.getEvaluationId()).isEqualTo(successId);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("rolls back evaluation persistence when assignment completion update fails")
    void rollsBackWhenAssignmentCompletionFails() {
        String templateId = "tmpl-tx-1";
        String campaignId = "camp-tx-1";
        String assignmentId = "assign-tx-1";
        seedTemplate(templateId);
        seedCampaign(campaignId, templateId, assignmentId, "evaluator-tx", "evaluatee-tx");

        EvaluationSubmissionUseCase.SubmitEvaluationCommand command = new EvaluationSubmissionUseCase.SubmitEvaluationCommand(
                CampaignId.of(campaignId),
                assignmentId,
                "evaluator-tx",
                "evaluatee-tx",
                templateId,
                List.of(new Answer(UUID.randomUUID().toString(), "q1", 6, List.of(), null, Map.of())));

        assertThatThrownBy(() -> submissionUseCase.submitEvaluation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Assignment not found for completion update");

        assertThat(evaluationRepository.findByAssignmentId(assignmentId)).isEmpty();
        assertThat(evaluationRepository.count()).isZero();
        assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
    }

    private void seedTemplate(String templateId) {
        TemplateEntity template = new TemplateEntity();
        template.setId(templateId);
        template.setName("Integration Template " + templateId);
        template.setDescription("Template for submission integration tests");
        template.setCategory("INTEGRATION");
        template.setStatus(TemplateStatus.PUBLISHED.name());
        template.setCurrentVersion(1);
        template.setScoringMethod(ScoringMethod.WEIGHTED_AVERAGE.name());
        template.setSectionsJson("[]");
        template.setCreatedBy("it-test");
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        templateRepository.save(template);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void seedCampaign(String campaignId, String templateId, String assignmentId, String evaluatorId, String evaluateeId) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(campaignId);
        campaign.setName("Integration Campaign " + campaignId);
        campaign.setTemplateId(templateId);
        campaign.setTemplateVersion(1);
        campaign.setStatus(CampaignStatus.ACTIVE.name());
        campaign.setStartDate(Instant.now().minusSeconds(3600));
        campaign.setEndDate(Instant.now().plusSeconds(86400));
        campaign.setScoringMethod(ScoringMethod.WEIGHTED_AVERAGE.name());
        campaign.setAnonymousMode(false);
        campaign.setAnonymousRolesJson("[]");
        campaign.setMinimumRespondents(1);
        campaign.setAudienceSourceType("INLINE");
        campaign.setAudienceSourceConfigJson("{}");
        campaign.setAssignmentRuleType("ALL_TO_ALL");
        campaign.setAssignmentRuleConfigJson("{}");
        campaign.setAssignmentsJson("""
                [{
                  "id":"%s",
                  "evaluatorId":"%s",
                  "evaluateeId":"%s",
                  "evaluatorRole":"PEER",
                  "completed":false
                }]
                """.formatted(assignmentId, evaluatorId, evaluateeId));
        campaign.setCreatedBy("it-test");
        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaignRepository.save(campaign);
    }

    private void seedAssignment(String campaignId, String assignmentId, String evaluatorId, String evaluateeId) {
        CampaignAssignmentEntity assignment = new CampaignAssignmentEntity();
        assignment.setId(assignmentId);
        assignment.setCampaignId(campaignId);
        assignment.setEvaluatorId(evaluatorId);
        assignment.setEvaluateeId(evaluateeId);
        assignment.setEvaluatorRole("PEER");
        assignment.setCompleted(false);
        assignment.setEvaluationId(null);
        assignment.setStatus("ACTIVE");
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);
    }
}
