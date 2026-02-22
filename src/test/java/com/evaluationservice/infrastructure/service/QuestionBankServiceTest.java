package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateQuestionBankItemRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankItemVersionRequest;
import com.evaluationservice.api.dto.request.CreateQuestionBankSetRequest;
import com.evaluationservice.infrastructure.entity.QuestionBankItemEntity;
import com.evaluationservice.infrastructure.entity.QuestionBankItemVersionEntity;
import com.evaluationservice.infrastructure.entity.QuestionBankSetEntity;
import com.evaluationservice.infrastructure.repository.QuestionBankItemRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankItemVersionRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankSetRepository setRepository;
    @Mock
    private QuestionBankItemRepository itemRepository;
    @Mock
    private QuestionBankItemVersionRepository versionRepository;

    private QuestionBankService service;

    @BeforeEach
    void setUp() {
        service = new QuestionBankService(setRepository, itemRepository, versionRepository, new ObjectMapper());
    }

    @Test
    void createsQuestionBankSet() {
        QuestionBankSetEntity saved = new QuestionBankSetEntity();
        saved.setId(1L);
        saved.setName("Core");
        saved.setStatus("ACTIVE");
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());
        when(setRepository.save(any(QuestionBankSetEntity.class))).thenReturn(saved);

        var response = service.createSet(new CreateQuestionBankSetRequest("t1", "Core", "v1", "admin"));
        assertEquals(1L, response.id());
        assertEquals("Core", response.name());
    }

    @Test
    void rejectsDuplicateStableKey() {
        QuestionBankSetEntity set = new QuestionBankSetEntity();
        set.setId(10L);
        when(setRepository.findById(10L)).thenReturn(Optional.of(set));
        QuestionBankItemEntity existing = new QuestionBankItemEntity();
        existing.setId(7L);
        when(itemRepository.findBySetIdAndStableKey(10L, "Q1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.createItem(
                10L,
                new CreateQuestionBankItemRequest("Q1", null, null, "NUMERIC_RATING", BigDecimal.ONE)));
    }

    @Test
    void activatingVersionUpdatesActiveVersionNo() {
        QuestionBankItemEntity item = new QuestionBankItemEntity();
        item.setId(5L);
        item.setActiveVersionNo(1);
        item.setDefaultType("OPEN_TEXT");
        item.setDefaultMarks(BigDecimal.ONE);
        QuestionBankItemVersionEntity version = new QuestionBankItemVersionEntity();
        version.setId(11L);
        version.setQuestionItemId(5L);
        version.setVersionNo(2);
        version.setStatus("DRAFT");
        version.setQuestionType("NPS");
        version.setMarks(BigDecimal.TEN);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(versionRepository.findByQuestionItemIdAndVersionNo(5L, 2)).thenReturn(Optional.of(version));
        when(versionRepository.findByQuestionItemIdAndStatusOrderByVersionNoDesc(5L, "ACTIVE")).thenReturn(List.of());
        when(versionRepository.save(any(QuestionBankItemVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.save(any(QuestionBankItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.activateVersion(5L, 2);
        assertEquals("ACTIVE", response.status());
        assertEquals(2, item.getActiveVersionNo());
        assertEquals("NPS", item.getDefaultType());
    }

    @Test
    void compareVersionsReturnsDiffs() {
        QuestionBankItemVersionEntity from = new QuestionBankItemVersionEntity();
        from.setQuestionItemId(3L);
        from.setVersionNo(1);
        from.setStatus("DRAFT");
        from.setQuestionText("Old");
        from.setQuestionType("OPEN_TEXT");
        from.setMarks(BigDecimal.ONE);
        from.setRemarksMandatory(false);
        from.setMetadataJson("{\"a\":1}");
        QuestionBankItemVersionEntity to = new QuestionBankItemVersionEntity();
        to.setQuestionItemId(3L);
        to.setVersionNo(2);
        to.setStatus("ACTIVE");
        to.setQuestionText("New");
        to.setQuestionType("OPEN_TEXT");
        to.setMarks(BigDecimal.TEN);
        to.setRemarksMandatory(true);
        to.setMetadataJson("{\"a\":2}");
        when(versionRepository.findByQuestionItemIdAndVersionNo(3L, 1)).thenReturn(Optional.of(from));
        when(versionRepository.findByQuestionItemIdAndVersionNo(3L, 2)).thenReturn(Optional.of(to));

        var response = service.compareVersions(3L, 1, 2);
        assertEquals(3L, response.questionItemId());
        assertEquals(1, response.fromVersion());
        assertEquals(2, response.toVersion());
    }

    @Test
    void createVersionRejectsInvalidQuestionType() {
        QuestionBankItemEntity item = new QuestionBankItemEntity();
        item.setId(5L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        assertThrows(IllegalArgumentException.class, () -> service.createVersion(
                5L,
                new CreateQuestionBankItemVersionRequest(
                        "DRAFT", null, "Text", "BAD_TYPE", BigDecimal.ONE, false, Map.of())));
    }
}
