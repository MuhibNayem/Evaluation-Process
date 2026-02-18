package com.evaluationservice.api.dto.response;

import java.util.List;

/**
 * Response for assignment storage reconciliation checks.
 * Compares legacy campaign JSON assignments with relational assignments table.
 */
public record AssignmentReconciliationResponse(
        String campaignId,
        int legacyCount,
        int relationalCount,
        int onlyInLegacyCount,
        int onlyInRelationalCount,
        int completionMismatchCount,
        boolean consistent,
        List<String> onlyInLegacy,
        List<String> onlyInRelational,
        List<String> completionMismatches) {
}
