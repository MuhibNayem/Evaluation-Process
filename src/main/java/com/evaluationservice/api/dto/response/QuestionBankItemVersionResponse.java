package com.evaluationservice.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record QuestionBankItemVersionResponse(
        Long id,
        Long questionItemId,
        int versionNo,
        String status,
        String changeSummary,
        String questionText,
        String questionType,
        BigDecimal marks,
        boolean remarksMandatory,
        Map<String, Object> metadata,
        Instant effectiveFrom,
        Instant createdAt,
        Instant updatedAt) {
}
