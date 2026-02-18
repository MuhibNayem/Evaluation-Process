package com.evaluationservice.application.service;

import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.value.CampaignId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DynamicAssignmentEngine")
class DynamicAssignmentEngineTest {

    private final DynamicAssignmentEngine engine = new DynamicAssignmentEngine();

    @Test
    @DisplayName("generates all-to-all peer assignments without self-evaluation")
    void generatesAllToAllWithoutSelfEvaluation() {
        CampaignId campaignId = CampaignId.of("campaign-1");

        List<CampaignAssignment> generated = engine.generate(
                campaignId,
                "INLINE",
                Map.of("participants", List.of(
                        Map.of("userId", "u1", "department", "ENG"),
                        Map.of("userId", "u2", "department", "ENG"),
                        Map.of("userId", "u3", "department", "ENG"))),
                "ALL_TO_ALL",
                Map.of("evaluatorRole", "PEER", "allowSelfEvaluation", false),
                List.of(),
                false);

        assertThat(generated).hasSize(6);
        assertThat(generated)
                .allMatch(a -> a.getEvaluatorRole() == EvaluatorRole.PEER)
                .noneMatch(a -> a.getEvaluatorId().equals(a.getEvaluateeId()));
    }

    @Test
    @DisplayName("respects existing assignments when replaceExistingAssignments is false")
    void skipsExistingAssignmentsWhenNotReplacing() {
        CampaignId campaignId = CampaignId.of("campaign-2");
        CampaignAssignment existing = new CampaignAssignment(
                "existing-1",
                campaignId,
                "u1",
                "u2",
                EvaluatorRole.PEER,
                false,
                null);

        List<CampaignAssignment> generated = engine.generate(
                campaignId,
                "INLINE",
                Map.of("participants", List.of(
                        Map.of("userId", "u1"),
                        Map.of("userId", "u2"))),
                "ALL_TO_ALL",
                Map.of("evaluatorRole", "PEER", "allowSelfEvaluation", false),
                List.of(existing),
                false);

        assertThat(generated).hasSize(1);
        assertThat(generated.getFirst().getEvaluatorId()).isEqualTo("u2");
        assertThat(generated.getFirst().getEvaluateeId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("generates manager hierarchy assignments from supervisorId")
    void generatesManagerHierarchyAssignments() {
        CampaignId campaignId = CampaignId.of("campaign-3");

        List<CampaignAssignment> generated = engine.generate(
                campaignId,
                "INLINE",
                Map.of("participants", List.of(
                        Map.of("userId", "mgr1"),
                        Map.of("userId", "u1", "supervisorId", "mgr1"),
                        Map.of("userId", "u2", "supervisorId", "mgr1"))),
                "MANAGER_HIERARCHY",
                Map.of(),
                List.of(),
                false);

        assertThat(generated).hasSize(2);
        assertThat(generated)
                .allMatch(a -> a.getEvaluatorId().equals("mgr1"))
                .allMatch(a -> a.getEvaluatorRole() == EvaluatorRole.SUPERVISOR);
    }
}
