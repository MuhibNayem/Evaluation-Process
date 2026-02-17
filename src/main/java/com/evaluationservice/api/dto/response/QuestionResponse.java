package com.evaluationservice.api.dto.response;

import com.evaluationservice.domain.enums.QuestionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record QuestionResponse(
        String id,
        String text,
        QuestionType type,
        int orderIndex,
        boolean required,
        List<String> options,
        BigDecimal weight,
        Map<String, Object> metadata,
        String conditionalLogic) {
}
