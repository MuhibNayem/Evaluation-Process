package com.evaluationservice.infrastructure.service;

import com.evaluationservice.domain.event.AudienceMappingProfileLifecycleEvent;
import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("MappingProfileLifecycleOutboxService")
class MappingProfileLifecycleOutboxServiceTest {

    @Test
    @DisplayName("stores lifecycle event in integration outbox")
    void storesLifecycleEventInOutbox() {
        IntegrationOutboxEventRepository repository = mock(IntegrationOutboxEventRepository.class);
        MappingProfileLifecycleOutboxService service =
                new MappingProfileLifecycleOutboxService(repository, new ObjectMapper());

        service.onLifecycleEvent(new AudienceMappingProfileLifecycleEvent(
                42L,
                "tenant-a",
                "UPDATED",
                "tester",
                Map.of("active", true),
                Instant.parse("2026-02-18T00:00:00Z")));

        ArgumentCaptor<IntegrationOutboxEventEntity> captor =
                ArgumentCaptor.forClass(IntegrationOutboxEventEntity.class);
        verify(repository).save(captor.capture());

        IntegrationOutboxEventEntity saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("AUDIENCE_MAPPING_PROFILE");
        assertThat(saved.getAggregateId()).isEqualTo("42");
        assertThat(saved.getEventType()).isEqualTo("AUDIENCE_MAPPING_PROFILE_UPDATED");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getPayloadJson()).contains("\"tenantId\":\"tenant-a\"");
    }
}
