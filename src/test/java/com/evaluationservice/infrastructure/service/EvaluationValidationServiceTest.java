package com.evaluationservice.infrastructure.service;

import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.entity.Section;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.QuestionType;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.domain.value.Weight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationValidationServiceTest {

    @Mock
    private TemplatePersistencePort templatePersistencePort;

    private EvaluationValidationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationValidationService(templatePersistencePort);
    }

    @Test
    void reportsMissingRequiredAnswer() {
        Template template = templateWithQuestions();
        when(templatePersistencePort.findById(TemplateId.of("t1"))).thenReturn(Optional.of(template));

        var result = service.validate("t1", List.of());

        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> "REQUIRED_MISSING".equals(i.code())));
    }

    @Test
    void passesWhenRequiredAndRemarksProvided() {
        Template template = templateWithQuestions();
        when(templatePersistencePort.findById(TemplateId.of("t1"))).thenReturn(Optional.of(template));
        List<Answer> answers = List.of(
                new Answer("a1", "q1", 4, List.of(), null, Map.of()),
                new Answer("a2", "q2", null, List.of(), "remarks", Map.of()),
                new Answer("a3", "q3", "A", List.of(), null, Map.of()),
                new Answer("a4", "q4", null, List.of("X"), null, Map.of()),
                new Answer("a5", "q5", true, List.of(), null, Map.of()),
                new Answer("a6", "q6", 10, List.of(), null, Map.of()));

        var result = service.validate("t1", answers);

        assertTrue(result.valid());
    }

    @Test
    void reportsTypeSpecificValidationIssues() {
        Template template = templateWithQuestions();
        when(templatePersistencePort.findById(TemplateId.of("t1"))).thenReturn(Optional.of(template));
        List<Answer> answers = List.of(
                new Answer("a1", "q1", "bad", List.of(), null, Map.of()),
                new Answer("a2", "q2", "dummy", List.of(), "", Map.of()),
                new Answer("a3", "q3", "Z", List.of(), null, Map.of()),
                new Answer("a4", "q4", null, List.of("BAD"), null, Map.of()),
                new Answer("a5", "q5", "not-bool", List.of(), null, Map.of()),
                new Answer("a6", "q6", 11, List.of(), null, Map.of()));

        var result = service.validate("t1", answers);

        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> "NUMERIC_REQUIRED".equals(i.code())));
        assertTrue(result.issues().stream().anyMatch(i -> "TEXT_REQUIRED".equals(i.code())));
        assertTrue(result.issues().stream().anyMatch(i -> "CHOICE_INVALID".equals(i.code())));
        assertTrue(result.issues().stream().anyMatch(i -> "OPTION_INVALID".equals(i.code())));
        assertTrue(result.issues().stream().anyMatch(i -> "BOOLEAN_REQUIRED".equals(i.code())));
        assertTrue(result.issues().stream().anyMatch(i -> "NPS_OUT_OF_RANGE".equals(i.code())));
    }

    private Template templateWithQuestions() {
        Question q1 = new Question("q1", "Score", QuestionType.NUMERIC_RATING, 1, true, List.of(), Weight.EQUAL, Map.of(), null);
        Question q2 = new Question(
                "q2",
                "Comments",
                QuestionType.OPEN_TEXT,
                2,
                false,
                List.of(),
                Weight.EQUAL,
                Map.of("remarksMandatory", true),
                null);
        Question q3 = new Question(
                "q3",
                "Single",
                QuestionType.SINGLE_CHOICE,
                3,
                false,
                List.of("A", "B"),
                Weight.EQUAL,
                Map.of(),
                null);
        Question q4 = new Question(
                "q4",
                "Multi",
                QuestionType.MULTIPLE_CHOICE,
                4,
                false,
                List.of("X", "Y"),
                Weight.EQUAL,
                Map.of(),
                null);
        Question q5 = new Question(
                "q5",
                "Bool",
                QuestionType.BOOLEAN,
                5,
                false,
                List.of(),
                Weight.EQUAL,
                Map.of(),
                null);
        Question q6 = new Question(
                "q6",
                "NPS",
                QuestionType.NPS,
                6,
                false,
                List.of(),
                Weight.EQUAL,
                Map.of(),
                null);
        Section section = new Section("s1", "Section", null, 1, Weight.EQUAL, List.of(q1, q2, q3, q4, q5, q6));
        return new Template(
                TemplateId.of("t1"),
                "Template",
                null,
                "CAT",
                TemplateStatus.PUBLISHED,
                1,
                ScoringMethod.WEIGHTED_AVERAGE,
                List.of(section),
                "admin",
                Timestamp.now(),
                Timestamp.now(),
                null);
    }
}
