package com.evaluationservice.api.dto.response;

import java.util.List;
import java.util.Map;

public record RuleSimulationResponse(
        Long ruleDefinitionId,
        String ruleType,
        int generatedCount,
        List<SimulationMatch> generated,
        List<SimulationExclusion> excluded) {

    public record SimulationMatch(
            String evaluatorId,
            String evaluateeId,
            String evaluatorRole,
            String reason,
            Map<String, Object> metadata) {
    }

    public record SimulationExclusion(
            String evaluatorId,
            String evaluateeId,
            String reason) {
    }
}
