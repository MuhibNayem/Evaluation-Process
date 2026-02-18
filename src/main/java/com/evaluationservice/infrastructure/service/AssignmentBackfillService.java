package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AssignmentBackfillResponse;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backfills relational assignments from legacy campaigns.assignments_json.
 */
@Service
public class AssignmentBackfillService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CampaignRepository campaignRepository;
    private final CampaignAssignmentRepository assignmentRepository;

    public AssignmentBackfillService(
            CampaignRepository campaignRepository,
            CampaignAssignmentRepository assignmentRepository) {
        this.campaignRepository = campaignRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public AssignmentBackfillResponse backfill(boolean dryRun, int maxCampaigns) {
        int scannedCampaigns = 0;
        int parsedAssignments = 0;
        int insertedAssignments = 0;
        int skippedExisting = 0;
        int invalidAssignments = 0;

        int page = 0;
        int pageSize = 100;
        int limit = Math.max(1, maxCampaigns);

        while (scannedCampaigns < limit) {
            var campaigns = campaignRepository.findAll(PageRequest.of(page, pageSize));
            if (campaigns.isEmpty()) {
                break;
            }
            for (var campaign : campaigns.getContent()) {
                if (scannedCampaigns >= limit) {
                    break;
                }
                scannedCampaigns++;
                BackfillParseResult result = parseAssignments(campaign.getId(), campaign.getAssignmentsJson());
                parsedAssignments += result.valid().size() + result.invalidCount();
                invalidAssignments += result.invalidCount();

                if (result.valid().isEmpty()) {
                    continue;
                }

                Set<String> existingIds = new HashSet<>();
                assignmentRepository.findAllById(result.valid().stream().map(CampaignAssignmentEntity::getId).toList())
                        .forEach(existing -> existingIds.add(existing.getId()));

                List<CampaignAssignmentEntity> toInsert = result.valid().stream()
                        .filter(entity -> !existingIds.contains(entity.getId()))
                        .toList();
                skippedExisting += result.valid().size() - toInsert.size();

                if (!dryRun && !toInsert.isEmpty()) {
                    assignmentRepository.saveAll(toInsert);
                    insertedAssignments += toInsert.size();
                }
            }
            if (!campaigns.hasNext()) {
                break;
            }
            page++;
        }

        return new AssignmentBackfillResponse(
                scannedCampaigns,
                parsedAssignments,
                insertedAssignments,
                skippedExisting,
                invalidAssignments,
                dryRun);
    }

    private BackfillParseResult parseAssignments(String campaignId, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new BackfillParseResult(List.of(), 0);
        }
        try {
            List<Map<String, Object>> items = JSON.readValue(rawJson, new TypeReference<>() {
            });
            List<CampaignAssignmentEntity> valid = new ArrayList<>();
            int invalid = 0;
            Instant now = Instant.now();
            for (Map<String, Object> item : items) {
                String id = asText(item.get("id"));
                String evaluatorId = asText(item.get("evaluatorId"));
                String evaluateeId = asText(item.get("evaluateeId"));
                String evaluatorRole = asText(item.get("evaluatorRole"));
                if (id == null || evaluatorId == null || evaluateeId == null || evaluatorRole == null) {
                    invalid++;
                    continue;
                }
                CampaignAssignmentEntity entity = new CampaignAssignmentEntity();
                entity.setId(id);
                entity.setCampaignId(campaignId);
                entity.setEvaluatorId(evaluatorId);
                entity.setEvaluateeId(evaluateeId);
                entity.setEvaluatorRole(evaluatorRole);
                entity.setCompleted(asBoolean(item.get("completed")));
                entity.setEvaluationId(asText(item.get("evaluationId")));
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                valid.add(entity);
            }
            return new BackfillParseResult(valid, invalid);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse assignments_json for campaign " + campaignId, ex);
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private record BackfillParseResult(List<CampaignAssignmentEntity> valid, int invalidCount) {
    }
}
