package com.evaluationservice.domain.event;

import com.evaluationservice.domain.value.TemplateId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when a template is published.
 */
public record TemplatePublishedEvent(
        TemplateId templateId,
        int version,
        String publishedBy,
        Instant occurredAt) {
    public TemplatePublishedEvent {
        Objects.requireNonNull(templateId);
        Objects.requireNonNull(publishedBy);
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }
}
