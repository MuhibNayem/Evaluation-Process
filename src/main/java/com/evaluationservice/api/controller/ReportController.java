package com.evaluationservice.api.controller;

import com.evaluationservice.application.port.in.ReportGenerationUseCase;
import com.evaluationservice.application.port.in.ReportGenerationUseCase.CampaignReportResult;
import com.evaluationservice.application.port.in.ReportGenerationUseCase.IndividualReportResult;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for report generation and export.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportGenerationUseCase reportUseCase;
    private final EvaluationServiceProperties properties;

    public ReportController(ReportGenerationUseCase reportUseCase,
            EvaluationServiceProperties properties) {
        this.reportUseCase = reportUseCase;
        this.properties = properties;
    }

    @GetMapping("/individual")
    public ResponseEntity<IndividualReportResult> getIndividualReport(
            @RequestParam String evaluateeId,
            @RequestParam String campaignId) {
        if (!properties.getFeatures().isEnableReports()) {
            return ResponseEntity.status(403).build();
        }
        IndividualReportResult report = reportUseCase.generateIndividualReport(
                evaluateeId, CampaignId.of(campaignId));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<CampaignReportResult> getCampaignReport(@PathVariable String campaignId) {
        if (!properties.getFeatures().isEnableReports()) {
            return ResponseEntity.status(403).build();
        }
        CampaignReportResult report = reportUseCase.generateCampaignReport(CampaignId.of(campaignId));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export/csv/{campaignId}")
    public ResponseEntity<byte[]> exportCampaignCsv(@PathVariable String campaignId) {
        if (!properties.getFeatures().isEnableCsvExport()) {
            return ResponseEntity.status(403).build();
        }
        byte[] csv = reportUseCase.exportReportAsCsv(CampaignId.of(campaignId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=campaign-" + campaignId + "-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportIndividualPdf(
            @RequestParam String evaluateeId,
            @RequestParam String campaignId) {
        if (!properties.getFeatures().isEnablePdfExport()) {
            return ResponseEntity.status(403).build();
        }
        byte[] pdf = reportUseCase.exportReportAsPdf(evaluateeId, CampaignId.of(campaignId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report-" + evaluateeId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
