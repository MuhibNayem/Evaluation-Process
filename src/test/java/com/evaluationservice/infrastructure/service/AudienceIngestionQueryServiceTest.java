package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.entity.AudienceIngestionRejectionEntity;
import com.evaluationservice.infrastructure.entity.AudienceIngestionRunEntity;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRejectionRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AudienceIngestionQueryService")
class AudienceIngestionQueryServiceTest {

    @Test
    @DisplayName("lists runs filtered by tenant")
    void listsRunsByTenant() {
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);

        AudienceIngestionRunEntity run = new AudienceIngestionRunEntity();
        run.setId("ing-1");
        run.setTenantId("tenant-a");
        run.setSourceType("CSV");
        run.setStatus("SUCCEEDED");
        run.setStartedAt(Instant.parse("2026-02-18T10:00:00Z"));

        when(runRepository.findByTenantIdOrderByStartedAtDesc(eq("tenant-a"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(run)));

        AudienceIngestionQueryService service = new AudienceIngestionQueryService(runRepository, rejectionRepository);

        var runs = service.listRuns("tenant-a", 10);

        assertThat(runs).hasSize(1);
        assertThat(runs.getFirst().id()).isEqualTo("ing-1");
        verify(runRepository, never()).findAllByOrderByStartedAtDesc(any(PageRequest.class));
    }

    @Test
    @DisplayName("lists rejections sorted by row")
    void listsRejections() {
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);

        AudienceIngestionRejectionEntity rejection = new AudienceIngestionRejectionEntity();
        rejection.setId(11L);
        rejection.setRunId("ing-1");
        rejection.setTenantId("tenant-a");
        rejection.setRowNumber(4);
        rejection.setReason("Missing person_id");
        rejection.setCreatedAt(Instant.parse("2026-02-18T10:00:00Z"));

        when(rejectionRepository.findByRunIdOrderByRowNumberAsc(eq("ing-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(rejection)));

        AudienceIngestionQueryService service = new AudienceIngestionQueryService(runRepository, rejectionRepository);

        var rejections = service.listRejections("ing-1", 100);

        assertThat(rejections).hasSize(1);
        assertThat(rejections.getFirst().id()).isEqualTo(11L);
        assertThat(rejections.getFirst().rowNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("returns run details by id")
    void getRun() {
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);

        AudienceIngestionRunEntity run = new AudienceIngestionRunEntity();
        run.setId("ing-2");
        run.setTenantId("tenant-b");
        run.setSourceType("CSV");
        run.setStatus("FAILED");
        run.setErrorMessage("boom");
        run.setStartedAt(Instant.parse("2026-02-18T10:00:00Z"));
        run.setEndedAt(Instant.parse("2026-02-18T10:01:00Z"));

        when(runRepository.findById("ing-2")).thenReturn(Optional.of(run));

        AudienceIngestionQueryService service = new AudienceIngestionQueryService(runRepository, rejectionRepository);
        var result = service.getRun("ing-2");

        assertThat(result.id()).isEqualTo("ing-2");
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).isEqualTo("boom");
    }
}
