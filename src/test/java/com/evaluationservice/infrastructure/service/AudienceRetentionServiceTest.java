package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.repository.AudienceIngestionSnapshotRepository;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileEventRepository;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AudienceRetentionService")
class AudienceRetentionServiceTest {

    @Test
    @DisplayName("deletes expired snapshots, events and outbox records")
    void deletesExpiredRecords() {
        AudienceIngestionSnapshotRepository snapshots = mock(AudienceIngestionSnapshotRepository.class);
        AudienceMappingProfileEventRepository events = mock(AudienceMappingProfileEventRepository.class);
        IntegrationOutboxEventRepository outbox = mock(IntegrationOutboxEventRepository.class);

        when(snapshots.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(3L);
        when(events.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(4L);
        when(outbox.deleteByStatusInAndCreatedAtBefore(eq(List.of("PUBLISHED")), any(Instant.class))).thenReturn(2L);
        when(outbox.deleteByStatusInAndCreatedAtBefore(eq(List.of("FAILED", "DEAD")), any(Instant.class))).thenReturn(1L);

        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getRetention().setSnapshotTtlDays(10);
        props.getAudience().getRetention().setMappingEventTtlDays(20);
        props.getAudience().getRetention().setOutboxPublishedTtlDays(30);
        props.getAudience().getRetention().setOutboxFailedTtlDays(40);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AudienceRetentionService service = new AudienceRetentionService(
                snapshots, events, outbox, props, meterRegistry);
        AudienceRetentionService.CleanupResult result = service.cleanup();

        assertThat(result.snapshotsDeleted()).isEqualTo(3L);
        assertThat(result.mappingEventsDeleted()).isEqualTo(4L);
        assertThat(result.outboxPublishedDeleted()).isEqualTo(2L);
        assertThat(result.outboxFailedDeleted()).isEqualTo(1L);
        assertThat(meterRegistry
                .counter("evaluation.audience.retention.deleted.records", "type", "snapshots")
                .count()).isEqualTo(3.0d);
    }
}
