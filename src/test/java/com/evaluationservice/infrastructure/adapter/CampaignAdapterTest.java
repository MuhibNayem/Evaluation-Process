package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.mapper.DomainEntityMapper;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CampaignAdapter")
class CampaignAdapterTest {

    @Test
    @DisplayName("uses JSON path for countTotalAssignments when storage mode is JSON")
    void usesJsonCountPathInJsonMode() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        DomainEntityMapper mapper = mock(DomainEntityMapper.class);
        EvaluationServiceProperties props = properties(EvaluationServiceProperties.AssignmentStorageMode.JSON);

        when(campaignRepository.countTotalAssignments()).thenReturn(12L);

        CampaignAdapter adapter = new CampaignAdapter(campaignRepository, assignmentRepository, mapper, props);
        long total = adapter.countTotalAssignments();

        assertThat(total).isEqualTo(12L);
        verify(campaignRepository).countTotalAssignments();
        verify(assignmentRepository, never()).count();
    }

    @Test
    @DisplayName("uses relational path for countTotalAssignments when storage mode is V2")
    void usesRelationalCountPathInV2Mode() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        DomainEntityMapper mapper = mock(DomainEntityMapper.class);
        EvaluationServiceProperties props = properties(EvaluationServiceProperties.AssignmentStorageMode.V2);

        when(assignmentRepository.count()).thenReturn(21L);

        CampaignAdapter adapter = new CampaignAdapter(campaignRepository, assignmentRepository, mapper, props);
        long total = adapter.countTotalAssignments();

        assertThat(total).isEqualTo(21L);
        verify(assignmentRepository).count();
        verify(campaignRepository, never()).countTotalAssignments();
    }

    @Test
    @DisplayName("uses JSON evaluator lookup in JSON mode")
    void usesJsonEvaluatorLookupInJsonMode() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        DomainEntityMapper mapper = mock(DomainEntityMapper.class);
        EvaluationServiceProperties props = properties(EvaluationServiceProperties.AssignmentStorageMode.JSON);

        CampaignEntity entity = campaignEntity("c1");
        Campaign mapped = campaign("c1");
        when(campaignRepository.findByEvaluatorId("u1")).thenReturn(List.of(entity));
        when(mapper.toDomainCampaign(entity)).thenReturn(mapped);

        CampaignAdapter adapter = new CampaignAdapter(campaignRepository, assignmentRepository, mapper, props);
        List<Campaign> campaigns = adapter.findByEvaluatorId("u1");

        assertThat(campaigns).hasSize(1);
        verify(campaignRepository).findByEvaluatorId("u1");
        verify(assignmentRepository, never()).findDistinctCampaignIdsByEvaluatorId(anyString());
    }

    @Test
    @DisplayName("uses relational evaluator lookup in DUAL mode")
    void usesRelationalEvaluatorLookupInDualMode() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        DomainEntityMapper mapper = mock(DomainEntityMapper.class);
        EvaluationServiceProperties props = properties(EvaluationServiceProperties.AssignmentStorageMode.DUAL);

        CampaignEntity entity = campaignEntity("c2");
        Campaign mapped = campaign("c2");
        when(assignmentRepository.findDistinctCampaignIdsByEvaluatorId("u9")).thenReturn(List.of("c2"));
        when(campaignRepository.findByIdIn(List.of("c2"))).thenReturn(List.of(entity));
        when(mapper.toDomainCampaign(entity)).thenReturn(mapped);
        when(assignmentRepository.findByCampaignId("c2")).thenReturn(List.of());

        CampaignAdapter adapter = new CampaignAdapter(campaignRepository, assignmentRepository, mapper, props);
        List<Campaign> campaigns = adapter.findByEvaluatorId("u9");

        assertThat(campaigns).hasSize(1);
        verify(assignmentRepository).findDistinctCampaignIdsByEvaluatorId("u9");
        verify(campaignRepository).findByIdIn(List.of("c2"));
        verify(campaignRepository, never()).findByEvaluatorId(anyString());
    }

    @Test
    @DisplayName("uses legacy JSON assignments for findById when storage mode is JSON")
    void usesLegacyAssignmentsInJsonModeForFindById() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        DomainEntityMapper mapper = mock(DomainEntityMapper.class);
        EvaluationServiceProperties props = properties(EvaluationServiceProperties.AssignmentStorageMode.JSON);

        CampaignEntity entity = campaignEntity("c-json");
        Campaign mapped = campaign("c-json");
        when(campaignRepository.findById("c-json")).thenReturn(java.util.Optional.of(entity));
        when(mapper.toDomainCampaign(entity)).thenReturn(mapped);

        CampaignAdapter adapter = new CampaignAdapter(campaignRepository, assignmentRepository, mapper, props);
        Campaign found = adapter.findById(CampaignId.of("c-json")).orElseThrow();

        assertThat(found.getId().value()).isEqualTo("c-json");
        verify(assignmentRepository, never()).findByCampaignId(anyString());
    }

    private EvaluationServiceProperties properties(EvaluationServiceProperties.AssignmentStorageMode mode) {
        EvaluationServiceProperties props = new EvaluationServiceProperties();
        props.getAssignment().setStorageMode(mode);
        return props;
    }

    private Campaign campaign(String id) {
        return new Campaign(
                CampaignId.of(id),
                "Campaign " + id,
                null,
                TemplateId.of("t1"),
                1,
                CampaignStatus.DRAFT,
                DateRange.of(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z")),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                EnumSet.noneOf(com.evaluationservice.domain.enums.EvaluatorRole.class),
                1,
                "INLINE",
                Map.of(),
                "ALL_TO_ALL",
                Map.of(),
                List.of(),
                "tester",
                Timestamp.now(),
                Timestamp.now());
    }

    private CampaignEntity campaignEntity(String id) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(id);
        entity.setName("Campaign " + id);
        entity.setTemplateId("t1");
        entity.setTemplateVersion(1);
        entity.setStatus(CampaignStatus.DRAFT.name());
        entity.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setEndDate(Instant.parse("2026-12-31T23:59:59Z"));
        entity.setScoringMethod(ScoringMethod.WEIGHTED_AVERAGE.name());
        entity.setAnonymousRolesJson("[]");
        entity.setMinimumRespondents(1);
        entity.setAudienceSourceType("INLINE");
        entity.setAudienceSourceConfigJson("{}");
        entity.setAssignmentRuleType("ALL_TO_ALL");
        entity.setAssignmentRuleConfigJson("{}");
        entity.setAssignmentsJson("[]");
        entity.setCreatedBy("tester");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
