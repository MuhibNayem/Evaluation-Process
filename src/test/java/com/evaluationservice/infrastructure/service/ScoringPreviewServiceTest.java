package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.AnswerRequest;
import com.evaluationservice.api.dto.request.ScoringPreviewRequest;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.application.service.ScoringService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringPreviewServiceTest {

    @Mock
    private TemplatePersistencePort templatePersistencePort;

    private ScoringPreviewService service;

    @BeforeEach
    void setUp() {
        service = new ScoringPreviewService(templatePersistencePort, new ScoringService(), new EvaluationValidationService(templatePersistencePort));
    }

    @Test
    void returnsPreviewForValidAnswers() {
        when(templatePersistencePort.findById(TemplateId.of("t1"))).thenReturn(Optional.of(template()));
        ScoringPreviewRequest request = new ScoringPreviewRequest(
                "t1",
                ScoringMethod.WEIGHTED_AVERAGE,
                null,
                List.of(new AnswerRequest("q1", 8, List.of(), null, Map.of())));

        var response = service.preview(request);
        assertEquals("t1", response.templateId());
        assertEquals(1, response.sections().size());
    }

    @Test
    void rejectsInvalidPreviewPayload() {
        when(templatePersistencePort.findById(TemplateId.of("t1"))).thenReturn(Optional.of(template()));
        ScoringPreviewRequest request = new ScoringPreviewRequest(
                "t1",
                null,
                null,
                List.of(new AnswerRequest("q1", "bad", List.of(), null, Map.of())));

        assertThrows(IllegalArgumentException.class, () -> service.preview(request));
    }

    private Template template() {
        Question q1 = new Question("q1", "Rate", QuestionType.NUMERIC_RATING, 1, true, List.of(), Weight.EQUAL, Map.of(), null);
        Section s1 = new Section("s1", "Section", null, 1, Weight.EQUAL, List.of(q1));
        return new Template(
                TemplateId.of("t1"),
                "Template",
                null,
                "CAT",
                TemplateStatus.PUBLISHED,
                1,
                ScoringMethod.WEIGHTED_AVERAGE,
                List.of(s1),
                "admin",
                Timestamp.now(),
                Timestamp.now(),
                null);
    }
}
