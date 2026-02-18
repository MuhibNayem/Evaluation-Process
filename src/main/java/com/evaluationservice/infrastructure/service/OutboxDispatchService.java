package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OutboxDispatchService {

    private static final List<String> DISPATCHABLE_STATUSES = List.of("PENDING");

    private final IntegrationOutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;
    private final EvaluationServiceProperties.Outbox outboxConfig;
    private final MeterRegistry meterRegistry;

    public OutboxDispatchService(
            IntegrationOutboxEventRepository outboxEventRepository,
            OutboxEventPublisher outboxEventPublisher,
            ObjectMapper objectMapper,
            EvaluationServiceProperties properties,
            MeterRegistry meterRegistry) {
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.outboxEventPublisher = Objects.requireNonNull(outboxEventPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.outboxConfig = Objects.requireNonNull(properties).getAudience().getOutbox();
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    @Transactional
    public DispatchResult dispatchDueEvents() {
        long startedAtNanos = System.nanoTime();
        int batchSize = Math.max(1, outboxConfig.getBatchSize());
        Instant now = Instant.now();
        List<IntegrationOutboxEventEntity> candidates = outboxEventRepository.findDispatchCandidates(
                DISPATCHABLE_STATUSES,
                now,
                PageRequest.of(0, batchSize));

        int successCount = 0;
        int retryCount = 0;
        int deadCount = 0;

        for (IntegrationOutboxEventEntity event : candidates) {
            int attempts = event.getAttemptCount() + 1;
            try {
                outboxEventPublisher.publish(new OutboxEventPublisher.OutboxEvent(
                        event.getId(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getEventType(),
                        parsePayload(event.getPayloadJson())));
                event.setAttemptCount(attempts);
                event.setStatus("PUBLISHED");
                event.setPublishedAt(now);
                event.setLastError(null);
                event.setNextAttemptAt(null);
                successCount++;
            } catch (Exception ex) {
                event.setAttemptCount(attempts);
                event.setLastError(ex.getMessage());
                if (attempts >= Math.max(1, outboxConfig.getMaxAttempts())) {
                    event.setStatus("DEAD");
                    event.setNextAttemptAt(null);
                    deadCount++;
                } else {
                    event.setStatus("PENDING");
                    event.setNextAttemptAt(now.plusSeconds(backoffSeconds(attempts)));
                    retryCount++;
                }
            }
            outboxEventRepository.save(event);
        }

        meterRegistry.counter("evaluation.audience.outbox.dispatch.events", "result", "scanned")
                .increment(candidates.size());
        meterRegistry.counter("evaluation.audience.outbox.dispatch.events", "result", "published")
                .increment(successCount);
        meterRegistry.counter("evaluation.audience.outbox.dispatch.events", "result", "retried")
                .increment(retryCount);
        meterRegistry.counter("evaluation.audience.outbox.dispatch.events", "result", "dead")
                .increment(deadCount);
        meterRegistry.timer("evaluation.audience.outbox.dispatch.duration")
                .record(System.nanoTime() - startedAtNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        return new DispatchResult(candidates.size(), successCount, retryCount, deadCount);
    }

    private long backoffSeconds(int attempts) {
        long base = Math.max(1, outboxConfig.getBaseBackoffSeconds());
        long max = Math.max(base, outboxConfig.getMaxBackoffSeconds());
        long exponent = Math.min(30, Math.max(0, attempts - 1));
        long value = base << exponent;
        return Math.min(value, max);
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid outbox payload JSON", ex);
        }
    }

    public record DispatchResult(
            int scanned,
            int published,
            int retried,
            int dead) {
    }
}
