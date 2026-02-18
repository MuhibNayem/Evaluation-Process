package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "evaluation.service.security.dev-mode=true",
        "spring.datasource.url=jdbc:h2:mem:assignment-backfill-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("AssignmentBackfillService Integration")
class AssignmentBackfillServiceIntegrationTest {

    @Autowired
    private AssignmentBackfillService backfillService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAssignmentRepository assignmentRepository;

    @BeforeEach
    void setup() {
        assignmentRepository.deleteAll();
        campaignRepository.deleteAll();
    }

    @Test
    @DisplayName("backfills assignments at scale from legacy campaign JSON")
    void backfillsAssignmentsAtScale() {
        int campaigns = 120;
        int assignmentsPerCampaign = 15;
        for (int i = 0; i < campaigns; i++) {
            campaignRepository.save(campaignEntity(i, assignmentsPerCampaign));
        }

        var firstRun = backfillService.backfill(false, 1000);
        assertThat(firstRun.scannedCampaigns()).isEqualTo(campaigns);
        assertThat(firstRun.insertedAssignments()).isEqualTo(campaigns * assignmentsPerCampaign);
        assertThat(assignmentRepository.count()).isEqualTo((long) campaigns * assignmentsPerCampaign);

        var secondRun = backfillService.backfill(false, 1000);
        assertThat(secondRun.insertedAssignments()).isZero();
        assertThat(secondRun.skippedExistingAssignments()).isEqualTo(campaigns * assignmentsPerCampaign);
    }

    private CampaignEntity campaignEntity(int index, int assignmentsPerCampaign) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId("camp-bf-" + index);
        campaign.setName("Backfill Campaign " + index);
        campaign.setTemplateId("tmpl-bf-1");
        campaign.setTemplateVersion(1);
        campaign.setStatus("ACTIVE");
        campaign.setStartDate(Instant.now().minusSeconds(3600));
        campaign.setEndDate(Instant.now().plusSeconds(86400));
        campaign.setScoringMethod("WEIGHTED_AVERAGE");
        campaign.setAnonymousMode(false);
        campaign.setAnonymousRolesJson("[]");
        campaign.setMinimumRespondents(1);
        campaign.setAudienceSourceType("INLINE");
        campaign.setAudienceSourceConfigJson("{}");
        campaign.setAssignmentRuleType("ALL_TO_ALL");
        campaign.setAssignmentRuleConfigJson("{}");
        campaign.setAssignmentsJson(assignmentsJson(index, assignmentsPerCampaign));
        campaign.setCreatedBy("it-test");
        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        return campaign;
    }

    private String assignmentsJson(int campaignIndex, int assignmentsPerCampaign) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < assignmentsPerCampaign; i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append("""
                    {"id":"a-%d-%d","evaluatorId":"ev-%d","evaluateeId":"ee-%d","evaluatorRole":"PEER","completed":false}
                    """.formatted(campaignIndex, i, i, i));
        }
        out.append("]");
        return out.toString();
    }
}
