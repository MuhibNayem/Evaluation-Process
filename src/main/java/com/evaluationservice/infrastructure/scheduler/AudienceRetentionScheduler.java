package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.AudienceRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AudienceRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AudienceRetentionScheduler.class);

    private final EvaluationServiceProperties.Retention retentionConfig;
    private final AudienceRetentionService retentionService;

    public AudienceRetentionScheduler(
            EvaluationServiceProperties properties,
            AudienceRetentionService retentionService) {
        this.retentionConfig = properties.getAudience().getRetention();
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${evaluation.service.audience.retention.cron:0 0 3 * * *}")
    public void run() {
        if (!retentionConfig.isEnabled()) {
            return;
        }

        AudienceRetentionService.CleanupResult result = retentionService.cleanup();
        long totalDeleted = result.snapshotsDeleted()
                + result.mappingEventsDeleted()
                + result.outboxPublishedDeleted()
                + result.outboxFailedDeleted();
        if (totalDeleted > 0) {
            log.info(
                    "Audience retention cleanup completed. snapshotsDeleted={}, mappingEventsDeleted={}, outboxPublishedDeleted={}, outboxFailedDeleted={}",
                    result.snapshotsDeleted(),
                    result.mappingEventsDeleted(),
                    result.outboxPublishedDeleted(),
                    result.outboxFailedDeleted());
        }
    }
}
