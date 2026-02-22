package com.evaluationservice.application.service;

import com.evaluationservice.application.port.out.CampaignSettingsPersistencePort;
import com.evaluationservice.application.port.out.SystemSettingsPersistencePort;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.domain.value.CampaignId;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the effective value for any setting key using a 3-level hierarchy:
 * <ol>
 * <li>Campaign-level override (if campaignId provided)</li>
 * <li>System-level DB setting</li>
 * <li>application.yml default (via {@link EvaluationServiceProperties})</li>
 * </ol>
 */
@Service
public class SettingsResolverService {

    private final SystemSettingsPersistencePort systemSettingsPort;
    private final CampaignSettingsPersistencePort campaignSettingsPort;
    private final EvaluationServiceProperties defaultProperties;

    public SettingsResolverService(
            SystemSettingsPersistencePort systemSettingsPort,
            CampaignSettingsPersistencePort campaignSettingsPort,
            EvaluationServiceProperties defaultProperties) {
        this.systemSettingsPort = Objects.requireNonNull(systemSettingsPort);
        this.campaignSettingsPort = Objects.requireNonNull(campaignSettingsPort);
        this.defaultProperties = Objects.requireNonNull(defaultProperties);
    }

    /**
     * Resolves a setting for a specific campaign (3-level fallback).
     */
    public String resolve(String key, CampaignId campaignId) {
        // Level 1: Campaign override
        if (campaignId != null) {
            Optional<CampaignSettingOverride> override = campaignSettingsPort.findByCampaignIdAndKey(campaignId, key);
            if (override.isPresent()) {
                return override.get().getSettingValue();
            }
        }

        // Level 2: System DB setting
        Optional<SystemSetting> systemSetting = systemSettingsPort.findByKey(key);
        if (systemSetting.isPresent()) {
            return systemSetting.get().getSettingValue();
        }

        // Level 3: application.yml default
        return resolveFromProperties(key);
    }

    /**
     * Resolves a setting at system level only (no campaign context).
     */
    public String resolve(String key) {
        return resolve(key, null);
    }

    // --- Typed resolvers ---

    public int resolveInt(String key, CampaignId campaignId) {
        return Integer.parseInt(resolve(key, campaignId));
    }

    public int resolveInt(String key) {
        return resolveInt(key, null);
    }

    public double resolveDouble(String key, CampaignId campaignId) {
        return Double.parseDouble(resolve(key, campaignId));
    }

    public double resolveDouble(String key) {
        return resolveDouble(key, null);
    }

    public boolean resolveBoolean(String key, CampaignId campaignId) {
        return Boolean.parseBoolean(resolve(key, campaignId));
    }

    public boolean resolveBoolean(String key) {
        return resolveBoolean(key, null);
    }

    /**
     * Falls back to application.yml defaults via EvaluationServiceProperties.
     */
    private String resolveFromProperties(String key) {
        return switch (key) {
            // Scoring
            case "scoring.default-method" -> defaultProperties.getScoring().getDefaultMethod().name();
            case "scoring.passing-score-threshold" ->
                String.valueOf(defaultProperties.getScoring().getPassingScoreThreshold());
            case "scoring.auto-score-on-submit" -> String.valueOf(defaultProperties.getScoring().isAutoScoreOnSubmit());
            case "scoring.enable-partial-credit" ->
                String.valueOf(defaultProperties.getScoring().isEnablePartialCredit());
            case "scoring.max-score-value" -> String.valueOf(defaultProperties.getScoring().getMaxScoreValue());

            // Campaign
            case "campaign.auto-activate" -> String.valueOf(defaultProperties.getCampaign().isAutoActivate());
            case "campaign.auto-close" -> String.valueOf(defaultProperties.getCampaign().isAutoClose());
            case "campaign.max-deadline-extension-days" ->
                String.valueOf(defaultProperties.getCampaign().getMaxDeadlineExtensionDays());
            case "campaign.default-minimum-respondents" ->
                String.valueOf(defaultProperties.getCampaign().getDefaultMinimumRespondents());
            case "campaign.send-deadline-reminders" ->
                String.valueOf(defaultProperties.getCampaign().isSendDeadlineReminders());
            case "campaign.reminder-days-before-deadline" ->
                String.valueOf(defaultProperties.getCampaign().getReminderDaysBeforeDeadline());

            // Notification
            case "notification.enabled" -> String.valueOf(defaultProperties.getNotification().isEnabled());

            // Features
            case "features.allow-anonymous-mode" ->
                String.valueOf(defaultProperties.getFeatures().isAllowAnonymousMode());
            case "features.enable-reports" -> String.valueOf(defaultProperties.getFeatures().isEnableReports());
            case "features.enable-csv-export" -> String.valueOf(defaultProperties.getFeatures().isEnableCsvExport());
            case "features.enable-pdf-export" -> String.valueOf(defaultProperties.getFeatures().isEnablePdfExport());
            case "features.enable-step-windows" ->
                String.valueOf(defaultProperties.getFeatures().isEnableStepWindows());
            case "features.enable-pdf-lifecycle" ->
                String.valueOf(defaultProperties.getFeatures().isEnablePdfLifecycle());
            case "features.enable-question-bank" ->
                String.valueOf(defaultProperties.getFeatures().isEnableQuestionBank());
            case "features.enable-notification-rule-engine" ->
                String.valueOf(defaultProperties.getFeatures().isEnableNotificationRuleEngine());

            // Pagination
            case "pagination.default-page-size" ->
                String.valueOf(defaultProperties.getPagination().getDefaultPageSize());
            case "pagination.max-page-size" -> String.valueOf(defaultProperties.getPagination().getMaxPageSize());

            default -> throw new IllegalArgumentException("Unknown setting key: " + key);
        };
    }
}
