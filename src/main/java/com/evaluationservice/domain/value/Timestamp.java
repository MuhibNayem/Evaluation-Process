package com.evaluationservice.domain.value;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a timestamp value with validation.
 * This is a value object that ensures type safety and validation.
 */
public record Timestamp(Instant value) implements Comparable<Timestamp> {
    
    public Timestamp {
        Objects.requireNonNull(value, "Timestamp cannot be null");
    }

    @Override
    public int compareTo(Timestamp other) {
        return this.value.compareTo(other.value);
    }
    
    public static Timestamp now() {
        return new Timestamp(Instant.now());
    }
}