package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateQuestionBankItemRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankItemVersionRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankSetRequest;
import com.evaluationservice.api.dto.response.QuestionBankItemResponse;
import com.evaluationservice.api.dto.response.QuestionBankItemVersionResponse;
import com.evaluationservice.api.dto.response.QuestionBankSetResponse;
import com.evaluationservice.api.dto.response.QuestionVersionCompareResponse;
import com.evaluationservice.domain.enums.QuestionType;
import com.evaluationservice.infrastructure.entity.QuestionBankItemEntity;
import com.evaluationservice.infrastructure.entity.QuestionBankItemVersionEntity;
import com.evaluationservice.infrastructure.entity.QuestionBankSetEntity;
import com.evaluationservice.infrastructure.repository.QuestionBankItemRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankItemVersionRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankSetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class QuestionBankService {

    private static final List<String> SET_STATUSES = List.of("ACTIVE", "INACTIVE");
    private static final List<String> ITEM_STATUSES = List.of("ACTIVE", "INACTIVE");
    private static final List<String> VERSION_STATUSES = List.of("DRAFT", "ACTIVE", "RETIRED");

    private final QuestionBankSetRepository setRepository;
    private final QuestionBankItemRepository itemRepository;
    private final QuestionBankItemVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public QuestionBankService(
            QuestionBankSetRepository setRepository,
            QuestionBankItemRepository itemRepository,
            QuestionBankItemVersionRepository versionRepository,
            ObjectMapper objectMapper) {
        this.setRepository = Objects.requireNonNull(setRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public QuestionBankSetResponse createSet(CreateQuestionBankSetRequest request) {
        Instant now = Instant.now();
        QuestionBankSetEntity entity = new QuestionBankSetEntity();
        entity.setTenantId(normalize(request.tenantId()));
        entity.setName(requireNonBlank(request.name(), "name"));
        entity.setVersionTag(normalize(request.versionTag()));
        entity.setOwner(normalize(request.owner()));
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(setRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<QuestionBankSetResponse> listSets(String tenantId, String status) {
        if (tenantId != null && !tenantId.isBlank()) {
            return setRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId.trim()).stream().map(this::toResponse).toList();
        }
        if (status != null && !status.isBlank()) {
            String normalized = normalizeStatus(status, SET_STATUSES, "status");
            return setRepository.findByStatusOrderByUpdatedAtDesc(normalized).stream().map(this::toResponse).toList();
        }
        return setRepository.findAll().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QuestionBankItemResponse createItem(Long setId, CreateQuestionBankItemRequest request) {
        setRepository.findById(setId).orElseThrow(() -> new IllegalArgumentException("Question bank set not found: " + setId));
        String stableKey = requireNonBlank(request.stableKey(), "stableKey");
        itemRepository.findBySetIdAndStableKey(setId, stableKey)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Duplicate stableKey in set: " + stableKey);
                });

        String defaultType = normalizeQuestionType(request.defaultType());
        BigDecimal marks = requireNonNegative(request.defaultMarks(), "defaultMarks");
        Instant now = Instant.now();
        QuestionBankItemEntity entity = new QuestionBankItemEntity();
        entity.setSetId(setId);
        entity.setStableKey(stableKey);
        entity.setContextType(normalize(request.contextType()));
        entity.setCategoryName(normalize(request.categoryName()));
        entity.setDefaultType(defaultType);
        entity.setDefaultMarks(marks);
        entity.setActiveVersionNo(1);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(itemRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<QuestionBankItemResponse> listItems(Long setId, String status) {
        if (status != null && !status.isBlank()) {
            String normalized = normalizeStatus(status, ITEM_STATUSES, "status");
            return itemRepository.findBySetIdAndStatusOrderByUpdatedAtDesc(setId, normalized).stream().map(this::toResponse).toList();
        }
        return itemRepository.findBySetIdOrderByUpdatedAtDesc(setId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuestionBankItemVersionResponse createVersion(Long itemId, CreateQuestionBankItemVersionRequest request) {
        QuestionBankItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Question bank item not found: " + itemId));
        String status = normalizeStatus(request.status(), VERSION_STATUSES, "status");
        String questionType = normalizeQuestionType(request.questionType());
        BigDecimal marks = requireNonNegative(request.marks(), "marks");
        int nextVersion = versionRepository.findByQuestionItemIdOrderByVersionNoDesc(itemId).stream()
                .mapToInt(QuestionBankItemVersionEntity::getVersionNo)
                .max()
                .orElse(0) + 1;

        Instant now = Instant.now();
        QuestionBankItemVersionEntity entity = new QuestionBankItemVersionEntity();
        entity.setQuestionItemId(itemId);
        entity.setVersionNo(nextVersion);
        entity.setStatus(status);
        entity.setChangeSummary(normalize(request.changeSummary()));
        entity.setQuestionText(requireNonBlank(request.questionText(), "questionText"));
        entity.setQuestionType(questionType);
        entity.setMarks(marks);
        entity.setRemarksMandatory(request.remarksMandatory());
        entity.setMetadataJson(toJson(request.metadata()));
        entity.setEffectiveFrom("ACTIVE".equals(status) ? now : null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        QuestionBankItemVersionEntity saved = versionRepository.save(entity);

        if ("ACTIVE".equals(status)) {
            retireOtherActiveVersions(itemId, saved.getVersionNo(), now);
            item.setActiveVersionNo(saved.getVersionNo());
            item.setDefaultType(saved.getQuestionType());
            item.setDefaultMarks(saved.getMarks());
            item.setUpdatedAt(now);
            itemRepository.save(item);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuestionBankItemVersionResponse> listVersions(Long itemId, String status) {
        if (status != null && !status.isBlank()) {
            String normalized = normalizeStatus(status, VERSION_STATUSES, "status");
            return versionRepository.findByQuestionItemIdAndStatusOrderByVersionNoDesc(itemId, normalized).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return versionRepository.findByQuestionItemIdOrderByVersionNoDesc(itemId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuestionBankItemVersionResponse activateVersion(Long itemId, int versionNo) {
        QuestionBankItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Question bank item not found: " + itemId));
        QuestionBankItemVersionEntity version = versionRepository.findByQuestionItemIdAndVersionNo(itemId, versionNo)
                .orElseThrow(() -> new IllegalArgumentException("Question bank item version not found: itemId=" + itemId + ", version=" + versionNo));
        Instant now = Instant.now();
        retireOtherActiveVersions(itemId, versionNo, now);
        version.setStatus("ACTIVE");
        version.setEffectiveFrom(now);
        version.setUpdatedAt(now);
        QuestionBankItemVersionEntity saved = versionRepository.save(version);
        item.setActiveVersionNo(versionNo);
        item.setDefaultType(saved.getQuestionType());
        item.setDefaultMarks(saved.getMarks());
        item.setUpdatedAt(now);
        itemRepository.save(item);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuestionVersionCompareResponse compareVersions(Long itemId, int fromVersion, int toVersion) {
        QuestionBankItemVersionEntity from = versionRepository.findByQuestionItemIdAndVersionNo(itemId, fromVersion)
                .orElseThrow(() -> new IllegalArgumentException("From version not found"));
        QuestionBankItemVersionEntity to = versionRepository.findByQuestionItemIdAndVersionNo(itemId, toVersion)
                .orElseThrow(() -> new IllegalArgumentException("To version not found"));
        Map<String, Object[]> diffs = new LinkedHashMap<>();
        diff(diffs, "status", from.getStatus(), to.getStatus());
        diff(diffs, "changeSummary", from.getChangeSummary(), to.getChangeSummary());
        diff(diffs, "questionText", from.getQuestionText(), to.getQuestionText());
        diff(diffs, "questionType", from.getQuestionType(), to.getQuestionType());
        diff(diffs, "marks", from.getMarks(), to.getMarks());
        diff(diffs, "remarksMandatory", from.isRemarksMandatory(), to.isRemarksMandatory());
        diff(diffs, "metadata", fromJson(from.getMetadataJson()), fromJson(to.getMetadataJson()));
        return QuestionVersionCompareResponse.fromDiffMap(itemId, fromVersion, toVersion, diffs);
    }

    private void retireOtherActiveVersions(Long itemId, int activeVersionNo, Instant now) {
        List<QuestionBankItemVersionEntity> versions = versionRepository.findByQuestionItemIdAndStatusOrderByVersionNoDesc(itemId, "ACTIVE");
        for (QuestionBankItemVersionEntity v : versions) {
            if (v.getVersionNo() != activeVersionNo) {
                v.setStatus("RETIRED");
                v.setUpdatedAt(now);
                versionRepository.save(v);
            }
        }
    }

    private void diff(Map<String, Object[]> diffs, String field, Object from, Object to) {
        if (!Objects.equals(from, to)) {
            diffs.put(field, new Object[] { from, to });
        }
    }

    private String normalizeQuestionType(String value) {
        String normalized = requireNonBlank(value, "questionType").toUpperCase(Locale.ROOT);
        try {
            QuestionType.valueOf(normalized);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported questionType: " + normalized);
        }
        return normalized;
    }

    private String normalizeStatus(String value, List<String> allowed, String field) {
        String normalized = requireNonBlank(value, field).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + normalized);
        }
        return normalized;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
        return value;
    }

    private String requireNonBlank(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize metadata", ex);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of("raw", json);
        }
    }

    private QuestionBankSetResponse toResponse(QuestionBankSetEntity entity) {
        return new QuestionBankSetResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getVersionTag(),
                entity.getOwner(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private QuestionBankItemResponse toResponse(QuestionBankItemEntity entity) {
        return new QuestionBankItemResponse(
                entity.getId(),
                entity.getSetId(),
                entity.getStableKey(),
                entity.getContextType(),
                entity.getCategoryName(),
                entity.getDefaultType(),
                entity.getDefaultMarks(),
                entity.getActiveVersionNo(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private QuestionBankItemVersionResponse toResponse(QuestionBankItemVersionEntity entity) {
        return new QuestionBankItemVersionResponse(
                entity.getId(),
                entity.getQuestionItemId(),
                entity.getVersionNo(),
                entity.getStatus(),
                entity.getChangeSummary(),
                entity.getQuestionText(),
                entity.getQuestionType(),
                entity.getMarks(),
                entity.isRemarksMandatory(),
                fromJson(entity.getMetadataJson()),
                entity.getEffectiveFrom(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
