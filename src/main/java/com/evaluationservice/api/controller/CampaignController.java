package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.AddAssignmentsRequest;
import com.evaluationservice.api.dto.request.CreateCampaignRequest;
import com.evaluationservice.api.dto.request.GenerateDynamicAssignmentsRequest;
import com.evaluationservice.api.dto.response.AssignmentParityReportResponse;
import com.evaluationservice.api.dto.response.AssignmentReconciliationResponse;
import com.evaluationservice.api.dto.response.AssignmentBackfillResponse;
import com.evaluationservice.api.dto.response.CampaignResponse;
import com.evaluationservice.api.dto.response.DynamicAssignmentResponse;
import com.evaluationservice.api.mapper.ResponseMapper;
import com.evaluationservice.application.port.in.CampaignManagementUseCase;
import com.evaluationservice.application.port.in.CampaignManagementUseCase.AssignmentEntry;
import com.evaluationservice.application.port.in.CampaignManagementUseCase.CreateCampaignCommand;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.application.service.SettingsResolverService;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.infrastructure.security.SecurityContextUserProvider;
import com.evaluationservice.infrastructure.service.AssignmentParityReportService;
import com.evaluationservice.infrastructure.service.AssignmentReconciliationService;
import com.evaluationservice.infrastructure.service.AssignmentBackfillService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for evaluation campaign management.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignManagementUseCase campaignUseCase;
    private final ResponseMapper responseMapper;
    private final SecurityContextUserProvider userProvider;
    private final SettingsResolverService settingsResolver;
    private final AssignmentReconciliationService assignmentReconciliationService;
    private final AssignmentParityReportService assignmentParityReportService;
    private final AssignmentBackfillService assignmentBackfillService;

    public CampaignController(
            CampaignManagementUseCase campaignUseCase,
            ResponseMapper responseMapper,
            SecurityContextUserProvider userProvider,
            SettingsResolverService settingsResolver,
            AssignmentReconciliationService assignmentReconciliationService,
            AssignmentParityReportService assignmentParityReportService,
            AssignmentBackfillService assignmentBackfillService) {
        this.campaignUseCase = campaignUseCase;
        this.responseMapper = responseMapper;
        this.userProvider = userProvider;
        this.settingsResolver = settingsResolver;
        this.assignmentReconciliationService = assignmentReconciliationService;
        this.assignmentParityReportService = assignmentParityReportService;
        this.assignmentBackfillService = assignmentBackfillService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        var command = new CreateCampaignCommand(
                request.name(),
                request.description(),
                TemplateId.of(request.templateId()),
                request.templateVersion(),
                DateRange.of(request.startDate(), request.endDate()),
                request.scoringMethod() != null ? request.scoringMethod()
                        : ScoringMethod.valueOf(settingsResolver.resolve("scoring.default-method")),
                request.anonymousMode(),
                request.anonymousRoles(),
                request.audienceSourceType(),
                request.audienceSourceConfig(),
                request.assignmentRuleType(),
                request.assignmentRuleConfig(),
                request.minimumRespondents() > 0 ? request.minimumRespondents()
                        : settingsResolver.resolveInt("campaign.default-minimum-respondents"),
                userProvider.getCurrentUserId());
        Campaign campaign = campaignUseCase.createCampaign(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(campaign));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable String id) {
        Campaign campaign = campaignUseCase.getCampaign(CampaignId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable String id,
            @Valid @RequestBody com.evaluationservice.api.dto.request.UpdateCampaignRequest request) {
        var command = new com.evaluationservice.application.port.in.CampaignManagementUseCase.UpdateCampaignCommand(
                CampaignId.of(id),
                request.name(),
                request.description(),
                DateRange.of(request.startDate(), request.endDate()),
                request.scoringMethod(),
                request.anonymousMode() != null ? request.anonymousMode() : false,
                request.anonymousRoles(),
                request.audienceSourceType(),
                request.audienceSourceConfig(),
                request.assignmentRuleType(),
                request.assignmentRuleConfig(),
                request.minimumRespondents() != null ? request.minimumRespondents() : 0);
        Campaign campaign = campaignUseCase.updateCampaign(command);
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int pageSize = resolvePageSize(size);
        List<CampaignResponse> campaigns = campaignUseCase.listCampaigns(status, page, pageSize)
                .stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<CampaignResponse> activateCampaign(@PathVariable String id) {
        Campaign campaign = campaignUseCase.activateCampaign(CampaignId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<CampaignResponse> closeCampaign(@PathVariable String id) {
        Campaign campaign = campaignUseCase.closeCampaign(CampaignId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<CampaignResponse> archiveCampaign(@PathVariable String id) {
        Campaign campaign = campaignUseCase.archiveCampaign(CampaignId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @PostMapping("/{id}/assignments")
    public ResponseEntity<CampaignResponse> addAssignments(
            @PathVariable String id,
            @Valid @RequestBody AddAssignmentsRequest request) {
        List<AssignmentEntry> entries = request.assignments().stream()
                .map(a -> new AssignmentEntry(a.evaluatorId(), a.evaluateeId(), a.evaluatorRole()))
                .toList();
        Campaign campaign = campaignUseCase.addAssignments(CampaignId.of(id), entries);
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @PostMapping("/{id}/assignments/dynamic")
    public ResponseEntity<DynamicAssignmentResponse> generateDynamicAssignments(
            @PathVariable String id,
            @Valid @RequestBody GenerateDynamicAssignmentsRequest request) {
        var result = campaignUseCase.generateDynamicAssignments(
                CampaignId.of(id),
                new com.evaluationservice.application.port.in.CampaignManagementUseCase.DynamicAssignmentCommand(
                        request.audienceSourceType(),
                        request.audienceSourceConfig(),
                        request.assignmentRuleType(),
                        request.assignmentRuleConfig(),
                        request.replaceExistingAssignments(),
                        request.dryRun()));

        List<DynamicAssignmentResponse.GeneratedAssignmentItem> generatedItems = result.generatedAssignments().stream()
                .map(a -> new DynamicAssignmentResponse.GeneratedAssignmentItem(
                        a.getId(),
                        a.getEvaluatorId(),
                        a.getEvaluateeId(),
                        a.getEvaluatorRole()))
                .toList();

        return ResponseEntity.ok(new DynamicAssignmentResponse(
                result.campaign().getId().value(),
                result.audienceSourceType(),
                result.assignmentRuleType(),
                result.replaceExistingAssignments(),
                result.dryRun(),
                generatedItems.size(),
                generatedItems));
    }

    @PostMapping("/{id}/extend-deadline")
    public ResponseEntity<CampaignResponse> extendDeadline(
            @PathVariable String id,
            @RequestBody Map<String, Instant> body) {
        Instant newEndDate = body.get("newEndDate");
        Campaign campaign = campaignUseCase.extendDeadline(CampaignId.of(id), newEndDate);
        return ResponseEntity.ok(responseMapper.toResponse(campaign));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<Map<String, Double>> getCampaignProgress(@PathVariable String id) {
        double progress = campaignUseCase.getCampaignProgress(CampaignId.of(id));
        return ResponseEntity.ok(Map.of("completionPercentage", progress));
    }

    @GetMapping("/assignments/me")
    public ResponseEntity<List<com.evaluationservice.api.dto.response.MyAssignmentResponse>> getMyAssignments() {
        String userId = userProvider.getCurrentUserId();
        List<Campaign> campaigns = campaignUseCase.listCampaignsForEvaluator(userId);

        List<com.evaluationservice.api.dto.response.MyAssignmentResponse> response = campaigns.stream()
                .flatMap(c -> c.getAssignments().stream()
                        .filter(a -> a.getEvaluatorId().equals(userId))
                        // Only include incomplete assignments or those with evaluationId?
                        // Let's include everything for now.
                        .map(a -> new com.evaluationservice.api.dto.response.MyAssignmentResponse(
                                a.getId(),
                                c.getId().value(),
                                c.getName(),
                                c.getDateRange().endDate(),
                                a.getEvaluateeId(),
                                a.isCompleted() ? "COMPLETED" : "PENDING",
                                a.getEvaluationId())))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/assignments/reconcile")
    public ResponseEntity<AssignmentReconciliationResponse> reconcileAssignments(@PathVariable String id) {
        return ResponseEntity.ok(assignmentReconciliationService.reconcile(id));
    }

    @GetMapping("/assignments/reconcile/report")
    public ResponseEntity<AssignmentParityReportResponse> reconcileAssignmentsReport(
            @RequestParam(defaultValue = "1000") int maxCampaigns) {
        return ResponseEntity.ok(assignmentParityReportService.buildReport(maxCampaigns));
    }

    @PostMapping("/assignments/backfill")
    public ResponseEntity<AssignmentBackfillResponse> backfillAssignments(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "1000") int maxCampaigns) {
        return ResponseEntity.ok(assignmentBackfillService.backfill(dryRun, maxCampaigns));
    }

    private int resolvePageSize(Integer requestedSize) {
        if (requestedSize == null) {
            return settingsResolver.resolveInt("pagination.default-page-size");
        }
        return Math.min(requestedSize, settingsResolver.resolveInt("pagination.max-page-size"));
    }
}
