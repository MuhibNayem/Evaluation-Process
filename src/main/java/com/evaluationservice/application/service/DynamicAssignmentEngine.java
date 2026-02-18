package com.evaluationservice.application.service;

import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.value.CampaignId;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Generates evaluator-to-evaluatee assignments from configurable audience and rule definitions.
 */
@Component
public class DynamicAssignmentEngine {

    private static final Set<String> RESERVED_PARTICIPANT_KEYS = Set.of(
            "userId",
            "id",
            "supervisorId",
            "managerId",
            "attributes");

    public List<CampaignAssignment> generate(
            CampaignId campaignId,
            String audienceSourceType,
            Map<String, Object> audienceSourceConfig,
            String assignmentRuleType,
            Map<String, Object> assignmentRuleConfig,
            List<CampaignAssignment> existingAssignments,
            boolean replaceExistingAssignments) {
        String sourceType = normalize(audienceSourceType);
        String ruleType = normalize(assignmentRuleType);
        Map<String, Object> sourceConfig = audienceSourceConfig == null ? Map.of() : audienceSourceConfig;
        Map<String, Object> ruleConfig = assignmentRuleConfig == null ? Map.of() : assignmentRuleConfig;

        List<Participant> participants = readParticipants(sourceType, sourceConfig);
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Audience source has no participants");
        }

        Set<String> existingKeys = new LinkedHashSet<>();
        if (!replaceExistingAssignments && existingAssignments != null) {
            for (CampaignAssignment existing : existingAssignments) {
                existingKeys.add(key(existing.getEvaluatorId(), existing.getEvaluateeId(), existing.getEvaluatorRole()));
            }
        }

        LinkedHashMap<String, CampaignAssignment> generated = switch (ruleType) {
            case "ALL_TO_ALL" -> allToAll(campaignId, participants, ruleConfig, existingKeys);
            case "ROUND_ROBIN" -> roundRobin(campaignId, participants, ruleConfig, existingKeys);
            case "MANAGER_HIERARCHY" -> managerHierarchy(campaignId, participants, ruleConfig, existingKeys);
            case "ATTRIBUTE_MATCH" -> attributeMatch(campaignId, participants, ruleConfig, existingKeys);
            default -> throw new IllegalArgumentException("Unsupported assignmentRuleType: " + ruleType);
        };

        return new ArrayList<>(generated.values());
    }

    private List<Participant> readParticipants(String sourceType, Map<String, Object> sourceConfig) {
        if (!"INLINE".equals(sourceType) && !"DIRECTORY_SNAPSHOT".equals(sourceType)) {
            throw new IllegalArgumentException("Unsupported audienceSourceType: " + sourceType);
        }
        Object raw = sourceConfig.get("participants");
        if (!(raw instanceof Collection<?> collection)) {
            throw new IllegalArgumentException("audienceSourceConfig.participants must be an array");
        }

        List<Participant> participants = new ArrayList<>();
        for (Object item : collection) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String userId = firstText(map.get("userId"), map.get("id"));
            if (userId == null || userId.isBlank()) {
                continue;
            }

            String supervisorId = firstText(map.get("supervisorId"), map.get("managerId"));
            Map<String, Object> attributes = new LinkedHashMap<>();
            Object attrs = map.get("attributes");
            if (attrs instanceof Map<?, ?> attrsMap) {
                for (var entry : attrsMap.entrySet()) {
                    attributes.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            for (var entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!RESERVED_PARTICIPANT_KEYS.contains(key)) {
                    attributes.putIfAbsent(key, entry.getValue());
                }
            }

            participants.add(new Participant(userId, supervisorId, attributes));
        }
        return participants;
    }

    private LinkedHashMap<String, CampaignAssignment> allToAll(
            CampaignId campaignId,
            List<Participant> participants,
            Map<String, Object> config,
            Set<String> existingKeys) {
        EvaluatorRole role = parseRole(config.get("evaluatorRole"), EvaluatorRole.PEER);
        boolean allowSelf = parseBoolean(config.get("allowSelfEvaluation"), false);
        int maxPerEvaluatee = parseInt(config.get("maxEvaluatorsPerEvaluatee"), Integer.MAX_VALUE);

        LinkedHashMap<String, CampaignAssignment> result = new LinkedHashMap<>();
        for (Participant evaluatee : participants) {
            int assigned = 0;
            for (Participant evaluator : participants) {
                if (!allowSelf && evaluator.userId().equals(evaluatee.userId())) {
                    continue;
                }
                if (assigned >= maxPerEvaluatee) {
                    break;
                }
                boolean added = putAssignment(result, existingKeys, campaignId, evaluator.userId(), evaluatee.userId(),
                        role);
                if (added) {
                    assigned++;
                }
            }
        }
        return result;
    }

    private LinkedHashMap<String, CampaignAssignment> roundRobin(
            CampaignId campaignId,
            List<Participant> participants,
            Map<String, Object> config,
            Set<String> existingKeys) {
        EvaluatorRole role = parseRole(config.get("evaluatorRole"), EvaluatorRole.PEER);
        boolean allowSelf = parseBoolean(config.get("allowSelfEvaluation"), false);
        int evaluatorsPerEvaluatee = Math.max(parseInt(config.get("evaluatorsPerEvaluatee"), 1), 1);

        LinkedHashMap<String, CampaignAssignment> result = new LinkedHashMap<>();
        int offset = 0;
        for (Participant evaluatee : participants) {
            int assigned = 0;
            int attempts = 0;
            while (assigned < evaluatorsPerEvaluatee && attempts < participants.size() * 2) {
                Participant evaluator = participants.get((offset + attempts) % participants.size());
                attempts++;
                if (!allowSelf && evaluator.userId().equals(evaluatee.userId())) {
                    continue;
                }
                boolean added = putAssignment(result, existingKeys, campaignId, evaluator.userId(), evaluatee.userId(),
                        role);
                if (added) {
                    assigned++;
                }
            }
            offset = (offset + 1) % participants.size();
        }
        return result;
    }

    private LinkedHashMap<String, CampaignAssignment> managerHierarchy(
            CampaignId campaignId,
            List<Participant> participants,
            Map<String, Object> config,
            Set<String> existingKeys) {
        EvaluatorRole role = parseRole(config.get("evaluatorRole"), EvaluatorRole.SUPERVISOR);
        boolean includeSelf = parseBoolean(config.get("includeSelfEvaluation"), false);
        boolean requireKnownManager = parseBoolean(config.get("requireKnownManager"), true);

        Set<String> knownUsers = participants.stream().map(Participant::userId).collect(LinkedHashSet::new, Set::add,
                Set::addAll);

        LinkedHashMap<String, CampaignAssignment> result = new LinkedHashMap<>();
        for (Participant evaluatee : participants) {
            String managerId = evaluatee.supervisorId();
            if (managerId == null || managerId.isBlank()) {
                continue;
            }
            if (requireKnownManager && !knownUsers.contains(managerId)) {
                continue;
            }
            if (!includeSelf && managerId.equals(evaluatee.userId())) {
                continue;
            }
            putAssignment(result, existingKeys, campaignId, managerId, evaluatee.userId(), role);
        }
        return result;
    }

    private LinkedHashMap<String, CampaignAssignment> attributeMatch(
            CampaignId campaignId,
            List<Participant> participants,
            Map<String, Object> config,
            Set<String> existingKeys) {
        String matchAttribute = parseString(config.get("matchAttribute"), "department");
        EvaluatorRole role = parseRole(config.get("evaluatorRole"), EvaluatorRole.PEER);
        boolean allowSelf = parseBoolean(config.get("allowSelfEvaluation"), false);
        int maxPerEvaluatee = Math.max(parseInt(config.get("maxEvaluatorsPerEvaluatee"), 3), 1);

        LinkedHashMap<String, CampaignAssignment> result = new LinkedHashMap<>();
        for (Participant evaluatee : participants) {
            Object evaluateeValue = evaluatee.attributes().get(matchAttribute);
            if (evaluateeValue == null) {
                continue;
            }
            int assigned = 0;
            for (Participant evaluator : participants) {
                if (!allowSelf && evaluator.userId().equals(evaluatee.userId())) {
                    continue;
                }
                Object evaluatorValue = evaluator.attributes().get(matchAttribute);
                if (Objects.equals(evaluatorValue, evaluateeValue)) {
                    boolean added = putAssignment(
                            result,
                            existingKeys,
                            campaignId,
                            evaluator.userId(),
                            evaluatee.userId(),
                            role);
                    if (added && ++assigned >= maxPerEvaluatee) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean putAssignment(
            LinkedHashMap<String, CampaignAssignment> target,
            Set<String> existingKeys,
            CampaignId campaignId,
            String evaluatorId,
            String evaluateeId,
            EvaluatorRole role) {
        String key = key(evaluatorId, evaluateeId, role);
        if (existingKeys.contains(key) || target.containsKey(key)) {
            return false;
        }
        target.put(key, new CampaignAssignment(
                UUID.randomUUID().toString(),
                campaignId,
                evaluatorId,
                evaluateeId,
                role,
                false,
                null));
        return true;
    }

    private String key(String evaluatorId, String evaluateeId, EvaluatorRole role) {
        return evaluatorId + "|" + evaluateeId + "|" + role.name();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private EvaluatorRole parseRole(Object value, EvaluatorRole fallback) {
        String normalized = parseString(value, fallback.name()).toUpperCase(Locale.ROOT);
        return EvaluatorRole.valueOf(normalized);
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String parseString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String firstText(Object first, Object second) {
        String a = first == null ? null : String.valueOf(first).trim();
        if (a != null && !a.isEmpty()) {
            return a;
        }
        String b = second == null ? null : String.valueOf(second).trim();
        return (b == null || b.isEmpty()) ? null : b;
    }

    private record Participant(String userId, String supervisorId, Map<String, Object> attributes) {
    }
}
