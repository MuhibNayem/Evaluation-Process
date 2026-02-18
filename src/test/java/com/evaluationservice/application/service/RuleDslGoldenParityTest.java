package com.evaluationservice.application.service;

import com.evaluationservice.domain.value.CampaignId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rule DSL Golden Parity")
class RuleDslGoldenParityTest {

    private final DynamicAssignmentEngine engine = new DynamicAssignmentEngine();

    @Test
    @DisplayName("produces expected assignment counts for baseline rule fixtures")
    void producesExpectedAssignmentCountsForBaselineRules() {
        CampaignId id = CampaignId.of("golden");
        List<Map<String, Object>> participants = List.of(
                Map.of("userId", "u1", "department", "ENG", "supervisorId", "m1"),
                Map.of("userId", "u2", "department", "ENG", "supervisorId", "m1"),
                Map.of("userId", "u3", "department", "HR", "supervisorId", "m2"),
                Map.of("userId", "m1", "department", "ENG"),
                Map.of("userId", "m2", "department", "HR"));

        assertThat(engine.generate(
                id, "INLINE", Map.of("participants", participants),
                "ALL_TO_ALL", Map.of("allowSelfEvaluation", false),
                List.of(), false)).hasSize(20);

        assertThat(engine.generate(
                id, "INLINE", Map.of("participants", participants),
                "ROUND_ROBIN", Map.of("evaluatorsPerEvaluatee", 2, "allowSelfEvaluation", false),
                List.of(), false)).hasSize(10);

        assertThat(engine.generate(
                id, "INLINE", Map.of("participants", participants),
                "MANAGER_HIERARCHY", Map.of("requireKnownManager", true),
                List.of(), false)).hasSize(3);

        assertThat(engine.generate(
                id, "INLINE", Map.of("participants", participants),
                "ATTRIBUTE_MATCH", Map.of("matchAttribute", "department", "allowSelfEvaluation", false, "maxEvaluatorsPerEvaluatee", 2),
                List.of(), false)).hasSize(8);
    }
}
