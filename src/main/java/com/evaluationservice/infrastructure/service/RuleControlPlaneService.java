package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.DynamicAssignmentResponse;
import com.evaluationservice.api.dto.response.RuleDefinitionResponse;
import com.evaluationservice.api.dto.response.RulePublishRequestResponse;
import com.evaluationservice.api.dto.response.RuleSimulationResponse;
import com.evaluationservice.application.port.in.CampaignManagementUseCase;
import com.evaluationservice.application.service.DynamicAssignmentEngine;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.AssignmentRuleDefinitionEntity;
import com.evaluationservice.infrastructure.entity.AssignmentRulePublishRequestEntity;
import com.evaluationservice.infrastructure.repository.AssignmentRuleDefinitionRepository;
import com.evaluationservice.infrastructure.repository.AssignmentRulePublishRequestRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RuleControlPlaneService {

    private static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Set<String> SUPPORTED_RULE_TYPES = Set.of(
            "ALL_TO_ALL", "ROUND_ROBIN", "MANAGER_HIERARCHY", "ATTRIBUTE_MATCH");
    private static final Set<String> SUPPORTED_AUDIENCE_TYPES = Set.of("INLINE", "DIRECTORY_SNAPSHOT");

    private final AssignmentRuleDefinitionRepository ruleDefinitionRepository;
    private final AssignmentRulePublishRequestRepository publishRequestRepository;
    private final TenantRepository tenantRepository;
    private final DynamicAssignmentEngine dynamicAssignmentEngine;
    private final CampaignManagementUseCase campaignManagementUseCase;
    private final AdminAuditLogService auditLogService;
    private final EvaluationServiceProperties.Admin adminConfig;
    private final ObjectMapper objectMapper;

    public RuleControlPlaneService(
            AssignmentRuleDefinitionRepository ruleDefinitionRepository,
            AssignmentRulePublishRequestRepository publishRequestRepository,
            TenantRepository tenantRepository,
            DynamicAssignmentEngine dynamicAssignmentEngine,
            CampaignManagementUseCase campaignManagementUseCase,
            AdminAuditLogService auditLogService,
            EvaluationServiceProperties properties,
            ObjectMapper objectMapper) {
        this.ruleDefinitionRepository = Objects.requireNonNull(ruleDefinitionRepository);
        this.publishRequestRepository = Objects.requireNonNull(publishRequestRepository);
        this.tenantRepository = Objects.requireNonNull(tenantRepository);
        this.dynamicAssignmentEngine = Objects.requireNonNull(dynamicAssignmentEngine);
        this.campaignManagementUseCase = Objects.requireNonNull(campaignManagementUseCase);
        this.auditLogService = Objects.requireNonNull(auditLogService);
        this.adminConfig = Objects.requireNonNull(properties).getAdmin();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public RuleDefinitionResponse createDraft(
            String tenantId,
            String name,
            String description,
            String semanticVersion,
            String ruleType,
            Map<String, Object> ruleConfig,
            String actor) {
        requireTenant(tenantId);
        validateDraftFields(name, semanticVersion, ruleType, ruleConfig);

        AssignmentRuleDefinitionEntity entity = new AssignmentRuleDefinitionEntity();
        entity.setTenantId(tenantId.trim());
        entity.setName(name.trim());
        entity.setDescription(description);
        entity.setSemanticVersion(semanticVersion.trim());
        entity.setStatus("DRAFT");
        entity.setRuleType(ruleType.trim().toUpperCase(Locale.ROOT));
        entity.setRuleConfigJson(toJson(ruleConfig));
        entity.setCreatedBy(actorOrSystem(actor));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        AssignmentRuleDefinitionEntity saved = ruleDefinitionRepository.save(entity);

        auditLogService.log(
                saved.getTenantId(),
                actorOrSystem(actor),
                "RULE_DEFINITION_CREATED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(saved.getId()),
                null,
                null,
                Map.of(
                        "name", saved.getName(),
                        "semanticVersion", saved.getSemanticVersion(),
                        "ruleType", saved.getRuleType()));
        return toResponse(saved);
    }

    @Transactional
    public RuleDefinitionResponse updateDraft(
            Long id,
            String tenantId,
            String name,
            String description,
            String semanticVersion,
            String ruleType,
            Map<String, Object> ruleConfig,
            String actor) {
        requireTenant(tenantId);
        validateDraftFields(name, semanticVersion, ruleType, ruleConfig);
        AssignmentRuleDefinitionEntity entity = getOwnedRule(id, tenantId);
        if (!"DRAFT".equals(entity.getStatus())) {
            throw new IllegalStateException("Only DRAFT rule definitions can be updated");
        }
        entity.setName(name.trim());
        entity.setDescription(description);
        entity.setSemanticVersion(semanticVersion.trim());
        entity.setRuleType(ruleType.trim().toUpperCase(Locale.ROOT));
        entity.setRuleConfigJson(toJson(ruleConfig));
        entity.setUpdatedAt(Instant.now());
        AssignmentRuleDefinitionEntity saved = ruleDefinitionRepository.save(entity);

        auditLogService.log(
                tenantId.trim(),
                actorOrSystem(actor),
                "RULE_DEFINITION_UPDATED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(saved.getId()),
                null,
                null,
                Map.of(
                        "semanticVersion", saved.getSemanticVersion(),
                        "ruleType", saved.getRuleType()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RuleDefinitionResponse> listRules(String tenantId, String status) {
        requireTenant(tenantId);
        if (status == null || status.isBlank()) {
            return ruleDefinitionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId.trim()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return ruleDefinitionRepository.findByTenantIdAndStatusOrderByUpdatedAtDesc(
                tenantId.trim(),
                status.trim().toUpperCase(Locale.ROOT)).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RuleDefinitionResponse getRule(Long id, String tenantId) {
        return toResponse(getOwnedRule(id, tenantId));
    }

    @Transactional
    public RulePublishRequestResponse requestPublish(
            Long ruleDefinitionId,
            String tenantId,
            String reasonCode,
            String comment,
            String actor) {
        AssignmentRuleDefinitionEntity rule = getOwnedRule(ruleDefinitionId, tenantId);
        if (!"DRAFT".equals(rule.getStatus())) {
            throw new IllegalStateException("Only DRAFT rule definitions can be submitted for publish approval");
        }
        if (publishRequestRepository.existsByRuleDefinitionIdAndStatus(rule.getId(), "PENDING")) {
            throw new IllegalStateException("A pending publish request already exists for this rule definition");
        }
        AssignmentRulePublishRequestEntity request = new AssignmentRulePublishRequestEntity();
        request.setRuleDefinitionId(rule.getId());
        request.setTenantId(tenantId.trim());
        request.setStatus("PENDING");
        request.setReasonCode(reasonCode);
        request.setComment(comment);
        request.setRequestedBy(actorOrSystem(actor));
        request.setRequestedAt(Instant.now());
        AssignmentRulePublishRequestEntity saved = publishRequestRepository.save(request);

        auditLogService.log(
                tenantId.trim(),
                actorOrSystem(actor),
                "RULE_PUBLISH_REQUESTED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(ruleDefinitionId),
                reasonCode,
                comment,
                Map.of("publishRequestId", saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public RulePublishRequestResponse approvePublish(Long publishRequestId, String tenantId, String decisionComment, String actor) {
        AssignmentRulePublishRequestEntity request = publishRequestRepository.findById(publishRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Publish request not found: " + publishRequestId));
        if (!tenantId.trim().equals(request.getTenantId())) {
            throw new IllegalArgumentException("Publish request does not belong to tenant: " + tenantId);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Publish request is not pending");
        }
        String approver = actorOrSystem(actor);
        if (adminConfig.isRequireFourEyesApproval() && approver.equals(request.getRequestedBy())) {
            throw new IllegalStateException("4-eyes approval violation: requester cannot approve own publish request");
        }
        request.setStatus("APPROVED");
        request.setDecidedBy(approver);
        request.setDecidedAt(Instant.now());
        request.setDecisionComment(decisionComment);
        AssignmentRulePublishRequestEntity savedRequest = publishRequestRepository.save(request);

        AssignmentRuleDefinitionEntity rule = ruleDefinitionRepository.findById(request.getRuleDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Rule definition not found: " + request.getRuleDefinitionId()));
        if (adminConfig.isPublishLockEnabled()) {
            rule.setStatus("PUBLISHED");
            rule.setPublishedAt(Instant.now());
            rule.setUpdatedAt(Instant.now());
            ruleDefinitionRepository.save(rule);
        }

        auditLogService.log(
                tenantId.trim(),
                approver,
                "RULE_PUBLISH_APPROVED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(rule.getId()),
                null,
                decisionComment,
                Map.of("publishRequestId", savedRequest.getId(), "publishLockEnabled", adminConfig.isPublishLockEnabled()));
        return toResponse(savedRequest);
    }

    @Transactional
    public RulePublishRequestResponse rejectPublish(Long publishRequestId, String tenantId, String decisionComment, String actor) {
        AssignmentRulePublishRequestEntity request = publishRequestRepository.findById(publishRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Publish request not found: " + publishRequestId));
        if (!tenantId.trim().equals(request.getTenantId())) {
            throw new IllegalArgumentException("Publish request does not belong to tenant: " + tenantId);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Publish request is not pending");
        }
        request.setStatus("REJECTED");
        request.setDecidedBy(actorOrSystem(actor));
        request.setDecidedAt(Instant.now());
        request.setDecisionComment(decisionComment);
        AssignmentRulePublishRequestEntity saved = publishRequestRepository.save(request);
        auditLogService.log(
                tenantId.trim(),
                actorOrSystem(actor),
                "RULE_PUBLISH_REJECTED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(saved.getRuleDefinitionId()),
                null,
                decisionComment,
                Map.of("publishRequestId", saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public RuleDefinitionResponse deprecateRule(Long ruleDefinitionId, String tenantId, String comment, String actor) {
        AssignmentRuleDefinitionEntity rule = getOwnedRule(ruleDefinitionId, tenantId);
        rule.setStatus("DEPRECATED");
        rule.setUpdatedAt(Instant.now());
        AssignmentRuleDefinitionEntity saved = ruleDefinitionRepository.save(rule);
        auditLogService.log(
                tenantId.trim(),
                actorOrSystem(actor),
                "RULE_DEPRECATED",
                "ASSIGNMENT_RULE_DEFINITION",
                String.valueOf(saved.getId()),
                null,
                comment,
                Map.of("status", "DEPRECATED"));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RuleSimulationResponse simulate(
            Long ruleDefinitionId,
            String tenantId,
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            boolean diagnosticMode) {
        AssignmentRuleDefinitionEntity rule = getOwnedRule(ruleDefinitionId, tenantId);
        List<com.evaluationservice.domain.entity.CampaignAssignment> assignments = dynamicAssignmentEngine.generate(
                CampaignId.of("00000000-0000-0000-0000-000000000000"),
                audienceSourceType,
                audienceSourceConfig,
                rule.getRuleType(),
                fromJsonObjectMap(rule.getRuleConfigJson()),
                List.of(),
                true);

        List<RuleSimulationResponse.SimulationMatch> generated = assignments.stream()
                .map(a -> new RuleSimulationResponse.SimulationMatch(
                        a.getEvaluatorId(),
                        a.getEvaluateeId(),
                        a.getEvaluatorRole().name(),
                        explainMatch(rule.getRuleType(), fromJsonObjectMap(rule.getRuleConfigJson())),
                        Map.of()))
                .toList();
        List<RuleSimulationResponse.SimulationExclusion> exclusions = diagnosticMode
                ? deriveExclusions(audienceSourceType, audienceSourceConfig, assignments)
                : List.of();
        return new RuleSimulationResponse(rule.getId(), rule.getRuleType(), generated.size(), generated, exclusions);
    }

    @Transactional
    public DynamicAssignmentResponse publishAssignments(
            Long ruleDefinitionId,
            String tenantId,
            String campaignId,
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            boolean replaceExistingAssignments,
            boolean dryRun,
            String actor) {
        AssignmentRuleDefinitionEntity rule = getOwnedRule(ruleDefinitionId, tenantId);
        if (adminConfig.isPublishLockEnabled() && !"PUBLISHED".equals(rule.getStatus())) {
            throw new IllegalStateException("Rule definition must be PUBLISHED before publishAssignments when publish lock is enabled");
        }
        var result = campaignManagementUseCase.generateDynamicAssignments(
                CampaignId.of(campaignId),
                new CampaignManagementUseCase.DynamicAssignmentCommand(
                        normalizeAudienceType(audienceSourceType),
                        audienceSourceConfig,
                        rule.getRuleType(),
                        fromJsonObjectMap(rule.getRuleConfigJson()),
                        replaceExistingAssignments,
                        dryRun));

        auditLogService.log(
                tenantId.trim(),
                actorOrSystem(actor),
                "RULE_ASSIGNMENTS_PUBLISHED",
                "CAMPAIGN",
                campaignId,
                null,
                null,
                Map.of(
                        "ruleDefinitionId", ruleDefinitionId,
                        "generatedCount", result.generatedAssignments().size(),
                        "dryRun", dryRun));

        List<DynamicAssignmentResponse.GeneratedAssignmentItem> generatedItems = result.generatedAssignments().stream()
                .map(a -> new DynamicAssignmentResponse.GeneratedAssignmentItem(
                        a.getId(),
                        a.getEvaluatorId(),
                        a.getEvaluateeId(),
                        a.getEvaluatorRole()))
                .toList();
        return new DynamicAssignmentResponse(
                result.campaign().getId().value(),
                result.audienceSourceType(),
                result.assignmentRuleType(),
                result.replaceExistingAssignments(),
                result.dryRun(),
                generatedItems.size(),
                generatedItems);
    }

    @Transactional(readOnly = true)
    public List<String> supportedRuleTypes() {
        return SUPPORTED_RULE_TYPES.stream().sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<String> supportedAudienceTypes() {
        return SUPPORTED_AUDIENCE_TYPES.stream().sorted().toList();
    }

    private String explainMatch(String ruleType, Map<String, Object> config) {
        return "Matched by rule type " + ruleType + " using config keys " + config.keySet();
    }

    private List<RuleSimulationResponse.SimulationExclusion> deriveExclusions(
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            List<com.evaluationservice.domain.entity.CampaignAssignment> assignments) {
        if (audienceSourceConfig == null) {
            return List.of();
        }
        Object participantsRaw = audienceSourceConfig.get("participants");
        if (!(participantsRaw instanceof List<?> participants)) {
            return List.of();
        }
        Set<String> matched = assignments.stream()
                .map(a -> a.getEvaluatorId() + "|" + a.getEvaluateeId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> ids = new ArrayList<>();
        for (Object p : participants) {
            if (p instanceof Map<?, ?> map) {
                Object userId = map.get("userId");
                if (userId == null) {
                    userId = map.get("id");
                }
                if (userId != null && !String.valueOf(userId).isBlank()) {
                    ids.add(String.valueOf(userId).trim());
                }
            }
        }
        List<RuleSimulationResponse.SimulationExclusion> out = new ArrayList<>();
        int limit = 500;
        for (String evaluator : ids) {
            for (String evaluatee : ids) {
                String key = evaluator + "|" + evaluatee;
                if (!matched.contains(key)) {
                    out.add(new RuleSimulationResponse.SimulationExclusion(
                            evaluator,
                            evaluatee,
                            "Not selected by current rule constraints"));
                    if (out.size() >= limit) {
                        return out;
                    }
                }
            }
        }
        return out;
    }

    private AssignmentRuleDefinitionEntity getOwnedRule(Long id, String tenantId) {
        requireTenant(tenantId);
        return ruleDefinitionRepository.findByIdAndTenantId(id, tenantId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Rule definition not found for tenant: " + id));
    }

    private void validateDraftFields(String name, String semanticVersion, String ruleType, Map<String, Object> ruleConfig) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (semanticVersion == null || semanticVersion.isBlank() || !SEMVER.matcher(semanticVersion.trim()).matches()) {
            throw new IllegalArgumentException("semanticVersion must match semantic version format x.y.z");
        }
        String normalizedRule = normalizeRuleType(ruleType);
        if (!SUPPORTED_RULE_TYPES.contains(normalizedRule)) {
            throw new IllegalArgumentException("Unsupported ruleType: " + normalizedRule);
        }
        if (ruleConfig == null || ruleConfig.isEmpty()) {
            throw new IllegalArgumentException("ruleConfig is required");
        }
    }

    private String normalizeRuleType(String ruleType) {
        if (ruleType == null || ruleType.isBlank()) {
            throw new IllegalArgumentException("ruleType is required");
        }
        return ruleType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAudienceType(String audienceSourceType) {
        if (audienceSourceType == null || audienceSourceType.isBlank()) {
            throw new IllegalArgumentException("audienceSourceType is required");
        }
        String normalized = audienceSourceType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_AUDIENCE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported audienceSourceType: " + normalized);
        }
        return normalized;
    }

    private void requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (!tenantRepository.existsById(tenantId.trim())) {
            throw new IllegalArgumentException("Unknown tenantId: " + tenantId);
        }
    }

    private String actorOrSystem(String actor) {
        if (actor == null || actor.isBlank()) {
            return "system";
        }
        return actor.trim();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize JSON payload", ex);
        }
    }

    private Map<String, Object> fromJsonObjectMap(String payload) {
        try {
            if (payload == null || payload.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse JSON payload", ex);
        }
    }

    private RuleDefinitionResponse toResponse(AssignmentRuleDefinitionEntity entity) {
        return new RuleDefinitionResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSemanticVersion(),
                entity.getStatus(),
                entity.getRuleType(),
                fromJsonObjectMap(entity.getRuleConfigJson()),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt());
    }

    private RulePublishRequestResponse toResponse(AssignmentRulePublishRequestEntity entity) {
        return new RulePublishRequestResponse(
                entity.getId(),
                entity.getRuleDefinitionId(),
                entity.getTenantId(),
                entity.getStatus(),
                entity.getReasonCode(),
                entity.getComment(),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                entity.getDecidedBy(),
                entity.getDecidedAt(),
                entity.getDecisionComment());
    }
}
