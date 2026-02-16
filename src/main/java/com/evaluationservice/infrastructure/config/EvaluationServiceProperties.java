package com.evaluationservice.infrastructure.config;

import com.evaluationservice.domain.enums.ScoringMethod;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized configuration properties for the evaluation service.
 * All runtime-configurable settings are externalized here and mapped
 * from {@code evaluation.service.*} keys in application.yml.
 *
 * @see <a href=
 *      "https://docs.spring.io/spring-boot/reference/features/external-config.html">Spring
 *      Boot External Config</a>
 */
@Configuration
@ConfigurationProperties(prefix = "evaluation.service")
public class EvaluationServiceProperties {

    private Scoring scoring = new Scoring();
    private Pagination pagination = new Pagination();
    private Campaign campaign = new Campaign();
    private Notification notification = new Notification();
    private Features features = new Features();

    // --- Scoring Configuration ---

    /**
     * Controls default scoring behavior, passing thresholds,
     * and whether auto-scoring is enabled on submission.
     */
    public static class Scoring {
        /** Default scoring method when none is specified on the template/campaign */
        private ScoringMethod defaultMethod = ScoringMethod.WEIGHTED_AVERAGE;
        /** Score threshold (percentage) to consider an evaluation "passing" */
        private double passingScoreThreshold = 70.0;
        /** Whether to automatically score evaluations on submission */
        private boolean autoScoreOnSubmit = true;
        /** Whether to enable partial credit for partially correct answers */
        private boolean enablePartialCredit = true;
        /** Maximum score value (used for normalization) */
        private double maxScoreValue = 100.0;

        public ScoringMethod getDefaultMethod() {
            return defaultMethod;
        }

        public void setDefaultMethod(ScoringMethod defaultMethod) {
            this.defaultMethod = defaultMethod;
        }

        public double getPassingScoreThreshold() {
            return passingScoreThreshold;
        }

        public void setPassingScoreThreshold(double passingScoreThreshold) {
            this.passingScoreThreshold = passingScoreThreshold;
        }

        public boolean isAutoScoreOnSubmit() {
            return autoScoreOnSubmit;
        }

        public void setAutoScoreOnSubmit(boolean autoScoreOnSubmit) {
            this.autoScoreOnSubmit = autoScoreOnSubmit;
        }

        public boolean isEnablePartialCredit() {
            return enablePartialCredit;
        }

        public void setEnablePartialCredit(boolean enablePartialCredit) {
            this.enablePartialCredit = enablePartialCredit;
        }

        public double getMaxScoreValue() {
            return maxScoreValue;
        }

        public void setMaxScoreValue(double maxScoreValue) {
            this.maxScoreValue = maxScoreValue;
        }
    }

    // --- Pagination Configuration ---

    /**
     * Controls default and maximum pagination sizes for list endpoints.
     */
    public static class Pagination {
        /** Default number of items per page */
        private int defaultPageSize = 20;
        /** Maximum allowed page size to prevent abuse */
        private int maxPageSize = 100;

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }

    // --- Campaign Configuration ---

    /**
     * Controls campaign lifecycle automation and constraints.
     */
    public static class Campaign {
        /** Whether to automatically activate campaigns at their start date */
        private boolean autoActivate = false;
        /** Whether to automatically close campaigns at their end date */
        private boolean autoClose = false;
        /** Cron expression for campaign lifecycle scheduler */
        private String schedulerCron = "0 0 * * * *"; // every hour
        /** Maximum number of days a deadline can be extended */
        private int maxDeadlineExtensionDays = 30;
        /** Default minimum respondents for a campaign */
        private int defaultMinimumRespondents = 1;
        /** Whether to send reminder notifications before deadline */
        private boolean sendDeadlineReminders = true;
        /** Days before deadline to send reminders */
        private int reminderDaysBeforeDeadline = 3;

        public boolean isAutoActivate() {
            return autoActivate;
        }

        public void setAutoActivate(boolean autoActivate) {
            this.autoActivate = autoActivate;
        }

        public boolean isAutoClose() {
            return autoClose;
        }

        public void setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
        }

        public String getSchedulerCron() {
            return schedulerCron;
        }

        public void setSchedulerCron(String schedulerCron) {
            this.schedulerCron = schedulerCron;
        }

        public int getMaxDeadlineExtensionDays() {
            return maxDeadlineExtensionDays;
        }

        public void setMaxDeadlineExtensionDays(int maxDeadlineExtensionDays) {
            this.maxDeadlineExtensionDays = maxDeadlineExtensionDays;
        }

        public int getDefaultMinimumRespondents() {
            return defaultMinimumRespondents;
        }

        public void setDefaultMinimumRespondents(int defaultMinimumRespondents) {
            this.defaultMinimumRespondents = defaultMinimumRespondents;
        }

        public boolean isSendDeadlineReminders() {
            return sendDeadlineReminders;
        }

        public void setSendDeadlineReminders(boolean sendDeadlineReminders) {
            this.sendDeadlineReminders = sendDeadlineReminders;
        }

        public int getReminderDaysBeforeDeadline() {
            return reminderDaysBeforeDeadline;
        }

        public void setReminderDaysBeforeDeadline(int reminderDaysBeforeDeadline) {
            this.reminderDaysBeforeDeadline = reminderDaysBeforeDeadline;
        }
    }

    // --- Notification Configuration ---

    /**
     * Controls which notification channels are enabled and their settings.
     */
    public static class Notification {
        /** Whether notifications are enabled globally */
        private boolean enabled = true;
        /** Whether to send via webhook */
        private boolean webhookEnabled = false;
        /** Webhook URL for external notification delivery */
        private String webhookUrl = "";
        /** Webhook timeout in milliseconds */
        private int webhookTimeoutMs = 5000;
        /** Whether to send email notifications */
        private boolean emailEnabled = false;
        /** Email sender address */
        private String emailFrom = "noreply@evaluationservice.com";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isWebhookEnabled() {
            return webhookEnabled;
        }

        public void setWebhookEnabled(boolean webhookEnabled) {
            this.webhookEnabled = webhookEnabled;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public int getWebhookTimeoutMs() {
            return webhookTimeoutMs;
        }

        public void setWebhookTimeoutMs(int webhookTimeoutMs) {
            this.webhookTimeoutMs = webhookTimeoutMs;
        }

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public String getEmailFrom() {
            return emailFrom;
        }

        public void setEmailFrom(String emailFrom) {
            this.emailFrom = emailFrom;
        }
    }

    // --- Feature Flags ---

    /**
     * Feature toggles for enabling/disabling service capabilities.
     */
    public static class Features {
        /** Whether anonymous submission mode is available */
        private boolean allowAnonymousMode = true;
        /** Whether report generation is enabled */
        private boolean enableReports = true;
        /** Whether CSV export is enabled */
        private boolean enableCsvExport = true;
        /** Whether PDF export is enabled */
        private boolean enablePdfExport = false;

        public boolean isAllowAnonymousMode() {
            return allowAnonymousMode;
        }

        public void setAllowAnonymousMode(boolean allowAnonymousMode) {
            this.allowAnonymousMode = allowAnonymousMode;
        }

        public boolean isEnableReports() {
            return enableReports;
        }

        public void setEnableReports(boolean enableReports) {
            this.enableReports = enableReports;
        }

        public boolean isEnableCsvExport() {
            return enableCsvExport;
        }

        public void setEnableCsvExport(boolean enableCsvExport) {
            this.enableCsvExport = enableCsvExport;
        }

        public boolean isEnablePdfExport() {
            return enablePdfExport;
        }

        public void setEnablePdfExport(boolean enablePdfExport) {
            this.enablePdfExport = enablePdfExport;
        }
    }

    // --- Root Getters/Setters ---

    public Scoring getScoring() {
        return scoring;
    }

    public void setScoring(Scoring scoring) {
        this.scoring = scoring;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }
}