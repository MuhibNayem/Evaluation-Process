package com.evaluationservice.application.port.in;

import com.evaluationservice.domain.value.CampaignId;

import java.util.Map;

/**
 * Inbound port for report generation operations.
 */
public interface ReportGenerationUseCase {

    record IndividualReportResult(
            String evaluateeId,
            String evaluateeName,
            CampaignId campaignId,
            double overallScore,
            Map<String, Double> sectionScores,
            Map<String, String> strengths,
            Map<String, String> weaknesses,
            int totalEvaluations,
            int respondentCount) {
    }

    record CampaignReportResult(
            CampaignId campaignId,
            String campaignName,
            double averageScore,
            double completionPercentage,
            int totalAssignments,
            int completedAssignments,
            Map<String, Double> sectionAverages) {
    }

    IndividualReportResult generateIndividualReport(String evaluateeId, CampaignId campaignId);

    CampaignReportResult generateCampaignReport(CampaignId campaignId);

    byte[] exportReportAsPdf(String evaluateeId, CampaignId campaignId);

    byte[] exportReportAsCsv(CampaignId campaignId);
}
