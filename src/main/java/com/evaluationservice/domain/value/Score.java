package com.evaluationservice.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a score value with validation and arithmetic operations.
 * Scores are always stored with 4 decimal places of precision.
 */
public record Score(BigDecimal value) {

    public static final Score ZERO = new Score(BigDecimal.ZERO);
    public static final int SCALE = 4;

    public Score {
        Objects.requireNonNull(value, "Score value cannot be null");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Score of(double value) {
        return new Score(BigDecimal.valueOf(value));
    }

    public static Score of(BigDecimal value) {
        return new Score(value);
    }

    public Score add(Score other) {
        return new Score(this.value.add(other.value));
    }

    public Score multiply(Weight weight) {
        return new Score(this.value.multiply(weight.value()));
    }

    public Score divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide score by zero");
        }
        return new Score(this.value.divide(divisor, SCALE, RoundingMode.HALF_UP));
    }

    public double toPercentage(Score maxPossible) {
        if (maxPossible.value.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return this.value.divide(maxPossible.value, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public boolean isGreaterThan(Score other) {
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isLessThan(Score other) {
        return this.value.compareTo(other.value) < 0;
    }
}
