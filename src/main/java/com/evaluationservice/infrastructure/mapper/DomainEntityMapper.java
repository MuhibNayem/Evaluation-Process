package com.evaluationservice.infrastructure.mapper;

import com.evaluationservice.domain.entity.*;
import com.evaluationservice.domain.enums.*;
import com.evaluationservice.domain.value.*;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.entity.EvaluationEntity;
import com.evaluationservice.infrastructure.entity.TemplateEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain entities and JPA entities.
 * Handles JSON serialization/deserialization for complex fields.
 */
@Component
public class DomainEntityMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ==================== Template Mapping ====================

    public TemplateEntity toJpaEntity(Template domain) {
        var entity = new TemplateEntity();
        entity.setId(domain.getId().value());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCategory(domain.getCategory());
        entity.setStatus(domain.getStatus().name());
        entity.setCurrentVersion(domain.getCurrentVersion());
        entity.setScoringMethod(domain.getScoringMethod().name());
        entity.setCustomFormula(domain.getCustomFormula());
        entity.setSectionsJson(serializeSections(domain.getSections()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt().value());
        entity.setUpdatedAt(domain.getUpdatedAt().value());
        return entity;
    }

    public Template toDomainTemplate(TemplateEntity entity) {
        return new Template(
                TemplateId.of(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                TemplateStatus.valueOf(entity.getStatus()),
                entity.getCurrentVersion(),
                ScoringMethod.valueOf(entity.getScoringMethod()),
                deserializeSections(entity.getSectionsJson()),
                entity.getCreatedBy(),
                new Timestamp(entity.getCreatedAt()),
                new Timestamp(entity.getUpdatedAt()),
                entity.getCustomFormula());
    }

    // ==================== Campaign Mapping ====================

    public CampaignEntity toJpaEntity(Campaign domain) {
        var entity = new CampaignEntity();
        entity.setId(domain.getId().value());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setTemplateId(domain.getTemplateId().value());
        entity.setTemplateVersion(domain.getTemplateVersion());
        entity.setStatus(domain.getStatus().name());
        entity.setStartDate(domain.getDateRange().startDate());
        entity.setEndDate(domain.getDateRange().endDate());
        entity.setScoringMethod(domain.getScoringMethod().name());
        entity.setAnonymousMode(domain.isAnonymousMode());
        entity.setAnonymousRolesJson(serializeEnumSet(domain.getAnonymousRoles()));
        entity.setMinimumRespondents(domain.getMinimumRespondents());
        entity.setAssignmentsJson(serializeAssignments(domain.getAssignments()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt().value());
        entity.setUpdatedAt(domain.getUpdatedAt().value());
        return entity;
    }

    public Campaign toDomainCampaign(CampaignEntity entity) {
        Set<EvaluatorRole> anonymousRoles = deserializeEnumSet(entity.getAnonymousRolesJson(), EvaluatorRole.class);
        return new Campaign(
                CampaignId.of(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                TemplateId.of(entity.getTemplateId()),
                entity.getTemplateVersion(),
                CampaignStatus.valueOf(entity.getStatus()),
                DateRange.of(entity.getStartDate(), entity.getEndDate()),
                ScoringMethod.valueOf(entity.getScoringMethod()),
                entity.isAnonymousMode(),
                anonymousRoles,
                entity.getMinimumRespondents(),
                deserializeAssignments(entity.getAssignmentsJson(), CampaignId.of(entity.getId())),
                entity.getCreatedBy(),
                new Timestamp(entity.getCreatedAt()),
                new Timestamp(entity.getUpdatedAt()));
    }

    // ==================== Evaluation Mapping ====================

    public EvaluationEntity toJpaEntity(Evaluation domain) {
        var entity = new EvaluationEntity();
        entity.setId(domain.getId().value());
        entity.setCampaignId(domain.getCampaignId().value());
        entity.setAssignmentId(domain.getAssignmentId());
        entity.setEvaluatorId(domain.getEvaluatorId());
        entity.setEvaluateeId(domain.getEvaluateeId());
        entity.setTemplateId(domain.getTemplateId());
        entity.setStatus(domain.getStatus().name());
        entity.setAnswersJson(toJson(domain.getAnswers()));
        entity.setTotalScore(domain.getTotalScore() != null ? domain.getTotalScore().value().doubleValue() : null);
        entity.setSectionScoresJson(toJson(domain.getSectionScores()));
        entity.setCreatedAt(domain.getCreatedAt().value());
        entity.setUpdatedAt(domain.getUpdatedAt().value());
        entity.setSubmittedAt(domain.getSubmittedAt() != null ? domain.getSubmittedAt().value() : null);
        return entity;
    }

    public Evaluation toDomainEvaluation(EvaluationEntity entity) {
        return new Evaluation(
                EvaluationId.of(entity.getId()),
                CampaignId.of(entity.getCampaignId()),
                entity.getAssignmentId(),
                entity.getEvaluatorId(),
                entity.getEvaluateeId(),
                entity.getTemplateId(),
                EvaluationStatus.valueOf(entity.getStatus()),
                deserializeAnswers(entity.getAnswersJson()),
                entity.getTotalScore() != null ? Score.of(entity.getTotalScore()) : null,
                deserializeSectionScores(entity.getSectionScoresJson()),
                new Timestamp(entity.getCreatedAt()),
                new Timestamp(entity.getUpdatedAt()),
                entity.getSubmittedAt() != null ? new Timestamp(entity.getSubmittedAt()) : null);
    }

    // ==================== JSON Helpers ====================

    private String serializeSections(List<Section> sections) {
        if (sections == null || sections.isEmpty())
            return "[]";
        List<Map<String, Object>> serialized = sections.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("title", s.getTitle());
            map.put("description", s.getDescription());
            map.put("orderIndex", s.getOrderIndex());
            map.put("weight", s.getWeight().value().doubleValue());
            map.put("questions", s.getQuestions().stream().map(this::questionToMap).toList());
            return map;
        }).toList();
        return toJson(serialized);
    }

    private Map<String, Object> questionToMap(Question q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("text", q.getText());
        map.put("type", q.getType().name());
        map.put("orderIndex", q.getOrderIndex());
        map.put("required", q.isRequired());
        map.put("options", q.getOptions());
        map.put("weight", q.getWeight().value().doubleValue());
        map.put("metadata", q.getMetadata());
        map.put("conditionalLogic", q.getConditionalLogic());
        return map;
    }

    @SuppressWarnings("unchecked")
    private List<Section> deserializeSections(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            List<Map<String, Object>> raw = JSON.readValue(json, new TypeReference<>() {
            });
            return raw.stream().map(map -> {
                List<Map<String, Object>> questionsRaw = (List<Map<String, Object>>) map.getOrDefault("questions",
                        List.of());
                List<Question> questions = questionsRaw.stream().map(this::mapToQuestion).toList();
                return new Section(
                        (String) map.get("id"),
                        (String) map.get("title"),
                        (String) map.get("description"),
                        ((Number) map.getOrDefault("orderIndex", 0)).intValue(),
                        Weight.of(((Number) map.getOrDefault("weight", 1.0)).doubleValue()),
                        questions);
            }).toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize sections", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Question mapToQuestion(Map<String, Object> map) {
        return new Question(
                (String) map.get("id"),
                (String) map.get("text"),
                QuestionType.valueOf((String) map.get("type")),
                ((Number) map.getOrDefault("orderIndex", 0)).intValue(),
                (Boolean) map.getOrDefault("required", true),
                (List<String>) map.get("options"),
                Weight.of(((Number) map.getOrDefault("weight", 1.0)).doubleValue()),
                (Map<String, Object>) map.get("metadata"),
                (String) map.get("conditionalLogic"));
    }

    private String serializeAssignments(List<CampaignAssignment> assignments) {
        if (assignments == null || assignments.isEmpty())
            return "[]";
        List<Map<String, Object>> serialized = assignments.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("evaluatorId", a.getEvaluatorId());
            map.put("evaluateeId", a.getEvaluateeId());
            map.put("evaluatorRole", a.getEvaluatorRole().name());
            map.put("completed", a.isCompleted());
            map.put("evaluationId", a.getEvaluationId());
            return map;
        }).toList();
        return toJson(serialized);
    }

    @SuppressWarnings("unchecked")
    private List<CampaignAssignment> deserializeAssignments(String json, CampaignId campaignId) {
        if (json == null || json.isBlank())
            return new ArrayList<>();
        try {
            List<Map<String, Object>> raw = JSON.readValue(json, new TypeReference<>() {
            });
            return raw.stream().map(map -> new CampaignAssignment(
                    (String) map.get("id"),
                    campaignId,
                    (String) map.get("evaluatorId"),
                    (String) map.get("evaluateeId"),
                    EvaluatorRole.valueOf((String) map.get("evaluatorRole")),
                    (Boolean) map.getOrDefault("completed", false),
                    (String) map.get("evaluationId"))).collect(Collectors.toCollection(ArrayList::new));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize assignments", e);
        }
    }

    private <E extends Enum<E>> String serializeEnumSet(Set<E> enumSet) {
        if (enumSet == null || enumSet.isEmpty())
            return "[]";
        return toJson(enumSet.stream().map(Enum::name).toList());
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> Set<E> deserializeEnumSet(String json, Class<E> enumClass) {
        if (json == null || json.isBlank())
            return EnumSet.noneOf(enumClass);
        try {
            List<String> names = JSON.readValue(json, new TypeReference<>() {
            });
            Set<E> result = EnumSet.noneOf(enumClass);
            for (String name : names) {
                result.add(Enum.valueOf(enumClass, name));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize enum set", e);
        }
    }

    private List<Answer> deserializeAnswers(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return JSON.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize answers", e);
        }
    }

    private List<SectionScore> deserializeSectionScores(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return JSON.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize section scores", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return JSON.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
