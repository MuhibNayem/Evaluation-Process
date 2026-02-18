package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AssignmentParityReportResponse;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds an aggregated reconciliation report across campaigns.
 */
@Service
public class AssignmentParityReportService {

    private final CampaignPersistencePort campaignPersistencePort;
    private final AssignmentReconciliationService reconciliationService;

    public AssignmentParityReportService(
            CampaignPersistencePort campaignPersistencePort,
            AssignmentReconciliationService reconciliationService) {
        this.campaignPersistencePort = campaignPersistencePort;
        this.reconciliationService = reconciliationService;
    }

    public AssignmentParityReportResponse buildReport(int maxCampaigns) {
        int limit = Math.max(maxCampaigns, 1);
        int page = 0;
        int pageSize = 100;

        int scanned = 0;
        int consistent = 0;
        int inconsistent = 0;
        int onlyInLegacy = 0;
        int onlyInRelational = 0;
        int completionMismatches = 0;
        List<String> inconsistentCampaignIds = new ArrayList<>();

        while (scanned < limit) {
            var campaigns = campaignPersistencePort.findAll(page, pageSize);
            if (campaigns.isEmpty()) {
                break;
            }

            for (var campaign : campaigns) {
                if (scanned >= limit) {
                    break;
                }
                var result = reconciliationService.reconcile(campaign.getId().value());
                scanned++;
                onlyInLegacy += result.onlyInLegacyCount();
                onlyInRelational += result.onlyInRelationalCount();
                completionMismatches += result.completionMismatchCount();
                if (result.consistent()) {
                    consistent++;
                } else {
                    inconsistent++;
                    inconsistentCampaignIds.add(result.campaignId());
                }
            }

            if (campaigns.size() < pageSize) {
                break;
            }
            page++;
        }

        return new AssignmentParityReportResponse(
                scanned,
                consistent,
                inconsistent,
                onlyInLegacy,
                onlyInRelational,
                completionMismatches,
                List.copyOf(inconsistentCampaignIds));
    }
}
