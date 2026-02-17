package com.evaluationservice.api.dto.request;

import java.util.List;
import java.util.Map;

public record AnswerRequest(
        String questionId,
        Object value,
        List<String> selectedOptions,
        String textResponse,
        Map<String, Object> metadata) {
}
