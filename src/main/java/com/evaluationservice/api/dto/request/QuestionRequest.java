package com.evaluationservice.api.dto.request;

import com.evaluationservice.domain.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record QuestionRequest(
        String id,
        @NotBlank(message = "Question text is required") String text,
        @NotNull(message = "Question type is required") QuestionType type,
        int orderIndex,
        boolean required,
        List<String> options,
        BigDecimal weight,
        Map<String, Object> metadata,
        String conditionalLogic) {
}
