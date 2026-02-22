package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateAssignmentRequest;
import com.evaluationservice.api.dto.request.UpdateAssignmentRequest;
import com.evaluationservice.api.dto.response.AssignmentListResponse;
import com.evaluationservice.api.dto.response.AssignmentResponse;
import com.evaluationservice.api.exception.DuplicateAssignmentException;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssignmentManagementService {

    private static final List<String> ALLOWED_STEP_TYPES = List.of("STUDENT", "PEER", "SELF", "DEPARTMENT");
    private static final List<String> ALLOWED_STATUSES = List.of("ACTIVE", "COMPLETED", "INACTIVE");
    private static final List<String> ALLOWED_ANONYMITY_MODES = List.of("VISIBLE", "ANONYMOUS");

    private final CampaignAssignmentRepository assignmentRepository;
    private final CampaignRepository campaignRepository;

    public AssignmentManagementService(
            CampaignAssignmentRepository assignmentRepository,
            CampaignRepository campaignRepository) {
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
        this.campaignRepository = Objects.requireNonNull(campaignRepository);
    }

    @Transactional(readOnly = true)
    public AssignmentListResponse list(
            String campaignId,
            String stepType,
            String sectionId,
            String facultyId,
            String status,
            String evaluatorId,
            String evaluateeId,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String resolvedSortBy = resolveSortBy(sortBy);
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, resolvedSortBy));
        var result = assignmentRepository.findFilteredPage(
                normalize(campaignId),
                normalizeUpper(stepType),
                normalize(sectionId),
                normalize(facultyId),
                normalizeUpper(status),
                normalize(evaluatorId),
                normalize(evaluateeId),
                pageable);
        return new AssignmentListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(String id) {
        return assignmentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest request) {
        String campaignId = normalize(request.campaignId());
        if (campaignId == null || !campaignRepository.existsById(campaignId)) {
            throw new IllegalArgumentException("Campaign not found: " + request.campaignId());
        }
        String evaluatorRole = normalizeEvaluatorRole(request.evaluatorRole());
        String evaluatorId = normalizeRequired(request.evaluatorId(), "evaluatorId");
        String evaluateeId = normalizeRequired(request.evaluateeId(), "evaluateeId");
        var duplicate = assignmentRepository.findByCampaignIdAndEvaluatorIdAndEvaluateeIdAndEvaluatorRole(
                campaignId,
                evaluatorId,
                evaluateeId,
                evaluatorRole);
        if (duplicate.isPresent()) {
            throw new DuplicateAssignmentException(
                    campaignId,
                    evaluatorId,
                    evaluateeId,
                    evaluatorRole,
                    duplicate.get().getId());
        }

        Instant now = Instant.now();
        CampaignAssignmentEntity entity = new CampaignAssignmentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCampaignId(campaignId);
        entity.setEvaluatorId(evaluatorId);
        entity.setEvaluateeId(evaluateeId);
        entity.setEvaluatorRole(evaluatorRole);
        entity.setCompleted(false);
        entity.setEvaluationId(null);
        entity.setStepType(normalizeAllowed(request.stepType(), ALLOWED_STEP_TYPES, "stepType"));
        entity.setSectionId(normalize(request.sectionId()));
        entity.setFacultyId(normalize(request.facultyId()));
        entity.setAnonymityMode(normalizeAllowed(request.anonymityMode(), ALLOWED_ANONYMITY_MODES, "anonymityMode"));
        entity.setStatus(defaultStatus(request.status()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return toResponse(assignmentRepository.save(entity));
    }

    @Transactional
    public AssignmentResponse update(String id, UpdateAssignmentRequest request) {
        CampaignAssignmentEntity entity = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        entity.setStepType(normalizeAllowed(request.stepType(), ALLOWED_STEP_TYPES, "stepType"));
        entity.setSectionId(normalize(request.sectionId()));
        entity.setFacultyId(normalize(request.facultyId()));
        entity.setAnonymityMode(normalizeAllowed(request.anonymityMode(), ALLOWED_ANONYMITY_MODES, "anonymityMode"));
        if (request.status() != null) {
            entity.setStatus(normalizeAllowed(request.status(), ALLOWED_STATUSES, "status"));
        }
        entity.setUpdatedAt(Instant.now());
        return toResponse(assignmentRepository.save(entity));
    }

    private String normalizeEvaluatorRole(String value) {
        String normalized = normalizeRequired(value, "evaluatorRole").toUpperCase(Locale.ROOT);
        try {
            EvaluatorRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported evaluatorRole: " + normalized);
        }
        return normalized;
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private String defaultStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        return normalizeAllowed(value, ALLOWED_STATUSES, "status");
    }

    private String normalizeAllowed(String value, List<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + normalized);
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

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "updatedAt";
        }
        return switch (sortBy.trim()) {
            case "createdAt", "updatedAt", "evaluatorId", "evaluateeId", "status", "stepType" -> sortBy.trim();
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + sortBy);
        };
    }

    private AssignmentResponse toResponse(CampaignAssignmentEntity entity) {
        return new AssignmentResponse(
                entity.getId(),
                entity.getCampaignId(),
                entity.getEvaluatorId(),
                entity.getEvaluateeId(),
                entity.getEvaluatorRole(),
                entity.isCompleted(),
                entity.getEvaluationId(),
                entity.getStepType(),
                entity.getSectionId(),
                entity.getFacultyId(),
                entity.getAnonymityMode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
