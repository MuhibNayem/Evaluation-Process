package com.evaluationservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record CreateQuestionBankItemVersionRequest(
        @NotBlank String status,
        String changeSummary,
        @NotBlank String questionText,
        @NotBlank String questionType,
        @NotNull BigDecimal marks,
        boolean remarksMandatory,
        Map<String, Object> metadata) {
}
