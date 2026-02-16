package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.value.Weight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a section within an evaluation template.
 * Sections group related questions and have their own weight for scoring.
 */
public final class Section {

    private final String id;
    private final String title;
    private final String description;
    private final int orderIndex;
    private final Weight weight;
    private final List<Question> questions;

    public Section(
            String id,
            String title,
            String description,
            int orderIndex,
            Weight weight,
            List<Question> questions) {
        this.id = Objects.requireNonNull(id, "Section ID cannot be null");
        this.title = Objects.requireNonNull(title, "Section title cannot be null");
        this.description = description;
        this.orderIndex = orderIndex;
        this.weight = weight != null ? weight : Weight.EQUAL;
        this.questions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();

        if (title.isBlank()) {
            throw new IllegalArgumentException("Section title cannot be blank");
        }
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public Weight getWeight() {
        return weight;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public int getRequiredQuestionCount() {
        return (int) questions.stream().filter(Question::isRequired).count();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Section section = (Section) o;
        return id.equals(section.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
