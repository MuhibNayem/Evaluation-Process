package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateAudienceMappingProfileRequest;
import com.evaluationservice.api.dto.request.DeactivateAudienceMappingProfileRequest;
import com.evaluationservice.api.dto.request.IngestAudienceRequest;
import com.evaluationservice.api.dto.request.ReplayAudienceIngestionRequest;
import com.evaluationservice.api.dto.request.UpdateAudienceMappingProfileRequest;
import com.evaluationservice.api.dto.request.ValidateAudienceMappingProfileRequest;
import com.evaluationservice.api.dto.response.AudienceIngestionResponse;
import com.evaluationservice.api.dto.response.AudienceIngestionRejectionResponse;
import com.evaluationservice.api.dto.response.AudienceIngestionRunResponse;
import com.evaluationservice.api.dto.response.AudienceMappingProfileEventResponse;
import com.evaluationservice.api.dto.response.AudienceMappingProfileResponse;
import com.evaluationservice.api.dto.response.AudienceMappingValidationResponse;
import com.evaluationservice.application.port.in.AudienceIngestionUseCase;
import com.evaluationservice.infrastructure.service.AudienceMappingProfileService;
import com.evaluationservice.infrastructure.service.AudienceIngestionQueryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audience")
public class AudienceController {

    private final AudienceIngestionUseCase audienceIngestionUseCase;
    private final AudienceIngestionQueryService audienceIngestionQueryService;
    private final AudienceMappingProfileService audienceMappingProfileService;

    public AudienceController(
            AudienceIngestionUseCase audienceIngestionUseCase,
            AudienceIngestionQueryService audienceIngestionQueryService,
            AudienceMappingProfileService audienceMappingProfileService) {
        this.audienceIngestionUseCase = audienceIngestionUseCase;
        this.audienceIngestionQueryService = audienceIngestionQueryService;
        this.audienceMappingProfileService = audienceMappingProfileService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<AudienceIngestionResponse> ingest(@Valid @RequestBody IngestAudienceRequest request) {
        var result = audienceIngestionUseCase.ingest(new AudienceIngestionUseCase.IngestionRequest(
                request.tenantId(),
                request.sourceType(),
                request.sourceConfig(),
                request.mappingProfileId(),
                request.dryRun()));

        return ResponseEntity.ok(new AudienceIngestionResponse(
                result.tenantId(),
                result.runId(),
                result.dryRun(),
                result.processedRecords(),
                result.rejectedRecords()));
    }

    @GetMapping("/ingestion-runs")
    public ResponseEntity<List<AudienceIngestionRunResponse>> listRuns(
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(audienceIngestionQueryService.listRuns(tenantId, limit));
    }

    @GetMapping("/ingestion-runs/{runId}")
    public ResponseEntity<AudienceIngestionRunResponse> getRun(@PathVariable String runId) {
        return ResponseEntity.ok(audienceIngestionQueryService.getRun(runId));
    }

    @GetMapping("/ingestion-runs/{runId}/rejections")
    public ResponseEntity<List<AudienceIngestionRejectionResponse>> listRejections(
            @PathVariable String runId,
            @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(audienceIngestionQueryService.listRejections(runId, limit));
    }

    @PostMapping("/ingestion-runs/{runId}/replay")
    public ResponseEntity<AudienceIngestionResponse> replayIngestionRun(
            @PathVariable String runId,
            @RequestBody(required = false) ReplayAudienceIngestionRequest request) {
        var result = audienceIngestionUseCase.replay(
                runId,
                request == null ? null : request.dryRun());

        return ResponseEntity.ok(new AudienceIngestionResponse(
                result.tenantId(),
                result.runId(),
                result.dryRun(),
                result.processedRecords(),
                result.rejectedRecords()));
    }

    @PostMapping("/mapping-profiles")
    public ResponseEntity<AudienceMappingProfileResponse> createMappingProfile(
            @Valid @RequestBody CreateAudienceMappingProfileRequest request) {
        AudienceMappingProfileResponse response = audienceMappingProfileService.create(
                request.tenantId(),
                request.name(),
                request.sourceType(),
                request.fieldMappings(),
                request.active() == null || request.active(),
                currentActor());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/mapping-profiles/{profileId}")
    public ResponseEntity<AudienceMappingProfileResponse> updateMappingProfile(
            @PathVariable long profileId,
            @Valid @RequestBody UpdateAudienceMappingProfileRequest request) {
        AudienceMappingProfileResponse response = audienceMappingProfileService.update(
                request.tenantId(),
                profileId,
                request.name(),
                request.fieldMappings(),
                request.active() == null || request.active(),
                currentActor());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mapping-profiles/{profileId}/deactivate")
    public ResponseEntity<AudienceMappingProfileResponse> deactivateMappingProfile(
            @PathVariable long profileId,
            @Valid @RequestBody DeactivateAudienceMappingProfileRequest request) {
        return ResponseEntity.ok(audienceMappingProfileService.deactivate(
                request.tenantId(),
                profileId,
                currentActor()));
    }

    @GetMapping("/mapping-profiles")
    public ResponseEntity<List<AudienceMappingProfileResponse>> listMappingProfiles(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(audienceMappingProfileService.listByTenant(tenantId));
    }

    @GetMapping("/mapping-profiles/{profileId}")
    public ResponseEntity<AudienceMappingProfileResponse> getMappingProfile(
            @PathVariable long profileId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(audienceMappingProfileService.getById(tenantId, profileId));
    }

    @GetMapping("/mapping-profiles/{profileId}/events")
    public ResponseEntity<List<AudienceMappingProfileEventResponse>> listMappingProfileEvents(
            @PathVariable long profileId,
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(audienceMappingProfileService.listEvents(tenantId, profileId, limit));
    }

    @PostMapping("/mapping-profiles/validate")
    public ResponseEntity<AudienceMappingValidationResponse> validateMappingProfile(
            @Valid @RequestBody ValidateAudienceMappingProfileRequest request) {
        var normalized = audienceMappingProfileService.normalizeAndValidate(
                request.sourceType(),
                request.fieldMappings());
        return ResponseEntity.ok(new AudienceMappingValidationResponse(
                request.sourceType().trim().toUpperCase(),
                normalized,
                true));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "system";
        }
        return authentication.getName();
    }
}
