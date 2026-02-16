package com.evaluationservice.api.controller;

import com.evaluationservice.application.port.in.SystemSettingsUseCase;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.security.SecurityContextUserProvider;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin REST controller for system-wide settings and per-campaign overrides.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
public class SystemSettingsController {

    private final SystemSettingsUseCase settingsUseCase;
    private final SecurityContextUserProvider userProvider;

    public SystemSettingsController(
            SystemSettingsUseCase settingsUseCase,
            SecurityContextUserProvider userProvider) {
        this.settingsUseCase = settingsUseCase;
        this.userProvider = userProvider;
    }

    // --- System-wide Settings ---

    @GetMapping
    public ResponseEntity<List<SettingResponse>> getAllSettings() {
        List<SettingResponse> response = settingsUseCase.getAllSettings().stream()
                .map(SettingResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SettingResponse>> getSettingsByCategory(
            @PathVariable String category) {
        SystemSettingCategory cat = SystemSettingCategory.valueOf(category.toUpperCase());
        List<SettingResponse> response = settingsUseCase.getSettingsByCategory(cat).stream()
                .map(SettingResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{key}")
    public ResponseEntity<SettingResponse> getSettingByKey(@PathVariable String key) {
        SystemSetting setting = settingsUseCase.getSettingByKey(key);
        return ResponseEntity.ok(SettingResponse.from(setting));
    }

    @PutMapping("/{key}")
    public ResponseEntity<SettingResponse> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        SystemSetting updated = settingsUseCase.updateSetting(
                key, value, userProvider.getCurrentUserId());
        return ResponseEntity.ok(SettingResponse.from(updated));
    }

    // --- Campaign Overrides ---

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<List<OverrideResponse>> getCampaignOverrides(
            @PathVariable String campaignId) {
        List<OverrideResponse> response = settingsUseCase
                .getCampaignOverrides(CampaignId.of(campaignId))
                .stream()
                .map(OverrideResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/campaigns/{campaignId}/{key}")
    public ResponseEntity<OverrideResponse> setCampaignOverride(
            @PathVariable String campaignId,
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        CampaignSettingOverride override = settingsUseCase.setCampaignOverride(
                CampaignId.of(campaignId), key, value, userProvider.getCurrentUserId());
        return ResponseEntity.ok(OverrideResponse.from(override));
    }

    @DeleteMapping("/campaigns/{campaignId}/{key}")
    public ResponseEntity<Void> removeCampaignOverride(
            @PathVariable String campaignId,
            @PathVariable String key) {
        settingsUseCase.removeCampaignOverride(CampaignId.of(campaignId), key);
        return ResponseEntity.noContent().build();
    }

    // --- Response DTOs ---

    record SettingResponse(
            String key,
            String value,
            String category,
            String description,
            String updatedBy,
            String updatedAt) {
        static SettingResponse from(SystemSetting s) {
            return new SettingResponse(
                    s.getSettingKey(),
                    s.getSettingValue(),
                    s.getCategory().name(),
                    s.getDescription(),
                    s.getUpdatedBy(),
                    s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        }
    }

    record OverrideResponse(
            String campaignId,
            String key,
            String value,
            String updatedBy,
            String updatedAt) {
        static OverrideResponse from(CampaignSettingOverride o) {
            return new OverrideResponse(
                    o.getCampaignId().value(),
                    o.getSettingKey(),
                    o.getSettingValue(),
                    o.getUpdatedBy(),
                    o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        }
    }
}
