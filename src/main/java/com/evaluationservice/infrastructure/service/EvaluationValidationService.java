package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.SubmissionValidationResponse;
import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.QuestionType;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EvaluationValidationService {

    private final TemplatePersistencePort templatePersistencePort;

    public EvaluationValidationService(TemplatePersistencePort templatePersistencePort) {
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
    }

    @Transactional(readOnly = true)
    public SubmissionValidationResponse validate(String templateId, List<Answer> answers) {
        Template template = templatePersistencePort.findById(TemplateId.of(templateId))
                .orElseThrow(() -> new EntityNotFoundException("Template", templateId));

        Map<String, Question> questionsById = new LinkedHashMap<>();
        for (var section : template.getSections()) {
            for (Question question : section.getQuestions()) {
                questionsById.put(question.getId(), question);
            }
        }

        Map<String, Answer> answersByQuestionId = new LinkedHashMap<>();
        List<SubmissionValidationResponse.ValidationIssue> issues = new ArrayList<>();
        for (Answer answer : answers == null ? List.<Answer>of() : answers) {
            answersByQuestionId.put(answer.questionId(), answer);
            if (!questionsById.containsKey(answer.questionId())) {
                issues.add(new SubmissionValidationResponse.ValidationIssue(
                        answer.questionId(),
                        "UNKNOWN_QUESTION",
                        "Answer references unknown questionId"));
            }
        }

        for (Question question : questionsById.values()) {
            Answer answer = answersByQuestionId.get(question.getId());
            if (question.isRequired() && !isAnswered(answer)) {
                issues.add(new SubmissionValidationResponse.ValidationIssue(
                        question.getId(),
                        "REQUIRED_MISSING",
                        "Required question is missing an answer"));
            }
            if (isRemarksMandatory(question) && (answer == null || !answer.hasTextResponse())) {
                issues.add(new SubmissionValidationResponse.ValidationIssue(
                        question.getId(),
                        "REMARKS_REQUIRED",
                        "Remarks text is required for this question"));
            }
            if (answer != null && isAnswered(answer)) {
                validateAnswerShape(question, answer, issues);
            }
        }

        return new SubmissionValidationResponse(issues.isEmpty(), issues.size(), issues);
    }

    private boolean isAnswered(Answer answer) {
        if (answer == null) {
            return false;
        }
        if (answer.value() != null) {
            if (answer.value() instanceof String s) {
                return !s.isBlank();
            }
            return true;
        }
        if (answer.selectedOptions() != null && !answer.selectedOptions().isEmpty()) {
            return true;
        }
        return answer.hasTextResponse();
    }

    private boolean isRemarksMandatory(Question question) {
        Object camel = question.getMetadata().get("remarksMandatory");
        if (camel instanceof Boolean b) {
            return b;
        }
        Object snake = question.getMetadata().get("remarks_mandatory");
        return snake instanceof Boolean b && b;
    }

    private void validateAnswerShape(
            Question question,
            Answer answer,
            List<SubmissionValidationResponse.ValidationIssue> issues) {
        QuestionType type = question.getType();
        switch (type) {
            case OPEN_TEXT -> {
                if (!answer.hasTextResponse()) {
                    issues.add(issue(question, "TEXT_REQUIRED", "Open text answer is required"));
                }
            }
            case BOOLEAN -> {
                if (!(answer.value() instanceof Boolean)) {
                    issues.add(issue(question, "BOOLEAN_REQUIRED", "Boolean value is required"));
                }
            }
            case SINGLE_CHOICE -> {
                String value = asNonBlankString(answer.value());
                if (value == null) {
                    issues.add(issue(question, "CHOICE_REQUIRED", "Single choice value is required"));
                } else if (!question.getOptions().contains(value)) {
                    issues.add(issue(question, "CHOICE_INVALID", "Selected choice is not in allowed options"));
                }
            }
            case MULTIPLE_CHOICE, RANKING -> {
                if (answer.selectedOptions() == null || answer.selectedOptions().isEmpty()) {
                    issues.add(issue(question, "OPTIONS_REQUIRED", "At least one option must be selected"));
                } else {
                    boolean invalid = answer.selectedOptions().stream().anyMatch(o -> !question.getOptions().contains(o));
                    if (invalid) {
                        issues.add(issue(question, "OPTION_INVALID", "One or more selected options are invalid"));
                    }
                }
            }
            case LIKERT_SCALE, NUMERIC_RATING, NPS -> {
                if (!(answer.value() instanceof Number number)) {
                    issues.add(issue(question, "NUMERIC_REQUIRED", "Numeric value is required"));
                } else if (type == QuestionType.NPS && (number.doubleValue() < 0 || number.doubleValue() > 10)) {
                    issues.add(issue(question, "NPS_OUT_OF_RANGE", "NPS value must be between 0 and 10"));
                }
            }
            default -> {
                // MATRIX and FILE_UPLOAD are currently validated as present only.
            }
        }
    }

    private SubmissionValidationResponse.ValidationIssue issue(Question question, String code, String message) {
        return new SubmissionValidationResponse.ValidationIssue(question.getId(), code, message);
    }

    private String asNonBlankString(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
