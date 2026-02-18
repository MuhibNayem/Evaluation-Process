package com.evaluationservice.infrastructure.config;

import com.evaluationservice.domain.enums.ScoringMethod;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public enum AssignmentStorageMode {
        JSON,
        DUAL,
        V2
    }

    private Scoring scoring = new Scoring();
    private Pagination pagination = new Pagination();
    private Campaign campaign = new Campaign();
    private Notification notification = new Notification();
    private Features features = new Features();
    private Assignment assignment = new Assignment();
    private Audience audience = new Audience();
    private Admin admin = new Admin();

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

    /**
     * Controls migration/cutover behavior for campaign assignment storage.
     */
    public static class Assignment {
        /** Assignment storage/read mode. */
        private AssignmentStorageMode storageMode = AssignmentStorageMode.DUAL;
        /** Whether scheduled reconciliation checks are enabled. */
        private boolean reconciliationEnabled = true;
        /** Cron expression for scheduled reconciliation checks. */
        private String reconciliationCron = "0 */30 * * * *";
        /** Max campaigns scanned per run. */
        private int reconciliationMaxCampaigns = 500;

        public AssignmentStorageMode getStorageMode() {
            return storageMode;
        }

        public void setStorageMode(AssignmentStorageMode storageMode) {
            this.storageMode = storageMode;
        }

        public boolean isReconciliationEnabled() {
            return reconciliationEnabled;
        }

        public void setReconciliationEnabled(boolean reconciliationEnabled) {
            this.reconciliationEnabled = reconciliationEnabled;
        }

        public String getReconciliationCron() {
            return reconciliationCron;
        }

        public void setReconciliationCron(String reconciliationCron) {
            this.reconciliationCron = reconciliationCron;
        }

        public int getReconciliationMaxCampaigns() {
            return reconciliationMaxCampaigns;
        }

        public void setReconciliationMaxCampaigns(int reconciliationMaxCampaigns) {
            this.reconciliationMaxCampaigns = reconciliationMaxCampaigns;
        }
    }

    /**
     * Audience ingestion connector configuration (Phase 3+).
     */
    public static class Audience {
        private Jdbc jdbc = new Jdbc();
        private Retention retention = new Retention();
        private Outbox outbox = new Outbox();
        private Map<String, ValidationProfile> validationProfiles = new HashMap<>();

        public Jdbc getJdbc() {
            return jdbc;
        }

        public void setJdbc(Jdbc jdbc) {
            this.jdbc = jdbc;
        }

        public Retention getRetention() {
            return retention;
        }

        public void setRetention(Retention retention) {
            this.retention = retention;
        }

        public Outbox getOutbox() {
            return outbox;
        }

        public void setOutbox(Outbox outbox) {
            this.outbox = outbox;
        }

        public Map<String, ValidationProfile> getValidationProfiles() {
            return validationProfiles;
        }

        public void setValidationProfiles(Map<String, ValidationProfile> validationProfiles) {
            this.validationProfiles = validationProfiles;
        }
    }

    public static class ValidationProfile {
        private boolean requirePersonDisplayName = false;
        private int minPersonDisplayNameLength = 0;
        private boolean requirePersonEmail = false;
        private List<String> allowedEmailDomains = List.of();
        private List<String> allowedGroupTypes = List.of();
        private boolean requireGroupExternalRef = false;
        private boolean requireMembershipRole = false;
        private boolean requireMembershipValidityWindow = false;
        private boolean requireActivePersonForMembership = false;
        private boolean requireActiveGroupForMembership = false;
        private Map<String, List<String>> membershipRoleAllowedGroupTypes = new HashMap<>();

        public boolean isRequirePersonDisplayName() {
            return requirePersonDisplayName;
        }

        public void setRequirePersonDisplayName(boolean requirePersonDisplayName) {
            this.requirePersonDisplayName = requirePersonDisplayName;
        }

        public int getMinPersonDisplayNameLength() {
            return minPersonDisplayNameLength;
        }

        public void setMinPersonDisplayNameLength(int minPersonDisplayNameLength) {
            this.minPersonDisplayNameLength = minPersonDisplayNameLength;
        }

        public boolean isRequirePersonEmail() {
            return requirePersonEmail;
        }

        public void setRequirePersonEmail(boolean requirePersonEmail) {
            this.requirePersonEmail = requirePersonEmail;
        }

        public List<String> getAllowedEmailDomains() {
            return allowedEmailDomains;
        }

        public void setAllowedEmailDomains(List<String> allowedEmailDomains) {
            this.allowedEmailDomains = allowedEmailDomains == null ? List.of() : allowedEmailDomains;
        }

        public List<String> getAllowedGroupTypes() {
            return allowedGroupTypes;
        }

        public void setAllowedGroupTypes(List<String> allowedGroupTypes) {
            this.allowedGroupTypes = allowedGroupTypes == null ? List.of() : allowedGroupTypes;
        }

        public boolean isRequireGroupExternalRef() {
            return requireGroupExternalRef;
        }

        public void setRequireGroupExternalRef(boolean requireGroupExternalRef) {
            this.requireGroupExternalRef = requireGroupExternalRef;
        }

        public boolean isRequireMembershipRole() {
            return requireMembershipRole;
        }

        public void setRequireMembershipRole(boolean requireMembershipRole) {
            this.requireMembershipRole = requireMembershipRole;
        }

        public boolean isRequireMembershipValidityWindow() {
            return requireMembershipValidityWindow;
        }

        public void setRequireMembershipValidityWindow(boolean requireMembershipValidityWindow) {
            this.requireMembershipValidityWindow = requireMembershipValidityWindow;
        }

        public boolean isRequireActivePersonForMembership() {
            return requireActivePersonForMembership;
        }

        public void setRequireActivePersonForMembership(boolean requireActivePersonForMembership) {
            this.requireActivePersonForMembership = requireActivePersonForMembership;
        }

        public boolean isRequireActiveGroupForMembership() {
            return requireActiveGroupForMembership;
        }

        public void setRequireActiveGroupForMembership(boolean requireActiveGroupForMembership) {
            this.requireActiveGroupForMembership = requireActiveGroupForMembership;
        }

        public Map<String, List<String>> getMembershipRoleAllowedGroupTypes() {
            return membershipRoleAllowedGroupTypes;
        }

        public void setMembershipRoleAllowedGroupTypes(Map<String, List<String>> membershipRoleAllowedGroupTypes) {
            this.membershipRoleAllowedGroupTypes = membershipRoleAllowedGroupTypes == null
                    ? new HashMap<>()
                    : membershipRoleAllowedGroupTypes;
        }
    }

    public static class Jdbc {
        /**
         * Named connection references used by JDBC audience ingestion.
         * Key = connectionRef from request.sourceConfig.
         */
        private Map<String, JdbcConnectionRef> connections = new HashMap<>();

        public Map<String, JdbcConnectionRef> getConnections() {
            return connections;
        }

        public void setConnections(Map<String, JdbcConnectionRef> connections) {
            this.connections = connections;
        }
    }

    public static class JdbcConnectionRef {
        private boolean enabled = true;
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private String defaultQuery;
        private boolean allowCustomQuery = false;
        private int queryTimeoutSeconds = 30;
        private int maxRows = 10000;
        private int fetchSize = 500;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getDefaultQuery() {
            return defaultQuery;
        }

        public void setDefaultQuery(String defaultQuery) {
            this.defaultQuery = defaultQuery;
        }

        public boolean isAllowCustomQuery() {
            return allowCustomQuery;
        }

        public void setAllowCustomQuery(boolean allowCustomQuery) {
            this.allowCustomQuery = allowCustomQuery;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public int getFetchSize() {
            return fetchSize;
        }

        public void setFetchSize(int fetchSize) {
            this.fetchSize = fetchSize;
        }
    }

    public static class Retention {
        private boolean enabled = true;
        private String cron = "0 0 3 * * *";
        private int snapshotTtlDays = 30;
        private int mappingEventTtlDays = 90;
        private int outboxPublishedTtlDays = 30;
        private int outboxFailedTtlDays = 180;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getSnapshotTtlDays() {
            return snapshotTtlDays;
        }

        public void setSnapshotTtlDays(int snapshotTtlDays) {
            this.snapshotTtlDays = snapshotTtlDays;
        }

        public int getMappingEventTtlDays() {
            return mappingEventTtlDays;
        }

        public void setMappingEventTtlDays(int mappingEventTtlDays) {
            this.mappingEventTtlDays = mappingEventTtlDays;
        }

        public int getOutboxPublishedTtlDays() {
            return outboxPublishedTtlDays;
        }

        public void setOutboxPublishedTtlDays(int outboxPublishedTtlDays) {
            this.outboxPublishedTtlDays = outboxPublishedTtlDays;
        }

        public int getOutboxFailedTtlDays() {
            return outboxFailedTtlDays;
        }

        public void setOutboxFailedTtlDays(int outboxFailedTtlDays) {
            this.outboxFailedTtlDays = outboxFailedTtlDays;
        }
    }

    public static class Outbox {
        private String transport = "LOG";
        private boolean enabled = true;
        private String cron = "0 */1 * * * *";
        private int batchSize = 100;
        private int maxAttempts = 10;
        private int baseBackoffSeconds = 5;
        private int maxBackoffSeconds = 3600;
        private boolean webhookEnabled = false;
        private String webhookUrl = "";
        private int webhookTimeoutMs = 5000;
        private String webhookAuthToken;
        private Map<String, String> webhookHeaders = new HashMap<>();
        private Kafka kafka = new Kafka();
        private Rabbitmq rabbitmq = new Rabbitmq();

        public String getTransport() {
            return transport;
        }

        public void setTransport(String transport) {
            this.transport = transport;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getBaseBackoffSeconds() {
            return baseBackoffSeconds;
        }

        public void setBaseBackoffSeconds(int baseBackoffSeconds) {
            this.baseBackoffSeconds = baseBackoffSeconds;
        }

        public int getMaxBackoffSeconds() {
            return maxBackoffSeconds;
        }

        public void setMaxBackoffSeconds(int maxBackoffSeconds) {
            this.maxBackoffSeconds = maxBackoffSeconds;
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

        public String getWebhookAuthToken() {
            return webhookAuthToken;
        }

        public void setWebhookAuthToken(String webhookAuthToken) {
            this.webhookAuthToken = webhookAuthToken;
        }

        public Map<String, String> getWebhookHeaders() {
            return webhookHeaders;
        }

        public void setWebhookHeaders(Map<String, String> webhookHeaders) {
            this.webhookHeaders = webhookHeaders;
        }

        public Kafka getKafka() {
            return kafka;
        }

        public void setKafka(Kafka kafka) {
            this.kafka = kafka;
        }

        public Rabbitmq getRabbitmq() {
            return rabbitmq;
        }

        public void setRabbitmq(Rabbitmq rabbitmq) {
            this.rabbitmq = rabbitmq;
        }
    }

    public static class Admin {
        private boolean publishLockEnabled = true;
        private boolean requireFourEyesApproval = true;

        public boolean isPublishLockEnabled() {
            return publishLockEnabled;
        }

        public void setPublishLockEnabled(boolean publishLockEnabled) {
            this.publishLockEnabled = publishLockEnabled;
        }

        public boolean isRequireFourEyesApproval() {
            return requireFourEyesApproval;
        }

        public void setRequireFourEyesApproval(boolean requireFourEyesApproval) {
            this.requireFourEyesApproval = requireFourEyesApproval;
        }
    }

    public static class Kafka {
        private boolean enabled = false;
        private String topic = "evaluation.outbox.events";
        private long sendTimeoutMs = 5000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public long getSendTimeoutMs() {
            return sendTimeoutMs;
        }

        public void setSendTimeoutMs(long sendTimeoutMs) {
            this.sendTimeoutMs = sendTimeoutMs;
        }
    }

    public static class Rabbitmq {
        private boolean enabled = false;
        private String exchange = "evaluation.outbox.events";
        private String routingKey = "evaluation.outbox.event";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
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

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }
}
