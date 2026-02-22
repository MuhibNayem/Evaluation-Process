package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateNotificationRuleRequest;
import com.evaluationservice.api.dto.request.CreateNotificationTemplateRequest;
import com.evaluationservice.api.dto.request.UpdateNotificationRuleRequest;
import com.evaluationservice.api.dto.request.UpdateNotificationTemplateRequest;
import com.evaluationservice.api.dto.response.NotificationDeliveryResponse;
import com.evaluationservice.api.dto.response.NotificationRuleResponse;
import com.evaluationservice.api.dto.response.NotificationTemplateResponse;
import com.evaluationservice.application.port.out.NotificationPort;
import com.evaluationservice.infrastructure.entity.NotificationDeliveryEntity;
import com.evaluationservice.infrastructure.entity.NotificationRuleEntity;
import com.evaluationservice.infrastructure.entity.NotificationTemplateEntity;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.NotificationDeliveryRepository;
import com.evaluationservice.infrastructure.repository.NotificationRuleRepository;
import com.evaluationservice.infrastructure.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationModuleService {

    private static final List<String> TRIGGERS = List.of("EVALUATION_SUBMITTED", "CAMPAIGN_CLOSED", "SCHEDULED");
    private static final List<String> AUDIENCES = List.of("EVALUATOR", "EVALUATEE", "CAMPAIGN_OWNER", "CUSTOM");
    private static final List<String> CHANNELS = List.of("IN_APP", "EMAIL", "WEBHOOK", "SMS");
    private static final List<String> TEMPLATE_STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    private final NotificationRuleRepository ruleRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationPort notificationPort;
    private final ObjectMapper objectMapper;

    public NotificationModuleService(
            NotificationRuleRepository ruleRepository,
            NotificationTemplateRepository templateRepository,
            NotificationDeliveryRepository deliveryRepository,
            CampaignRepository campaignRepository,
            NotificationPort notificationPort,
            ObjectMapper objectMapper) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository);
        this.templateRepository = Objects.requireNonNull(templateRepository);
        this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
        this.campaignRepository = Objects.requireNonNull(campaignRepository);
        this.notificationPort = Objects.requireNonNull(notificationPort);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public NotificationRuleResponse createRule(CreateNotificationRuleRequest request) {
        String campaignId = require(request.campaignId(), "campaignId");
        ensureCampaignExists(campaignId);
        String code = require(request.ruleCode(), "ruleCode");
        ruleRepository.findByCampaignIdAndRuleCode(campaignId, code).ifPresent(existing -> {
            throw new IllegalArgumentException("Duplicate ruleCode for campaign: " + code);
        });
        NotificationRuleEntity entity = new NotificationRuleEntity();
        Instant now = Instant.now();
        entity.setCampaignId(campaignId);
        entity.setRuleCode(code);
        entity.setTriggerType(normalizeEnum(request.triggerType(), TRIGGERS, "triggerType"));
        entity.setAudience(normalizeEnum(request.audience(), AUDIENCES, "audience"));
        entity.setChannel(normalizeEnum(request.channel(), CHANNELS, "channel"));
        entity.setScheduleExpr(normalize(request.scheduleExpr()));
        if ("SCHEDULED".equals(entity.getTriggerType()) && entity.getScheduleExpr() == null) {
            throw new IllegalArgumentException("scheduleExpr is required for SCHEDULED trigger");
        }
        validateCron(entity.getScheduleExpr());
        entity.setEnabled(request.enabled());
        entity.setConfigJson(toJson(request.config()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(ruleRepository.save(entity));
    }

    @Transactional
    public NotificationRuleResponse updateRule(Long id, UpdateNotificationRuleRequest request) {
        NotificationRuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification rule not found: " + id));
        if (request.triggerType() != null) {
            entity.setTriggerType(normalizeEnum(request.triggerType(), TRIGGERS, "triggerType"));
        }
        if (request.audience() != null) {
            entity.setAudience(normalizeEnum(request.audience(), AUDIENCES, "audience"));
        }
        if (request.channel() != null) {
            entity.setChannel(normalizeEnum(request.channel(), CHANNELS, "channel"));
        }
        if (request.scheduleExpr() != null) {
            entity.setScheduleExpr(normalize(request.scheduleExpr()));
        }
        if ("SCHEDULED".equals(entity.getTriggerType()) && entity.getScheduleExpr() == null) {
            throw new IllegalArgumentException("scheduleExpr is required for SCHEDULED trigger");
        }
        validateCron(entity.getScheduleExpr());
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.config() != null) {
            entity.setConfigJson(toJson(request.config()));
        }
        entity.setUpdatedAt(Instant.now());
        return toResponse(ruleRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<NotificationRuleResponse> listRules(String campaignId) {
        List<NotificationRuleEntity> entities = campaignId == null || campaignId.isBlank()
                ? ruleRepository.findAll()
                : ruleRepository.findByCampaignIdOrderByUpdatedAtDesc(campaignId.trim());
        return entities.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationRuleResponse getRule(Long id) {
        return toResponse(ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification rule not found: " + id)));
    }

    @Transactional
    public void deleteRule(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification rule not found: " + id);
        }
        ruleRepository.deleteById(id);
    }

    @Transactional
    public NotificationTemplateResponse createTemplate(CreateNotificationTemplateRequest request) {
        String campaignId = normalize(request.campaignId());
        if (campaignId != null) {
            ensureCampaignExists(campaignId);
        }
        String code = require(request.templateCode(), "templateCode");
        if (campaignId != null) {
            templateRepository.findByCampaignIdAndTemplateCode(campaignId, code).ifPresent(existing -> {
                throw new IllegalArgumentException("Duplicate templateCode for campaign: " + code);
            });
        } else {
            templateRepository.findByTemplateCode(code).ifPresent(existing -> {
                throw new IllegalArgumentException("Duplicate global templateCode: " + code);
            });
        }
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        Instant now = Instant.now();
        entity.setCampaignId(campaignId);
        entity.setTemplateCode(code);
        entity.setName(require(request.name(), "name"));
        entity.setChannel(normalizeEnum(request.channel(), CHANNELS, "channel"));
        entity.setSubject(normalize(request.subject()));
        entity.setBody(require(request.body(), "body"));
        entity.setRequiredVariablesJson(toJson(request.requiredVariables() == null ? List.of() : request.requiredVariables()));
        entity.setStatus(normalizeEnum(request.status() == null ? "DRAFT" : request.status(), TEMPLATE_STATUSES, "status"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        validateTemplateEntity(entity);
        return toResponse(templateRepository.save(entity));
    }

    @Transactional
    public NotificationTemplateResponse updateTemplate(Long id, UpdateNotificationTemplateRequest request) {
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification template not found: " + id));
        if (request.name() != null) entity.setName(require(request.name(), "name"));
        if (request.channel() != null) entity.setChannel(normalizeEnum(request.channel(), CHANNELS, "channel"));
        if (request.subject() != null) entity.setSubject(normalize(request.subject()));
        if (request.body() != null) entity.setBody(require(request.body(), "body"));
        if (request.requiredVariables() != null) entity.setRequiredVariablesJson(toJson(request.requiredVariables()));
        if (request.status() != null) entity.setStatus(normalizeEnum(request.status(), TEMPLATE_STATUSES, "status"));
        entity.setUpdatedAt(Instant.now());
        validateTemplateEntity(entity);
        return toResponse(templateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplates(String campaignId) {
        List<NotificationTemplateEntity> entities = campaignId == null || campaignId.isBlank()
                ? templateRepository.findAll()
                : templateRepository.findByCampaignIdOrderByUpdatedAtDesc(campaignId.trim());
        return entities.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplate(Long id) {
        return toResponse(templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification template not found: " + id)));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new IllegalArgumentException("Notification template not found: " + id);
        }
        templateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryResponse> listDeliveries(String campaignId, Long ruleId, String status) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        return deliveryRepository.findFiltered(normalize(campaignId), ruleId, normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationDeliveryResponse retryDelivery(Long deliveryId) {
        NotificationDeliveryEntity existing = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Notification delivery not found: " + deliveryId));
        if (!"FAILED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Only FAILED deliveries can be retried");
        }
        Map<String, Object> payload = fromJsonMap(existing.getMessagePayloadJson());
        String recipient = String.valueOf(payload.getOrDefault("recipient", existing.getRecipient()));
        String campaignName = String.valueOf(payload.getOrDefault("campaignName", "Campaign"));
        String message = String.valueOf(payload.getOrDefault("message", "Notification retry"));
        dispatchViaPort(recipient, campaignName, message, payload);
        existing.setStatus("SENT");
        existing.setErrorCode(null);
        existing.setErrorMessage(null);
        existing.setSentAt(Instant.now());
        existing.setDeliveredAt(Instant.now());
        return toResponse(deliveryRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public String renderTemplate(Long templateId, Map<String, Object> variables) {
        NotificationTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Notification template not found: " + templateId));
        validateRequiredVariables(template, variables);
        return render(template.getBody(), variables);
    }

    @Transactional
    public void processEvent(String triggerType, String campaignId, Map<String, Object> context) {
        String normalizedTrigger = normalizeEnum(triggerType, TRIGGERS, "triggerType");
        List<NotificationRuleEntity> rules = ruleRepository.findByEnabledTrueAndTriggerTypeOrderByUpdatedAtDesc(normalizedTrigger);
        for (NotificationRuleEntity rule : rules) {
            if (campaignId != null && !campaignId.equals(rule.getCampaignId())) {
                continue;
            }
            deliverForRule(rule, context);
        }
    }

    @Transactional
    public void runScheduledRules(Instant now) {
        for (NotificationRuleEntity rule : ruleRepository.findByEnabledTrueAndTriggerTypeOrderByUpdatedAtDesc("SCHEDULED")) {
            if (rule.getScheduleExpr() == null || rule.getScheduleExpr().isBlank()) {
                continue;
            }
            CronExpression expression = CronExpression.parse(rule.getScheduleExpr());
            if (!expression.next(now.minusSeconds(60).atZone(ZoneOffset.UTC)).toInstant().isAfter(now)) {
                Instant minuteStart = now.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
                Instant minuteEnd = minuteStart.plusSeconds(59);
                if (!deliveryRepository.existsByRuleIdAndCreatedAtBetween(rule.getId(), minuteStart, minuteEnd)) {
                    deliverForRule(rule, Map.of("campaignId", rule.getCampaignId(), "message", "Scheduled notification"));
                }
            }
        }
    }

    private void deliverForRule(NotificationRuleEntity rule, Map<String, Object> context) {
        Map<String, Object> cfg = fromJsonMap(rule.getConfigJson());
        String recipient = resolveRecipient(rule.getAudience(), cfg, context);
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        NotificationTemplateEntity template = resolveTemplate(rule, cfg);
        String message;
        String subject = null;
        Long templateId = null;
        if (template != null) {
            validateRequiredVariables(template, context);
            message = render(template.getBody(), context);
            subject = template.getSubject() == null ? null : render(template.getSubject(), context);
            templateId = template.getId();
        } else {
            message = String.valueOf(context.getOrDefault("message", "Notification event: " + rule.getTriggerType()));
        }
        String campaignName = String.valueOf(context.getOrDefault("campaignName", rule.getCampaignId()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleCode", rule.getRuleCode());
        payload.put("recipient", recipient);
        payload.put("campaignName", campaignName);
        payload.put("subject", subject);
        payload.put("message", message);
        payload.put("context", context);

        NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
        delivery.setCampaignId(rule.getCampaignId());
        delivery.setRuleId(rule.getId());
        delivery.setTemplateId(templateId);
        delivery.setRecipient(recipient);
        delivery.setChannel(rule.getChannel());
        delivery.setCreatedAt(Instant.now());
        delivery.setMessagePayloadJson(toJson(payload));
        try {
            dispatchViaPort(recipient, campaignName, message, payload);
            delivery.setStatus("SENT");
            delivery.setSentAt(Instant.now());
            delivery.setDeliveredAt(Instant.now());
        } catch (Exception ex) {
            delivery.setStatus("FAILED");
            delivery.setErrorCode("DELIVERY_ERROR");
            delivery.setErrorMessage(ex.getMessage());
        }
        deliveryRepository.save(delivery);
    }

    private void dispatchViaPort(String recipient, String campaignName, String message, Map<String, Object> payload) {
        String type = String.valueOf(payload.getOrDefault("type", "REMINDER"));
        switch (type) {
            case "COMPLETION" -> notificationPort.sendCompletionNotification(recipient, campaignName);
            case "DEADLINE_EXTENSION" -> notificationPort.sendDeadlineExtensionNotification(
                    recipient,
                    campaignName,
                    String.valueOf(payload.getOrDefault("newDeadline", "")));
            default -> notificationPort.sendReminder(recipient, campaignName, message);
        }
    }

    private NotificationTemplateEntity resolveTemplate(NotificationRuleEntity rule, Map<String, Object> cfg) {
        Object templateId = cfg.get("templateId");
        if (templateId instanceof Number n) {
            return templateRepository.findById(n.longValue()).orElse(null);
        }
        Object templateCode = cfg.get("templateCode");
        if (templateCode instanceof String code && !code.isBlank()) {
            return templateRepository.findByCampaignIdAndTemplateCode(rule.getCampaignId(), code.trim()).orElse(null);
        }
        return null;
    }

    private String resolveRecipient(String audience, Map<String, Object> cfg, Map<String, Object> context) {
        return switch (audience) {
            case "EVALUATOR" -> asString(context.get("evaluatorId"));
            case "EVALUATEE" -> asString(context.get("evaluateeId"));
            case "CAMPAIGN_OWNER" -> asString(context.get("campaignOwnerId"));
            case "CUSTOM" -> asString(cfg.get("recipient"));
            default -> null;
        };
    }

    private String render(String text, Map<String, Object> variables) {
        if (text == null) return null;
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void validateTemplateEntity(NotificationTemplateEntity entity) {
        List<String> requiredVars = fromJsonList(entity.getRequiredVariablesJson());
        for (String var : requiredVars) {
            if (var == null || var.isBlank()) {
                throw new IllegalArgumentException("requiredVariables contains blank entry");
            }
        }
        if ("PUBLISHED".equals(entity.getStatus())) {
            List<String> placeholders = extractPlaceholders(entity.getBody(), entity.getSubject());
            for (String placeholder : placeholders) {
                if (!requiredVars.contains(placeholder)) {
                    throw new IllegalArgumentException("Missing requiredVariables entry for placeholder: " + placeholder);
                }
            }
        }
    }

    private List<String> extractPlaceholders(String... texts) {
        java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
        for (String text : texts) {
            if (text == null) continue;
            Matcher matcher = VARIABLE_PATTERN.matcher(text);
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return List.copyOf(keys);
    }

    private void validateRequiredVariables(NotificationTemplateEntity template, Map<String, Object> variables) {
        List<String> required = fromJsonList(template.getRequiredVariablesJson());
        for (String key : required) {
            Object value = variables.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                throw new IllegalArgumentException("Missing required template variable: " + key);
            }
        }
    }

    private String normalizeEnum(String value, List<String> allowed, String field) {
        String normalized = require(value, field).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + normalized);
        }
        return normalized;
    }

    private void validateCron(String cron) {
        if (cron == null || cron.isBlank()) return;
        try {
            CronExpression.parse(cron);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid scheduleExpr cron");
        }
    }

    private String require(String value, String field) {
        String v = normalize(value);
        if (v == null) throw new IllegalArgumentException(field + " is required");
        return v;
    }

    private String normalize(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String asString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private void ensureCampaignExists(String campaignId) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new IllegalArgumentException("Campaign not found: " + campaignId);
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize json", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private NotificationRuleResponse toResponse(NotificationRuleEntity e) {
        return new NotificationRuleResponse(
                e.getId(),
                e.getCampaignId(),
                e.getRuleCode(),
                e.getTriggerType(),
                e.getAudience(),
                e.getChannel(),
                e.getScheduleExpr(),
                e.isEnabled(),
                fromJsonMap(e.getConfigJson()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private NotificationTemplateResponse toResponse(NotificationTemplateEntity e) {
        return new NotificationTemplateResponse(
                e.getId(),
                e.getCampaignId(),
                e.getTemplateCode(),
                e.getName(),
                e.getChannel(),
                e.getSubject(),
                e.getBody(),
                fromJsonList(e.getRequiredVariablesJson()),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private NotificationDeliveryResponse toResponse(NotificationDeliveryEntity e) {
        return new NotificationDeliveryResponse(
                e.getId(),
                e.getCampaignId(),
                e.getRuleId(),
                e.getTemplateId(),
                e.getRecipient(),
                e.getChannel(),
                e.getStatus(),
                e.getErrorCode(),
                e.getErrorMessage(),
                fromJsonMap(e.getMessagePayloadJson()),
                e.getSentAt(),
                e.getDeliveredAt(),
                e.getCreatedAt());
    }
}
