package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.OutboxDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchScheduler.class);

    private final EvaluationServiceProperties.Outbox outboxConfig;
    private final OutboxDispatchService dispatchService;

    public OutboxDispatchScheduler(
            EvaluationServiceProperties properties,
            OutboxDispatchService dispatchService) {
        this.outboxConfig = properties.getAudience().getOutbox();
        this.dispatchService = dispatchService;
    }

    @Scheduled(cron = "${evaluation.service.audience.outbox.cron:0 */1 * * * *}")
    public void run() {
        if (!outboxConfig.isEnabled()) {
            return;
        }
        OutboxDispatchService.DispatchResult result = dispatchService.dispatchDueEvents();
        if (result.scanned() > 0) {
            log.info("Outbox dispatch run finished. scanned={}, published={}, retried={}, dead={}",
                    result.scanned(),
                    result.published(),
                    result.retried(),
                    result.dead());
            if (result.dead() > 0) {
                log.warn("Outbox dispatcher moved events to DEAD state. dead={}", result.dead());
            }
        }
    }
}
