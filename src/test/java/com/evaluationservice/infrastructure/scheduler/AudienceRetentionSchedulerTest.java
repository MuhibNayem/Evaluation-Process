package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.AudienceRetentionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@DisplayName("AudienceRetentionScheduler")
class AudienceRetentionSchedulerTest {

    @Test
    @DisplayName("skips cleanup when retention is disabled")
    void skipsWhenDisabled() {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getRetention().setEnabled(false);
        AudienceRetentionService retentionService = mock(AudienceRetentionService.class);

        AudienceRetentionScheduler scheduler = new AudienceRetentionScheduler(props, retentionService);
        scheduler.run();

        verifyNoInteractions(retentionService);
    }

    @Test
    @DisplayName("runs cleanup when retention is enabled")
    void runsWhenEnabled() {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAudience().getRetention().setEnabled(true);
        AudienceRetentionService retentionService = mock(AudienceRetentionService.class);
        when(retentionService.cleanup()).thenReturn(new AudienceRetentionService.CleanupResult(0, 0, 0, 0));

        AudienceRetentionScheduler scheduler = new AudienceRetentionScheduler(props, retentionService);
        scheduler.run();

        verify(retentionService).cleanup();
    }
}
