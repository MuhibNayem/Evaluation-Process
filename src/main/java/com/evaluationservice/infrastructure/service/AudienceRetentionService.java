package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.repository.AudienceIngestionSnapshotRepository;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileEventRepository;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class AudienceRetentionService {

    private static final List<String> OUTBOX_PUBLISHED_STATUSES = List.of("PUBLISHED");
    private static final List<String> OUTBOX_FAILED_STATUSES = List.of("FAILED", "DEAD");

    private final AudienceIngestionSnapshotRepository snapshotRepository;
    private final AudienceMappingProfileEventRepository mappingEventRepository;
    private final IntegrationOutboxEventRepository outboxEventRepository;
    private final EvaluationServiceProperties.Audience audienceConfig;
    private final MeterRegistry meterRegistry;

    public AudienceRetentionService(
            AudienceIngestionSnapshotRepository snapshotRepository,
            AudienceMappingProfileEventRepository mappingEventRepository,
            IntegrationOutboxEventRepository outboxEventRepository,
            EvaluationServiceProperties properties,
            MeterRegistry meterRegistry) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.mappingEventRepository = Objects.requireNonNull(mappingEventRepository);
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.audienceConfig = Objects.requireNonNull(properties).getAudience();
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    @Transactional
    public CleanupResult cleanup() {
        long startedAtNanos = System.nanoTime();
        EvaluationServiceProperties.Retention retention = audienceConfig.getRetention();
        Instant now = Instant.now();

        Instant snapshotCutoff = now.minus(Math.max(retention.getSnapshotTtlDays(), 1), ChronoUnit.DAYS);
        Instant mappingEventCutoff = now.minus(Math.max(retention.getMappingEventTtlDays(), 1), ChronoUnit.DAYS);
        Instant outboxPublishedCutoff = now.minus(Math.max(retention.getOutboxPublishedTtlDays(), 1), ChronoUnit.DAYS);
        Instant outboxFailedCutoff = now.minus(Math.max(retention.getOutboxFailedTtlDays(), 1), ChronoUnit.DAYS);

        long snapshotsDeleted = snapshotRepository.deleteByCreatedAtBefore(snapshotCutoff);
        long mappingEventsDeleted = mappingEventRepository.deleteByCreatedAtBefore(mappingEventCutoff);
        long outboxPublishedDeleted = outboxEventRepository
                .deleteByStatusInAndCreatedAtBefore(OUTBOX_PUBLISHED_STATUSES, outboxPublishedCutoff);
        long outboxFailedDeleted = outboxEventRepository
                .deleteByStatusInAndCreatedAtBefore(OUTBOX_FAILED_STATUSES, outboxFailedCutoff);

        meterRegistry.counter("evaluation.audience.retention.deleted.records", "type", "snapshots")
                .increment(snapshotsDeleted);
        meterRegistry.counter("evaluation.audience.retention.deleted.records", "type", "mapping_events")
                .increment(mappingEventsDeleted);
        meterRegistry.counter("evaluation.audience.retention.deleted.records", "type", "outbox_published")
                .increment(outboxPublishedDeleted);
        meterRegistry.counter("evaluation.audience.retention.deleted.records", "type", "outbox_failed")
                .increment(outboxFailedDeleted);
        meterRegistry.timer("evaluation.audience.retention.cleanup.duration")
                .record(System.nanoTime() - startedAtNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        return new CleanupResult(
                snapshotsDeleted,
                mappingEventsDeleted,
                outboxPublishedDeleted,
                outboxFailedDeleted);
    }

    public record CleanupResult(
            long snapshotsDeleted,
            long mappingEventsDeleted,
            long outboxPublishedDeleted,
            long outboxFailedDeleted) {
    }
}
