package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.QuestionType;
import com.evaluationservice.domain.value.Weight;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a question within an evaluation template section.
 * Questions are immutable — modifications create new template versions.
 */
public final class Question {

    private final String id;
    private final String text;
    private final QuestionType type;
    private final int orderIndex;
    private final boolean required;
    private final List<String> options;
    private final Weight weight;
    private final Map<String, Object> metadata;
    private final String conditionalLogic;

    public Question(
            String id,
            String text,
            QuestionType type,
            int orderIndex,
            boolean required,
            List<String> options,
            Weight weight,
            Map<String, Object> metadata,
            String conditionalLogic) {
        this.id = Objects.requireNonNull(id, "Question ID cannot be null");
        this.text = Objects.requireNonNull(text, "Question text cannot be null");
        this.type = Objects.requireNonNull(type, "Question type cannot be null");
        this.orderIndex = orderIndex;
        this.required = required;
        this.options = options != null ? List.copyOf(options) : List.of();
        this.weight = weight != null ? weight : Weight.EQUAL;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.conditionalLogic = conditionalLogic;

        if (text.isBlank()) {
            throw new IllegalArgumentException("Question text cannot be blank");
        }
        if (type.requiresOptions() && this.options.isEmpty()) {
            throw new IllegalArgumentException("Question type " + type + " requires options");
        }
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public boolean isRequired() {
        return required;
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Weight getWeight() {
        return weight;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public String getConditionalLogic() {
        return conditionalLogic;
    }

    public boolean hasConditionalLogic() {
        return conditionalLogic != null && !conditionalLogic.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Question question = (Question) o;
        return id.equals(question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}