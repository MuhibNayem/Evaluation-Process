package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateNotificationRuleRequest;
import com.evaluationservice.api.dto.request.CreateNotificationTemplateRequest;
import com.evaluationservice.api.dto.request.TestNotificationTemplateRequest;
import com.evaluationservice.api.dto.request.UpdateNotificationRuleRequest;
import com.evaluationservice.api.dto.request.UpdateNotificationTemplateRequest;
import com.evaluationservice.api.dto.response.NotificationDeliveryResponse;
import com.evaluationservice.api.dto.response.NotificationRuleResponse;
import com.evaluationservice.api.dto.response.NotificationTemplateResponse;
import com.evaluationservice.application.service.SettingsResolverService;
import com.evaluationservice.infrastructure.service.NotificationModuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private static final String FEATURE_FLAG = "features.enable-notification-rule-engine";

    private final NotificationModuleService notificationModuleService;
    private final SettingsResolverService settingsResolver;

    public NotificationController(
            NotificationModuleService notificationModuleService,
            SettingsResolverService settingsResolver) {
        this.notificationModuleService = notificationModuleService;
        this.settingsResolver = settingsResolver;
    }

    @PostMapping("/rules")
    public ResponseEntity<NotificationRuleResponse> createRule(
            @Valid @RequestBody CreateNotificationRuleRequest request) {
        ensureEnabled();
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationModuleService.createRule(request));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<NotificationRuleResponse>> listRules(
            @RequestParam(required = false) String campaignId) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.listRules(campaignId));
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<NotificationRuleResponse> getRule(@PathVariable Long id) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.getRule(id));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<NotificationRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotificationRuleRequest request) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ensureEnabled();
        notificationModuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody CreateNotificationTemplateRequest request) {
        ensureEnabled();
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationModuleService.createTemplate(request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplateResponse>> listTemplates(
            @RequestParam(required = false) String campaignId) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.listTemplates(campaignId));
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<NotificationTemplateResponse> getTemplate(@PathVariable Long id) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.getTemplate(id));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotificationTemplateRequest request) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        ensureEnabled();
        notificationModuleService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/{id}/test-render")
    public ResponseEntity<TemplateRenderResponse> testRender(
            @PathVariable Long id,
            @Valid @RequestBody TestNotificationTemplateRequest request) {
        ensureEnabled();
        Map<String, Object> variables = request.variables() == null ? Map.of() : request.variables();
        String renderedBody = notificationModuleService.renderTemplate(id, variables);
        return ResponseEntity.ok(new TemplateRenderResponse(request.recipient(), renderedBody));
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<NotificationDeliveryResponse>> listDeliveries(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String status) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.listDeliveries(campaignId, ruleId, status));
    }

    @PostMapping("/deliveries/{id}/retry")
    public ResponseEntity<NotificationDeliveryResponse> retryDelivery(@PathVariable Long id) {
        ensureEnabled();
        return ResponseEntity.ok(notificationModuleService.retryDelivery(id));
    }

    private void ensureEnabled() {
        if (!settingsResolver.resolveBoolean(FEATURE_FLAG)) {
            throw new IllegalStateException("Feature is disabled: " + FEATURE_FLAG);
        }
    }

    public record TemplateRenderResponse(String recipient, String renderedBody) {
    }
}
