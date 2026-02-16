package com.evaluationservice.domain.value;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a unique identifier for an evaluation entity.
 * This is a value object that ensures type safety and validation.
 */
public record EvaluationId(String value) implements Comparable<EvaluationId> {

    public EvaluationId {
        Objects.requireNonNull(value, "Evaluation ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Evaluation ID cannot be blank");
        }
    }

    public static EvaluationId generate() {
        return new EvaluationId(UUID.randomUUID().toString());
    }

    public static EvaluationId of(String value) {
        return new EvaluationId(value);
    }

    @Override
    public int compareTo(EvaluationId other) {
        return this.value.compareTo(other.value);
    }
}