package com.evaluationservice.api.dto.response;

/**
 * Report for legacy JSON to relational assignment backfill execution.
 */
public record AssignmentBackfillResponse(
        int scannedCampaigns,
        int parsedAssignments,
        int insertedAssignments,
        int skippedExistingAssignments,
        int invalidAssignments,
        boolean dryRun) {
}
