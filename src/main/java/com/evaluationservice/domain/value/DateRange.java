package com.evaluationservice.domain.value;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a date range with start and end timestamps.
 * Used for campaign scheduling and deadline management.
 */
public record DateRange(Instant startDate, Instant endDate) {

    public DateRange {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date (%s) cannot be before start date (%s)".formatted(endDate, startDate));
        }
    }

    public static DateRange of(Instant start, Instant end) {
        return new DateRange(start, end);
    }

    public boolean isActiveAt(Instant instant) {
        return !instant.isBefore(startDate) && !instant.isAfter(endDate);
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(startDate);
    }

    public boolean hasEnded(Instant now) {
        return now.isAfter(endDate);
    }

    public DateRange extendEndDate(Instant newEndDate) {
        return new DateRange(this.startDate, newEndDate);
    }
}
