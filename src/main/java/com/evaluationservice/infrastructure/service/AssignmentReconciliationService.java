package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AssignmentReconciliationResponse;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AssignmentReconciliationService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CampaignRepository campaignRepository;
    private final CampaignAssignmentRepository assignmentRepository;

    public AssignmentReconciliationService(
            CampaignRepository campaignRepository,
            CampaignAssignmentRepository assignmentRepository) {
        this.campaignRepository = campaignRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public AssignmentReconciliationResponse reconcile(String campaignId) {
        Map<String, Boolean> legacy = legacyAssignments(campaignId);
        Map<String, Boolean> relational = relationalAssignments(campaignId);

        Set<String> onlyInLegacy = new LinkedHashSet<>(legacy.keySet());
        onlyInLegacy.removeAll(relational.keySet());

        Set<String> onlyInRelational = new LinkedHashSet<>(relational.keySet());
        onlyInRelational.removeAll(legacy.keySet());

        List<String> completionMismatches = new ArrayList<>();
        for (String key : legacy.keySet()) {
            if (relational.containsKey(key) && !legacy.get(key).equals(relational.get(key))) {
                completionMismatches.add(key);
            }
        }

        boolean consistent = onlyInLegacy.isEmpty() && onlyInRelational.isEmpty() && completionMismatches.isEmpty();

        return new AssignmentReconciliationResponse(
                campaignId,
                legacy.size(),
                relational.size(),
                onlyInLegacy.size(),
                onlyInRelational.size(),
                completionMismatches.size(),
                consistent,
                List.copyOf(onlyInLegacy),
                List.copyOf(onlyInRelational),
                completionMismatches);
    }

    private Map<String, Boolean> legacyAssignments(String campaignId) {
        String rawJson = campaignRepository.findAssignmentsJsonByCampaignId(campaignId);
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }

        try {
            List<Map<String, Object>> items = JSON.readValue(rawJson, new TypeReference<>() {
            });
            Map<String, Boolean> out = new LinkedHashMap<>();
            for (Map<String, Object> item : items) {
                String evaluatorId = asString(item.get("evaluatorId"));
                String evaluateeId = asString(item.get("evaluateeId"));
                String evaluatorRole = asString(item.get("evaluatorRole"));
                if (evaluatorId == null || evaluateeId == null || evaluatorRole == null) {
                    continue;
                }
                String key = key(evaluatorId, evaluateeId, evaluatorRole);
                out.put(key, asBoolean(item.get("completed")));
            }
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse legacy assignments_json for campaign " + campaignId, ex);
        }
    }

    private Map<String, Boolean> relationalAssignments(String campaignId) {
        List<CampaignAssignmentEntity> assignments = assignmentRepository.findByCampaignId(campaignId);
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (CampaignAssignmentEntity assignment : assignments) {
            out.put(
                    key(assignment.getEvaluatorId(), assignment.getEvaluateeId(), assignment.getEvaluatorRole()),
                    assignment.isCompleted());
        }
        return out;
    }

    private String key(String evaluatorId, String evaluateeId, String evaluatorRole) {
        return evaluatorId + "|" + evaluateeId + "|" + evaluatorRole;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
