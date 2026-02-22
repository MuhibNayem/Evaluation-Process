package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateQuestionBankItemRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankItemVersionRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankSetRequest;
import com.evaluationservice.api.dto.response.QuestionBankItemResponse;
import com.evaluationservice.api.dto.response.QuestionBankItemVersionResponse;
import com.evaluationservice.api.dto.response.QuestionBankSetResponse;
import com.evaluationservice.api.dto.response.QuestionVersionCompareResponse;
import com.evaluationservice.application.service.SettingsResolverService;
import com.evaluationservice.infrastructure.service.QuestionBankService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions-bank")
@PreAuthorize("hasRole('ADMIN')")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final SettingsResolverService settingsResolver;

    public QuestionBankController(
            QuestionBankService questionBankService,
            SettingsResolverService settingsResolver) {
        this.questionBankService = questionBankService;
        this.settingsResolver = settingsResolver;
    }

    @PostMapping("/sets")
    public ResponseEntity<QuestionBankSetResponse> createSet(@Valid @RequestBody CreateQuestionBankSetRequest request) {
        ensureEnabled();
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createSet(request));
    }

    @GetMapping("/sets")
    public ResponseEntity<List<QuestionBankSetResponse>> listSets(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status) {
        ensureEnabled();
        return ResponseEntity.ok(questionBankService.listSets(tenantId, status));
    }

    @PostMapping("/sets/{setId}/items")
    public ResponseEntity<QuestionBankItemResponse> createItem(
            @PathVariable Long setId,
            @Valid @RequestBody CreateQuestionBankItemRequest request) {
        ensureEnabled();
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createItem(setId, request));
    }

    @GetMapping("/sets/{setId}/items")
    public ResponseEntity<List<QuestionBankItemResponse>> listItems(
            @PathVariable Long setId,
            @RequestParam(required = false) String status) {
        ensureEnabled();
        return ResponseEntity.ok(questionBankService.listItems(setId, status));
    }

    @PostMapping("/items/{itemId}/versions")
    public ResponseEntity<QuestionBankItemVersionResponse> createVersion(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateQuestionBankItemVersionRequest request) {
        ensureEnabled();
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createVersion(itemId, request));
    }

    @GetMapping("/items/{itemId}/versions")
    public ResponseEntity<List<QuestionBankItemVersionResponse>> listVersions(
            @PathVariable Long itemId,
            @RequestParam(required = false) String status) {
        ensureEnabled();
        return ResponseEntity.ok(questionBankService.listVersions(itemId, status));
    }

    @PostMapping("/items/{itemId}/versions/{versionNo}/activate")
    public ResponseEntity<QuestionBankItemVersionResponse> activateVersion(
            @PathVariable Long itemId,
            @PathVariable int versionNo) {
        ensureEnabled();
        return ResponseEntity.ok(questionBankService.activateVersion(itemId, versionNo));
    }

    @GetMapping("/items/{itemId}/versions/compare")
    public ResponseEntity<QuestionVersionCompareResponse> compareVersions(
            @PathVariable Long itemId,
            @RequestParam int fromVersion,
            @RequestParam int toVersion) {
        ensureEnabled();
        return ResponseEntity.ok(questionBankService.compareVersions(itemId, fromVersion, toVersion));
    }

    private void ensureEnabled() {
        if (!settingsResolver.resolveBoolean("features.enable-question-bank")) {
            throw new IllegalStateException("Feature is disabled: features.enable-question-bank");
        }
    }
}
