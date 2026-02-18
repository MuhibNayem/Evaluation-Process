package com.evaluationservice.application.service;

import com.evaluationservice.domain.value.CampaignId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rule Execution Benchmark")
class RuleExecutionBenchmarkTest {

    private final DynamicAssignmentEngine engine = new DynamicAssignmentEngine();

    @Test
    @DisplayName("executes round-robin assignment generation within target latency for benchmark fixture")
    void executesRoundRobinWithinTargetLatency() {
        List<Map<String, Object>> participants = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            participants.add(Map.of("userId", "u" + i, "department", (i % 2 == 0 ? "ENG" : "HR")));
        }

        long start = System.currentTimeMillis();
        var generated = engine.generate(
                CampaignId.of("bench"),
                "INLINE",
                Map.of("participants", participants),
                "ROUND_ROBIN",
                Map.of("evaluatorsPerEvaluatee", 3, "allowSelfEvaluation", false),
                List.of(),
                false);
        long elapsedMs = System.currentTimeMillis() - start;

        assertThat(generated).hasSize(1500);
        // Relaxed budget to avoid CI environment flakiness while still providing a scale gate.
        assertThat(elapsedMs).isLessThan(2500);
    }
}
