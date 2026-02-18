package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AssignmentBackfillService")
class AssignmentBackfillServiceTest {

    @Test
    @DisplayName("backfills valid legacy assignments and skips existing ids")
    void backfillsValidAssignmentsAndSkipsExisting() {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignAssignmentRepository assignmentRepository = mock(CampaignAssignmentRepository.class);
        AssignmentBackfillService service = new AssignmentBackfillService(campaignRepository, assignmentRepository);

        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("c-1");
        campaign.setName("Campaign");
        campaign.setTemplateId("t-1");
        campaign.setTemplateVersion(1);
        campaign.setStatus("ACTIVE");
        campaign.setStartDate(Instant.now().minusSeconds(100));
        campaign.setEndDate(Instant.now().plusSeconds(100));
        campaign.setScoringMethod("WEIGHTED_AVERAGE");
        campaign.setAssignmentsJson("""
                [
                  {"id":"a-1","evaluatorId":"e1","evaluateeId":"u1","evaluatorRole":"PEER","completed":false},
                  {"id":"a-2","evaluatorId":"e2","evaluateeId":"u2","evaluatorRole":"MANAGER","completed":true},
                  {"id":"","evaluatorId":"e3","evaluateeId":"u3","evaluatorRole":"PEER"}
                ]
                """);

        when(campaignRepository.findAll(PageRequest.of(0, 100))).thenReturn(new PageImpl<>(List.of(campaign)));
        when(assignmentRepository.findAllById(List.of("a-1", "a-2"))).thenReturn(List.of(existingAssignment("a-2")));

        var result = service.backfill(false, 10);

        assertThat(result.scannedCampaigns()).isEqualTo(1);
        assertThat(result.parsedAssignments()).isEqualTo(3);
        assertThat(result.invalidAssignments()).isEqualTo(1);
        assertThat(result.skippedExistingAssignments()).isEqualTo(1);
        assertThat(result.insertedAssignments()).isEqualTo(1);
        verify(assignmentRepository).saveAll(any());
    }

    private com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity existingAssignment(String id) {
        com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity entity =
                new com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity();
        entity.setId(id);
        return entity;
    }
}
