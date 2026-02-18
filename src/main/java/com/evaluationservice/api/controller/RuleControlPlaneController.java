package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateRuleDefinitionRequest;
import com.evaluationservice.api.dto.request.CreateRulePublishRequest;
import com.evaluationservice.api.dto.request.DecideRulePublishRequest;
import com.evaluationservice.api.dto.request.PublishRuleAssignmentsRequest;
import com.evaluationservice.api.dto.request.SimulateRuleRequest;
import com.evaluationservice.api.dto.request.UpdateRuleDefinitionRequest;
import com.evaluationservice.api.dto.response.DynamicAssignmentResponse;
import com.evaluationservice.api.dto.response.RuleControlPlaneCapabilitiesResponse;
import com.evaluationservice.api.dto.response.RuleDefinitionResponse;
import com.evaluationservice.api.dto.response.RulePublishRequestResponse;
import com.evaluationservice.api.dto.response.RuleSimulationResponse;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.RuleControlPlaneService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/rules")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class RuleControlPlaneController {

    private final RuleControlPlaneService ruleControlPlaneService;
    private final EvaluationServiceProperties.Admin adminConfig;

    public RuleControlPlaneController(
            RuleControlPlaneService ruleControlPlaneService,
            EvaluationServiceProperties properties) {
        this.ruleControlPlaneService = ruleControlPlaneService;
        this.adminConfig = properties.getAdmin();
    }

    @PostMapping
    public ResponseEntity<RuleDefinitionResponse> createRuleDefinition(
            @Valid @RequestBody CreateRuleDefinitionRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.createDraft(
                request.tenantId(),
                request.name(),
                request.description(),
                request.semanticVersion(),
                request.ruleType(),
                request.ruleConfig(),
                actor()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleDefinitionResponse> updateRuleDefinition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRuleDefinitionRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.updateDraft(
                id,
                request.tenantId(),
                request.name(),
                request.description(),
                request.semanticVersion(),
                request.ruleType(),
                request.ruleConfig(),
                actor()));
    }

    @GetMapping
    public ResponseEntity<List<RuleDefinitionResponse>> listRuleDefinitions(
            @RequestParam String tenantId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ruleControlPlaneService.listRules(tenantId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleDefinitionResponse> getRuleDefinition(
            @PathVariable Long id,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(ruleControlPlaneService.getRule(id, tenantId));
    }

    @PostMapping("/{id}/publish-requests")
    public ResponseEntity<RulePublishRequestResponse> createPublishRequest(
            @PathVariable Long id,
            @Valid @RequestBody CreateRulePublishRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.requestPublish(
                id,
                request.tenantId(),
                request.reasonCode(),
                request.comment(),
                actor()));
    }

    @PostMapping("/publish-requests/{publishRequestId}/approve")
    public ResponseEntity<RulePublishRequestResponse> approvePublishRequest(
            @PathVariable Long publishRequestId,
            @Valid @RequestBody DecideRulePublishRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.approvePublish(
                publishRequestId,
                request.tenantId(),
                request.decisionComment(),
                actor()));
    }

    @PostMapping("/publish-requests/{publishRequestId}/reject")
    public ResponseEntity<RulePublishRequestResponse> rejectPublishRequest(
            @PathVariable Long publishRequestId,
            @Valid @RequestBody DecideRulePublishRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.rejectPublish(
                publishRequestId,
                request.tenantId(),
                request.decisionComment(),
                actor()));
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<RuleDefinitionResponse> deprecateRuleDefinition(
            @PathVariable Long id,
            @Valid @RequestBody DecideRulePublishRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.deprecateRule(
                id,
                request.tenantId(),
                request.decisionComment(),
                actor()));
    }

    @PostMapping("/{id}/simulate")
    public ResponseEntity<RuleSimulationResponse> simulateRule(
            @PathVariable Long id,
            @Valid @RequestBody SimulateRuleRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.simulate(
                id,
                request.tenantId(),
                request.audienceSourceType(),
                request.audienceSourceConfig(),
                request.diagnosticMode()));
    }

    @PostMapping("/{id}/publish-assignments")
    public ResponseEntity<DynamicAssignmentResponse> publishAssignments(
            @PathVariable Long id,
            @Valid @RequestBody PublishRuleAssignmentsRequest request) {
        return ResponseEntity.ok(ruleControlPlaneService.publishAssignments(
                id,
                request.tenantId(),
                request.campaignId(),
                request.audienceSourceType(),
                request.audienceSourceConfig(),
                request.replaceExistingAssignments(),
                request.dryRun(),
                actor()));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<RuleControlPlaneCapabilitiesResponse> capabilities() {
        return ResponseEntity.ok(new RuleControlPlaneCapabilitiesResponse(
                ruleControlPlaneService.supportedRuleTypes(),
                ruleControlPlaneService.supportedAudienceTypes(),
                Map.of(
                        "publishLockEnabled", adminConfig.isPublishLockEnabled(),
                        "requireFourEyesApproval", adminConfig.isRequireFourEyesApproval())));
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "system";
        }
        return authentication.getName();
    }
}
