package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.ReportGenerationUseCase;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application service implementing report generation use cases.
 * Aggregates evaluation data to produce individual and campaign-level reports.
 */
@Service
@Transactional(readOnly = true)
public class ReportGenerationService implements ReportGenerationUseCase {

    private final EvaluationPersistencePort evaluationPersistencePort;
    private final CampaignPersistencePort campaignPersistencePort;

    public ReportGenerationService(
            EvaluationPersistencePort evaluationPersistencePort,
            CampaignPersistencePort campaignPersistencePort) {
        this.evaluationPersistencePort = Objects.requireNonNull(evaluationPersistencePort);
        this.campaignPersistencePort = Objects.requireNonNull(campaignPersistencePort);
    }

    @Override
    public IndividualReportResult generateIndividualReport(String evaluateeId, CampaignId campaignId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        List<Evaluation> evaluations = evaluationPersistencePort
                .findByCampaignId(campaignId, 0, Integer.MAX_VALUE)
                .stream()
                .filter(e -> evaluateeId.equals(e.getEvaluateeId()))
                .toList();

        double overallScore = evaluations.stream()
                .filter(e -> e.getTotalScore() != null)
                .mapToDouble(e -> e.getTotalScore().value().doubleValue())
                .average()
                .orElse(0.0);

        Map<String, Double> sectionScores = aggregateSectionScores(evaluations);
        Map<String, String> strengths = identifyTopSections(sectionScores, true);
        Map<String, String> weaknesses = identifyTopSections(sectionScores, false);

        return new IndividualReportResult(
                evaluateeId,
                evaluateeId, // name resolution would require a user service
                campaignId,
                round(overallScore),
                sectionScores,
                strengths,
                weaknesses,
                evaluations.size(),
                evaluations.size());
    }

    @Override
    public CampaignReportResult generateCampaignReport(CampaignId campaignId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        List<Evaluation> evaluations = evaluationPersistencePort
                .findByCampaignId(campaignId, 0, Integer.MAX_VALUE);

        int totalAssignments = campaign.getAssignments().size();
        int completedAssignments = (int) evaluations.stream()
                .filter(Evaluation::isCompleted)
                .count();

        double averageScore = evaluations.stream()
                .filter(e -> e.getTotalScore() != null)
                .mapToDouble(e -> e.getTotalScore().value().doubleValue())
                .average()
                .orElse(0.0);

        double completionPercentage = totalAssignments > 0
                ? (double) completedAssignments / totalAssignments * 100.0
                : 0.0;

        Map<String, Double> sectionAverages = aggregateSectionScores(evaluations);

        return new CampaignReportResult(
                campaignId,
                campaign.getName(),
                round(averageScore),
                round(completionPercentage),
                totalAssignments,
                completedAssignments,
                sectionAverages);
    }

    @Override
    public byte[] exportReportAsPdf(String evaluateeId, CampaignId campaignId) {
        // PDF generation would require a library like iText or Apache PDFBox
        // For now, return a text-based representation
        IndividualReportResult report = generateIndividualReport(evaluateeId, campaignId);
        String content = formatReportAsText(report);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportReportAsCsv(CampaignId campaignId) {
        CampaignReportResult report = generateCampaignReport(campaignId);

        StringBuilder csv = new StringBuilder();
        csv.append("Campaign,Average Score,Completion %,Total Assignments,Completed\n");
        csv.append(String.format("%s,%.2f,%.2f,%d,%d\n",
                report.campaignName(),
                report.averageScore(),
                report.completionPercentage(),
                report.totalAssignments(),
                report.completedAssignments()));

        if (!report.sectionAverages().isEmpty()) {
            csv.append("\nSection,Average Score\n");
            report.sectionAverages()
                    .forEach((section, score) -> csv.append(String.format("%s,%.2f\n", section, score)));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- Helpers ---

    private Campaign findCampaignOrThrow(CampaignId campaignId) {
        return campaignPersistencePort.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign", campaignId.value()));
    }

    private Map<String, Double> aggregateSectionScores(List<Evaluation> evaluations) {
        Map<String, List<Double>> sectionScoresMap = new HashMap<>();

        for (Evaluation evaluation : evaluations) {
            if (evaluation.getSectionScores() != null) {
                evaluation.getSectionScores().forEach(ss -> {
                    sectionScoresMap
                            .computeIfAbsent(ss.sectionTitle(), k -> new java.util.ArrayList<>())
                            .add(ss.score().value().doubleValue());
                });
            }
        }

        Map<String, Double> averages = new HashMap<>();
        sectionScoresMap.forEach((section, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            averages.put(section, round(avg));
        });
        return averages;
    }

    private Map<String, String> identifyTopSections(Map<String, Double> sectionScores, boolean highest) {
        Map<String, String> result = new HashMap<>();
        sectionScores.entrySet().stream()
                .sorted(highest
                        ? Map.Entry.<String, Double>comparingByValue().reversed()
                        : Map.Entry.comparingByValue())
                .limit(3)
                .forEach(e -> result.put(e.getKey(),
                        String.format("Score: %.2f", e.getValue())));
        return result;
    }

    private String formatReportAsText(IndividualReportResult report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Individual Evaluation Report\n");
        sb.append("===========================\n");
        sb.append("Evaluatee: ").append(report.evaluateeId()).append("\n");
        sb.append("Campaign: ").append(report.campaignId().value()).append("\n");
        sb.append("Overall Score: ").append(String.format("%.2f", report.overallScore())).append("\n");
        sb.append("Total Evaluations: ").append(report.totalEvaluations()).append("\n\n");
        sb.append("Section Scores:\n");
        report.sectionScores().forEach((section, score) -> sb.append("  ").append(section).append(": ")
                .append(String.format("%.2f", score)).append("\n"));
        return sb.toString();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
