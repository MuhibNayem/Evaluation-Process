package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.port.in.CampaignManagementUseCase;
import com.evaluationservice.application.service.DynamicAssignmentEngine;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.AssignmentRuleDefinitionEntity;
import com.evaluationservice.infrastructure.entity.AssignmentRulePublishRequestEntity;
import com.evaluationservice.infrastructure.repository.AssignmentRuleDefinitionRepository;
import com.evaluationservice.infrastructure.repository.AssignmentRulePublishRequestRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RuleControlPlaneService")
class RuleControlPlaneServiceTest {

    @Test
    @DisplayName("creates draft rule definition with semantic version")
    void createsDraftRuleDefinition() {
        var ruleRepo = mock(AssignmentRuleDefinitionRepository.class);
        var publishRepo = mock(AssignmentRulePublishRequestRepository.class);
        var tenantRepo = mock(TenantRepository.class);
        var campaignUseCase = mock(CampaignManagementUseCase.class);
        var audit = mock(AdminAuditLogService.class);

        when(tenantRepo.existsById("tenant-a")).thenReturn(true);
        when(ruleRepo.save(any())).thenAnswer(invocation -> {
            AssignmentRuleDefinitionEntity e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        RuleControlPlaneService service = new RuleControlPlaneService(
                ruleRepo,
                publishRepo,
                tenantRepo,
                new DynamicAssignmentEngine(),
                campaignUseCase,
                audit,
                properties(true, true),
                new ObjectMapper());

        var response = service.createDraft(
                "tenant-a",
                "University Peer Rule",
                "rule desc",
                "1.0.0",
                "ALL_TO_ALL",
                Map.of("evaluatorRole", "PEER", "allowSelfEvaluation", false),
                "admin-user");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.semanticVersion()).isEqualTo("1.0.0");
        verify(audit).log(eq("tenant-a"), eq("admin-user"), eq("RULE_DEFINITION_CREATED"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("enforces 4-eyes approval when approving publish requests")
    void enforcesFourEyesApproval() {
        var ruleRepo = mock(AssignmentRuleDefinitionRepository.class);
        var publishRepo = mock(AssignmentRulePublishRequestRepository.class);
        var tenantRepo = mock(TenantRepository.class);
        var campaignUseCase = mock(CampaignManagementUseCase.class);
        var audit = mock(AdminAuditLogService.class);

        AssignmentRulePublishRequestEntity request = new AssignmentRulePublishRequestEntity();
        request.setId(1L);
        request.setRuleDefinitionId(10L);
        request.setTenantId("tenant-a");
        request.setStatus("PENDING");
        request.setRequestedBy("same-user");
        when(publishRepo.findById(1L)).thenReturn(Optional.of(request));

        RuleControlPlaneService service = new RuleControlPlaneService(
                ruleRepo,
                publishRepo,
                tenantRepo,
                new DynamicAssignmentEngine(),
                campaignUseCase,
                audit,
                properties(true, true),
                new ObjectMapper());

        assertThatThrownBy(() -> service.approvePublish(1L, "tenant-a", "ok", "same-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("4-eyes");
    }

    @Test
    @DisplayName("simulates rule and returns explainability payload")
    void simulatesRuleAndReturnsExplainability() {
        var ruleRepo = mock(AssignmentRuleDefinitionRepository.class);
        var publishRepo = mock(AssignmentRulePublishRequestRepository.class);
        var tenantRepo = mock(TenantRepository.class);
        var campaignUseCase = mock(CampaignManagementUseCase.class);
        var audit = mock(AdminAuditLogService.class);

        when(tenantRepo.existsById("tenant-a")).thenReturn(true);
        AssignmentRuleDefinitionEntity rule = new AssignmentRuleDefinitionEntity();
        rule.setId(5L);
        rule.setTenantId("tenant-a");
        rule.setName("Rule");
        rule.setSemanticVersion("1.0.0");
        rule.setStatus("PUBLISHED");
        rule.setRuleType("ALL_TO_ALL");
        rule.setRuleConfigJson("{\"evaluatorRole\":\"PEER\",\"allowSelfEvaluation\":false}");
        when(ruleRepo.findByIdAndTenantId(5L, "tenant-a")).thenReturn(Optional.of(rule));

        RuleControlPlaneService service = new RuleControlPlaneService(
                ruleRepo,
                publishRepo,
                tenantRepo,
                new DynamicAssignmentEngine(),
                campaignUseCase,
                audit,
                properties(true, true),
                new ObjectMapper());

        var response = service.simulate(
                5L,
                "tenant-a",
                "INLINE",
                Map.of("participants", List.of(
                        Map.of("userId", "u1"),
                        Map.of("userId", "u2"))),
                true);

        assertThat(response.generatedCount()).isEqualTo(2);
        assertThat(response.generated()).isNotEmpty();
        assertThat(response.generated().getFirst().reason()).contains("Matched by rule type");
        assertThat(response.excluded()).isNotNull();
    }

    @Test
    @DisplayName("publishes assignments through campaign orchestration using approved rule")
    void publishesAssignmentsThroughCampaignOrchestration() {
        var ruleRepo = mock(AssignmentRuleDefinitionRepository.class);
        var publishRepo = mock(AssignmentRulePublishRequestRepository.class);
        var tenantRepo = mock(TenantRepository.class);
        var campaignUseCase = mock(CampaignManagementUseCase.class);
        var audit = mock(AdminAuditLogService.class);

        when(tenantRepo.existsById("tenant-a")).thenReturn(true);
        AssignmentRuleDefinitionEntity rule = new AssignmentRuleDefinitionEntity();
        rule.setId(9L);
        rule.setTenantId("tenant-a");
        rule.setName("Rule");
        rule.setSemanticVersion("1.0.0");
        rule.setStatus("PUBLISHED");
        rule.setRuleType("ALL_TO_ALL");
        rule.setRuleConfigJson("{\"evaluatorRole\":\"PEER\",\"allowSelfEvaluation\":false}");
        when(ruleRepo.findByIdAndTenantId(9L, "tenant-a")).thenReturn(Optional.of(rule));

        Campaign campaign = campaign();
        List<CampaignAssignment> assignments = List.of(new CampaignAssignment(
                "a1",
                CampaignId.of("c1"),
                "u1",
                "u2",
                EvaluatorRole.PEER,
                false,
                null));
        when(campaignUseCase.generateDynamicAssignments(eq(CampaignId.of("c1")), any()))
                .thenReturn(new CampaignManagementUseCase.DynamicAssignmentResult(
                        campaign,
                        assignments,
                        "INLINE",
                        "ALL_TO_ALL",
                        false,
                        false));

        RuleControlPlaneService service = new RuleControlPlaneService(
                ruleRepo,
                publishRepo,
                tenantRepo,
                new DynamicAssignmentEngine(),
                campaignUseCase,
                audit,
                properties(true, true),
                new ObjectMapper());

        var response = service.publishAssignments(
                9L, "tenant-a", "c1", "INLINE", Map.of("participants", List.of()), false, false, "admin");

        assertThat(response.generatedCount()).isEqualTo(1);
        verify(campaignUseCase).generateDynamicAssignments(eq(CampaignId.of("c1")), any());
    }

    private EvaluationServiceProperties properties(boolean publishLock, boolean fourEyes) {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAdmin().setPublishLockEnabled(publishLock);
        properties.getAdmin().setRequireFourEyesApproval(fourEyes);
        return properties;
    }

    private Campaign campaign() {
        return new Campaign(
                CampaignId.of("c1"),
                "Campaign",
                null,
                TemplateId.of("t1"),
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
                List.of(),
                "tester",
                Timestamp.now(),
                Timestamp.now());
    }
}
