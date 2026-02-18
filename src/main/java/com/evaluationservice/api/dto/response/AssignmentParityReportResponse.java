package com.evaluationservice.api.dto.response;

import java.util.List;

/**
 * Aggregated parity report across campaigns for legacy JSON vs relational assignments.
 */
public record AssignmentParityReportResponse(
        int scannedCampaigns,
        int consistentCampaigns,
        int inconsistentCampaigns,
        int totalOnlyInLegacy,
        int totalOnlyInRelational,
        int totalCompletionMismatches,
        List<String> inconsistentCampaignIds) {
}
