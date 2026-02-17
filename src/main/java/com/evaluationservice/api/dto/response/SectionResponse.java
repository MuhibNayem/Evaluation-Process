package com.evaluationservice.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SectionResponse(
        String id,
        String title,
        String description,
        int orderIndex,
        BigDecimal weight,
        List<QuestionResponse> questions) {
}
