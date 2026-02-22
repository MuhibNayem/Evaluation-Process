package com.evaluationservice.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record QuestionBankItemResponse(
        Long id,
        Long setId,
        String stableKey,
        String contextType,
        String categoryName,
        String defaultType,
        BigDecimal defaultMarks,
        int activeVersionNo,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
