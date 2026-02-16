package com.evaluationservice.domain.value;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an evaluation template.
 */
public record TemplateId(String value) {

    public TemplateId {
        Objects.requireNonNull(value, "TemplateId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TemplateId cannot be blank");
        }
    }

    public static TemplateId generate() {
        return new TemplateId(UUID.randomUUID().toString());
    }

    public static TemplateId of(String value) {
        return new TemplateId(value);
    }
}
