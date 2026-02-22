package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateQuestionBankItemRequest(
        @NotBlank String stableKey,
        String contextType,
        String categoryName,
        @NotBlank String defaultType,
        @NotNull BigDecimal defaultMarks) {
}
