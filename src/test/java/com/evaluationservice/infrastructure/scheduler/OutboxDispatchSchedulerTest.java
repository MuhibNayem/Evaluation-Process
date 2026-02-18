package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.OutboxDispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@DisplayName("OutboxDispatchScheduler")
class OutboxDispatchSchedulerTest {

    @Test
    @DisplayName("skips dispatch when outbox is disabled")
    void skipsWhenDisabled() {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAudience().getOutbox().setEnabled(false);
        OutboxDispatchService dispatchService = mock(OutboxDispatchService.class);

        OutboxDispatchScheduler scheduler = new OutboxDispatchScheduler(properties, dispatchService);
        scheduler.run();

        verifyNoInteractions(dispatchService);
    }

    @Test
    @DisplayName("runs dispatch when outbox is enabled")
    void runsWhenEnabled() {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        properties.getAudience().getOutbox().setEnabled(true);
        OutboxDispatchService dispatchService = mock(OutboxDispatchService.class);
        when(dispatchService.dispatchDueEvents()).thenReturn(new OutboxDispatchService.DispatchResult(0, 0, 0, 0));

        OutboxDispatchScheduler scheduler = new OutboxDispatchScheduler(properties, dispatchService);
        scheduler.run();

        verify(dispatchService).dispatchDueEvents();
    }
}
