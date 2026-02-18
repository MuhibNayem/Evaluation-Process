package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AssignmentReconciliationResponse;
import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AssignmentParityReportService")
class AssignmentParityReportServiceTest {

    @Test
    @DisplayName("aggregates reconciliation results across campaigns")
    void aggregatesReconciliationResults() {
        CampaignPersistencePort campaignPort = mock(CampaignPersistencePort.class);
        AssignmentReconciliationService reconciliationService = mock(AssignmentReconciliationService.class);
        AssignmentParityReportService service = new AssignmentParityReportService(campaignPort, reconciliationService);

        Campaign c1 = campaign("c1");
        Campaign c2 = campaign("c2");
        when(campaignPort.findAll(0, 100)).thenReturn(List.of(c1, c2));
        when(campaignPort.findAll(1, 100)).thenReturn(List.of());
        when(reconciliationService.reconcile("c1")).thenReturn(new AssignmentReconciliationResponse(
                "c1",
                10,
                10,
                0,
                0,
                0,
                true,
                List.of(),
                List.of(),
                List.of()));
        when(reconciliationService.reconcile("c2")).thenReturn(new AssignmentReconciliationResponse(
                "c2",
                10,
                9,
                1,
                0,
                2,
                false,
                List.of("a"),
                List.of(),
                List.of("b", "c")));

        var report = service.buildReport(10);

        assertThat(report.scannedCampaigns()).isEqualTo(2);
        assertThat(report.consistentCampaigns()).isEqualTo(1);
        assertThat(report.inconsistentCampaigns()).isEqualTo(1);
        assertThat(report.totalOnlyInLegacy()).isEqualTo(1);
        assertThat(report.totalOnlyInRelational()).isZero();
        assertThat(report.totalCompletionMismatches()).isEqualTo(2);
        assertThat(report.inconsistentCampaignIds()).containsExactly("c2");
    }

    private Campaign campaign(String id) {
        return new Campaign(
                CampaignId.of(id),
                "Campaign " + id,
                null,
                TemplateId.of("tmpl-1"),
                1,
                CampaignStatus.ACTIVE,
                DateRange.of(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z")),
                ScoringMethod.WEIGHTED_AVERAGE,
                false,
                null,
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
}
