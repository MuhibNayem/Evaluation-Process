package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.ScoringPreviewRequest;
import com.evaluationservice.api.dto.response.ScoringPreviewResponse;
import com.evaluationservice.infrastructure.service.ScoringPreviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringPreviewController {

    private final ScoringPreviewService scoringPreviewService;

    public ScoringPreviewController(ScoringPreviewService scoringPreviewService) {
        this.scoringPreviewService = scoringPreviewService;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScoringPreviewResponse> preview(@Valid @RequestBody ScoringPreviewRequest request) {
        return ResponseEntity.ok(scoringPreviewService.preview(request));
    }
}
