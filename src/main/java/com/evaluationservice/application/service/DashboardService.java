package com.evaluationservice.application.service;

import com.evaluationservice.api.dto.response.DashboardStatsResponse;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.enums.CampaignStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final CampaignPersistencePort campaignPort;
    private final EvaluationPersistencePort evaluationPort;
    private final TemplatePersistencePort templatePort;

    public DashboardService(
            CampaignPersistencePort campaignPort,
            EvaluationPersistencePort evaluationPort,
            TemplatePersistencePort templatePort) {
        this.campaignPort = campaignPort;
        this.evaluationPort = evaluationPort;
        this.templatePort = templatePort;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalCampaigns = campaignPort.count();
        long activeCampaigns = campaignPort.countByStatus(CampaignStatus.ACTIVE);
        long totalTemplates = templatePort.count();

        long totalAssignments = campaignPort.countTotalAssignments();
        long completedAssignments = campaignPort.countCompletedAssignments();
        long pendingAssignments = totalAssignments - completedAssignments;

        double completionRate = 0.0;
        if (totalAssignments > 0) {
            completionRate = (double) completedAssignments / totalAssignments * 100.0;
        }

        List<DashboardStatsResponse.ActivityResponse> activity = buildRecentActivity();

        return new DashboardStatsResponse(
                totalCampaigns,
                activeCampaigns,
                totalTemplates,
                pendingAssignments,
                completedAssignments,
                completionRate,
                activity);
    }

    private List<DashboardStatsResponse.ActivityResponse> buildRecentActivity() {
        List<DashboardStatsResponse.ActivityResponse> campaignActivity = campaignPort.findRecentUpdated(5).stream()
                .map(c -> new DashboardStatsResponse.ActivityResponse(
                        "Campaign Updated",
                        c.getName() + " (" + c.getStatus().name() + ")",
                        c.getUpdatedAt().value(),
                        "CAMPAIGN"))
                .toList();

        List<DashboardStatsResponse.ActivityResponse> evaluationActivity = evaluationPort.findRecentUpdated(5).stream()
                .map(e -> new DashboardStatsResponse.ActivityResponse(
                        "Evaluation Updated",
                        "Assignment " + e.getAssignmentId() + " (" + e.getStatus().name() + ")",
                        e.getUpdatedAt().value(),
                        "EVALUATION"))
                .toList();

        return java.util.stream.Stream.concat(campaignActivity.stream(), evaluationActivity.stream())
                .sorted(Comparator.comparing(DashboardStatsResponse.ActivityResponse::timestamp).reversed())
                .limit(10)
                .toList();
    }
}
