package com.evaluationservice.application.port.in;

import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.enums.EvaluatorRole;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.DateRange;
import com.evaluationservice.domain.value.TemplateId;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Inbound port for campaign management operations.
 */
public interface CampaignManagementUseCase {

    record CreateCampaignCommand(
            String name,
            String description,
            TemplateId templateId,
            int templateVersion,
            DateRange dateRange,
            ScoringMethod scoringMethod,
            boolean anonymousMode,
            Set<EvaluatorRole> anonymousRoles,
            int minimumRespondents,
            String createdBy) {
    }

    record AssignmentEntry(
            String evaluatorId,
            String evaluateeId,
            EvaluatorRole evaluatorRole) {
    }

    Campaign createCampaign(CreateCampaignCommand command);

    Campaign activateCampaign(CampaignId campaignId);

    Campaign closeCampaign(CampaignId campaignId);

    Campaign archiveCampaign(CampaignId campaignId);

    Campaign extendDeadline(CampaignId campaignId, Instant newEndDate);

    Campaign addAssignments(CampaignId campaignId, List<AssignmentEntry> assignments);

    Campaign getCampaign(CampaignId campaignId);

    List<Campaign> listCampaigns(String status, int page, int size);

    double getCampaignProgress(CampaignId campaignId);
}
