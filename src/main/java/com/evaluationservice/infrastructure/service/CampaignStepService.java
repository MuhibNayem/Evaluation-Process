package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.UpdateCampaignStepsRequest;
import com.evaluationservice.api.dto.response.CampaignStepResponse;
import com.evaluationservice.infrastructure.entity.CampaignStepEntity;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.CampaignStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CampaignStepService {

    private static final Set<String> ALLOWED_STEP_TYPES = Set.of("STUDENT", "PEER", "SELF", "DEPARTMENT");

    private final CampaignStepRepository campaignStepRepository;
    private final CampaignRepository campaignRepository;

    public CampaignStepService(CampaignStepRepository campaignStepRepository, CampaignRepository campaignRepository) {
        this.campaignStepRepository = Objects.requireNonNull(campaignStepRepository);
        this.campaignRepository = Objects.requireNonNull(campaignRepository);
    }

    @Transactional(readOnly = true)
    public List<CampaignStepResponse> list(String campaignId) {
        return campaignStepRepository.findByCampaignIdOrderByDisplayOrderAsc(campaignId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<CampaignStepResponse> replace(String campaignId, List<UpdateCampaignStepsRequest.StepItem> steps) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
        ensureStepConfigEditable(campaign.getStatus(), campaign.isLocked());
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("steps cannot be empty");
        }
        Set<String> stepTypes = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (UpdateCampaignStepsRequest.StepItem step : steps) {
            if (step.stepType() == null || step.stepType().isBlank()) {
                throw new IllegalArgumentException("stepType is required");
            }
            String normalizedStepType = step.stepType().trim().toUpperCase();
            if (!ALLOWED_STEP_TYPES.contains(normalizedStepType)) {
                throw new IllegalArgumentException("Unsupported stepType: " + normalizedStepType);
            }
            if (!stepTypes.add(normalizedStepType)) {
                throw new IllegalArgumentException("Duplicate stepType: " + normalizedStepType);
            }
            if (!displayOrders.add(step.displayOrder())) {
                throw new IllegalArgumentException("Duplicate displayOrder: " + step.displayOrder());
            }
            if (step.displayOrder() <= 0) {
                throw new IllegalArgumentException("displayOrder must be >= 1");
            }
            if (step.lateDays() < 0) {
                throw new IllegalArgumentException("lateDays cannot be negative");
            }
            if (!step.lateAllowed() && step.lateDays() > 0) {
                throw new IllegalArgumentException("lateDays must be 0 when lateAllowed=false");
            }
            if (step.openAt() != null && step.closeAt() != null && !step.closeAt().isAfter(step.openAt())) {
                throw new IllegalArgumentException("closeAt must be after openAt for step " + step.stepType());
            }
        }
        ensureDisplayOrderContinuous(displayOrders, steps.size());

        campaignStepRepository.deleteByCampaignId(campaignId);
        Instant now = Instant.now();
        List<CampaignStepEntity> entities = steps.stream()
                .sorted(Comparator.comparingInt(UpdateCampaignStepsRequest.StepItem::displayOrder))
                .map(step -> {
                    CampaignStepEntity entity = new CampaignStepEntity();
                    entity.setCampaignId(campaignId);
                    entity.setStepType(step.stepType().trim().toUpperCase());
                    entity.setEnabled(step.enabled());
                    entity.setDisplayOrder(step.displayOrder());
                    entity.setOpenAt(step.openAt());
                    entity.setCloseAt(step.closeAt());
                    entity.setLateAllowed(step.lateAllowed());
                    entity.setLateDays(step.lateDays());
                    entity.setInstructions(sanitizeText(step.instructions()));
                    entity.setNotes(sanitizeText(step.notes()));
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    return entity;
                })
                .toList();
        return campaignStepRepository.saveAll(entities).stream().map(this::toResponse).toList();
    }

    private void ensureStepConfigEditable(String status, boolean locked) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (locked || (!"DRAFT".equals(normalizedStatus) && !"SCHEDULED".equals(normalizedStatus))) {
            throw new IllegalStateException(
                    "Campaign step configuration is locked for status: " + (status == null ? "UNKNOWN" : status));
        }
    }

    private void ensureDisplayOrderContinuous(Set<Integer> displayOrders, int totalSteps) {
        for (int i = 1; i <= totalSteps; i++) {
            if (!displayOrders.contains(i)) {
                throw new IllegalArgumentException("displayOrder must be continuous from 1 to " + totalSteps);
            }
        }
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CampaignStepResponse toResponse(CampaignStepEntity entity) {
        return new CampaignStepResponse(
                entity.getId(),
                entity.getCampaignId(),
                entity.getStepType(),
                entity.isEnabled(),
                entity.getDisplayOrder(),
                entity.getOpenAt(),
                entity.getCloseAt(),
                entity.isLateAllowed(),
                entity.getLateDays(),
                entity.getInstructions(),
                entity.getNotes());
    }
}
