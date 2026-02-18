package com.evaluationservice.api.dto.response;

import java.util.List;
import java.util.Map;

public record RuleControlPlaneCapabilitiesResponse(
        List<String> supportedRuleTypes,
        List<String> supportedAudienceSourceTypes,
        Map<String, Object> workflowConfig) {
}
