package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OutboxDispatchService")
class OutboxDispatchServiceTest {

    @Test
    @DisplayName("marks event as published when publish succeeds")
    void marksEventPublishedOnSuccess() throws Exception {
        IntegrationOutboxEventRepository repository = mock(IntegrationOutboxEventRepository.class);
        OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAudience().getOutbox().setBatchSize(10);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        IntegrationOutboxEventEntity event = event(1L, "PENDING", 0, null);
        when(repository.findDispatchCandidates(any(), any(), any(Pageable.class))).thenReturn(List.of(event));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxDispatchService service = new OutboxDispatchService(
                repository, publisher, new ObjectMapper(), properties, meterRegistry);

        OutboxDispatchService.DispatchResult result = service.dispatchDueEvents();

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.published()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(meterRegistry
                .counter("evaluation.audience.outbox.dispatch.events", "result", "published")
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("keeps event pending with next attempt when publish fails before max attempts")
    void retriesWhenFailureBeforeMaxAttempts() throws Exception {
        IntegrationOutboxEventRepository repository = mock(IntegrationOutboxEventRepository.class);
        OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
        doThrow(new RuntimeException("temporary")).when(publisher).publish(any());

        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAudience().getOutbox().setMaxAttempts(5);
        properties.getAudience().getOutbox().setBaseBackoffSeconds(5);
        properties.getAudience().getOutbox().setMaxBackoffSeconds(60);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        IntegrationOutboxEventEntity event = event(2L, "PENDING", 1, null);
        when(repository.findDispatchCandidates(any(), any(), any(Pageable.class))).thenReturn(List.of(event));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxDispatchService service = new OutboxDispatchService(
                repository, publisher, new ObjectMapper(), properties, meterRegistry);
        OutboxDispatchService.DispatchResult result = service.dispatchDueEvents();

        assertThat(result.retried()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getNextAttemptAt()).isNotNull();
        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(meterRegistry
                .counter("evaluation.audience.outbox.dispatch.events", "result", "retried")
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("marks event dead when max attempts reached")
    void marksDeadWhenMaxAttemptsReached() throws Exception {
        IntegrationOutboxEventRepository repository = mock(IntegrationOutboxEventRepository.class);
        OutboxEventPublisher publisher = mock(OutboxEventPublisher.class);
        doThrow(new RuntimeException("permanent")).when(publisher).publish(any());

        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAudience().getOutbox().setMaxAttempts(2);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        IntegrationOutboxEventEntity event = event(3L, "PENDING", 1, null);
        when(repository.findDispatchCandidates(any(), any(), any(Pageable.class))).thenReturn(List.of(event));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxDispatchService service = new OutboxDispatchService(
                repository, publisher, new ObjectMapper(), properties, meterRegistry);
        OutboxDispatchService.DispatchResult result = service.dispatchDueEvents();

        assertThat(result.dead()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("DEAD");
        assertThat(event.getNextAttemptAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(meterRegistry
                .counter("evaluation.audience.outbox.dispatch.events", "result", "dead")
                .count()).isEqualTo(1.0d);
    }

    private IntegrationOutboxEventEntity event(long id, String status, int attemptCount, Instant nextAttemptAt) {
        IntegrationOutboxEventEntity event = new IntegrationOutboxEventEntity();
        event.setId(id);
        event.setAggregateType("AUDIENCE_MAPPING_PROFILE");
        event.setAggregateId("42");
        event.setEventType("AUDIENCE_MAPPING_PROFILE_UPDATED");
        event.setPayloadJson("{\"x\":1}");
        event.setStatus(status);
        event.setAttemptCount(attemptCount);
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(nextAttemptAt);
        return event;
    }
}
