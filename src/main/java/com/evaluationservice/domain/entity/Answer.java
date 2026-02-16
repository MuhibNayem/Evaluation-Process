package com.evaluationservice.domain.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an answer to a single question within an evaluation.
 */
public record Answer(
        String id,
        String questionId,
        Object value,
        List<String> selectedOptions,
        String textResponse,
        Map<String, Object> metadata) {
    public Answer {
        Objects.requireNonNull(id, "Answer ID cannot be null");
        Objects.requireNonNull(questionId, "Question ID cannot be null");
        selectedOptions = selectedOptions != null ? List.copyOf(selectedOptions) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Returns the numeric value of this answer if applicable, or 0.
     */
    public double numericValue() {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    public boolean hasTextResponse() {
        return textResponse != null && !textResponse.isBlank();
    }
}