package com.evaluationservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record UpdateCampaignStepsRequest(
        @NotNull @Valid List<StepItem> steps) {

    public record StepItem(
            String stepType,
            boolean enabled,
            int displayOrder,
            Instant openAt,
            Instant closeAt,
            boolean lateAllowed,
            int lateDays,
            String instructions,
            String notes) {
    }
}

