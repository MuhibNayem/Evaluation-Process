package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAudienceMappingProfileRequest(
        @NotBlank(message = "tenantId is required") String tenantId) {
}
