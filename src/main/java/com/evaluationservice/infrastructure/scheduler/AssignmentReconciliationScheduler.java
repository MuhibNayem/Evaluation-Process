package com.evaluationservice.infrastructure.scheduler;

import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.service.AssignmentReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodic reconciliation scanner for legacy JSON vs relational assignments.
 */
@Component
public class AssignmentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentReconciliationScheduler.class);

    private final CampaignPersistencePort campaignPersistencePort;
    private final AssignmentReconciliationService reconciliationService;
    private final EvaluationServiceProperties.Assignment assignmentConfig;

    public AssignmentReconciliationScheduler(
            CampaignPersistencePort campaignPersistencePort,
            AssignmentReconciliationService reconciliationService,
            EvaluationServiceProperties properties) {
        this.campaignPersistencePort = campaignPersistencePort;
        this.reconciliationService = reconciliationService;
        this.assignmentConfig = properties.getAssignment();
    }

    @Scheduled(cron = "${evaluation.service.assignment.reconciliation-cron:0 */30 * * * *}")
    public void run() {
        if (!assignmentConfig.isReconciliationEnabled()) {
            return;
        }

        int maxCampaigns = Math.max(assignmentConfig.getReconciliationMaxCampaigns(), 1);
        AtomicInteger scanned = new AtomicInteger();
        AtomicInteger inconsistent = new AtomicInteger();

        int page = 0;
        int size = 100;
        while (scanned.get() < maxCampaigns) {
            var campaigns = campaignPersistencePort.findAll(page, size);
            if (campaigns.isEmpty()) {
                break;
            }

            for (var campaign : campaigns) {
                if (scanned.incrementAndGet() > maxCampaigns) {
                    break;
                }
                var result = reconciliationService.reconcile(campaign.getId().value());
                if (!result.consistent()) {
                    inconsistent.incrementAndGet();
                    log.warn("Assignment reconciliation mismatch for campaign {}: onlyInLegacy={}, onlyInRelational={}, completionMismatches={}",
                            result.campaignId(),
                            result.onlyInLegacyCount(),
                            result.onlyInRelationalCount(),
                            result.completionMismatchCount());
                }
            }

            if (campaigns.size() < size) {
                break;
            }
            page++;
        }

        if (scanned.get() > 0) {
            log.info("Assignment reconciliation run finished. scanned={}, inconsistent={}", scanned.get(), inconsistent.get());
        }
    }
}
