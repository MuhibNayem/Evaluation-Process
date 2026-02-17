package com.evaluationservice.api.dto.response;

import java.util.List;
import java.util.Map;

public record AnswerResponse(
        String id,
        String questionId,
        String value,
        List<String> selectedOptions,
        String textResponse,
        Map<String, Object> metadata) {
}
