package com.evaluationservice.domain.value;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a weight factor for scoring calculations.
 * Weight must be between 0 (exclusive) and 1 (inclusive).
 */
public record Weight(BigDecimal value) {

    public static final Weight EQUAL = new Weight(BigDecimal.ONE);

    public Weight {
        Objects.requireNonNull(value, "Weight value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0, got: " + value);
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Weight must not exceed 1, got: " + value);
        }
    }

    public static Weight of(double value) {
        return new Weight(BigDecimal.valueOf(value));
    }

    public static Weight of(BigDecimal value) {
        return new Weight(value);
    }
}
