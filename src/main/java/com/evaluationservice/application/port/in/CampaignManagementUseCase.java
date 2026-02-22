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
import java.util.Map;
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
                        String audienceSourceType,
                        Map<String, Object> audienceSourceConfig,
                        String assignmentRuleType,
                        Map<String, Object> assignmentRuleConfig,
                        int minimumRespondents,
                        String createdBy) {
        }

        record AssignmentEntry(
                        String evaluatorId,
                        String evaluateeId,
                        EvaluatorRole evaluatorRole) {
        }

        record UpdateCampaignCommand(
                        com.evaluationservice.domain.value.CampaignId campaignId,
                        String name,
                        String description,
                        DateRange dateRange,
                        ScoringMethod scoringMethod,
                        boolean anonymousMode,
                        Set<EvaluatorRole> anonymousRoles,
                        String audienceSourceType,
                        Map<String, Object> audienceSourceConfig,
                        String assignmentRuleType,
                        Map<String, Object> assignmentRuleConfig,
                        int minimumRespondents) {
        }

        record DynamicAssignmentCommand(
                        String audienceSourceType,
                        Map<String, Object> audienceSourceConfig,
                        String assignmentRuleType,
                        Map<String, Object> assignmentRuleConfig,
                        boolean replaceExistingAssignments,
                        boolean dryRun) {
        }

        record DynamicAssignmentResult(
                        Campaign campaign,
                        List<CampaignAssignment> generatedAssignments,
                        String audienceSourceType,
                        String assignmentRuleType,
                        boolean replaceExistingAssignments,
                        boolean dryRun) {
        }

        record LifecycleImpactPreview(
                        CampaignId campaignId,
                        String action,
                        long totalAssignments,
                        long completedAssignments,
                        long pendingAssignments,
                        String summary) {
        }

        Campaign createCampaign(CreateCampaignCommand command);

        Campaign updateCampaign(UpdateCampaignCommand command);

        Campaign activateCampaign(CampaignId campaignId);

        Campaign closeCampaign(CampaignId campaignId);

        Campaign closeCampaign(CampaignId campaignId, String actor, String reason);

        Campaign publishCampaign(CampaignId campaignId);

        Campaign publishCampaign(CampaignId campaignId, String actor, String reason);

        Campaign reopenCampaign(CampaignId campaignId);

        Campaign reopenCampaign(CampaignId campaignId, String actor, String reason);

        Campaign publishResults(CampaignId campaignId);

        Campaign publishResults(CampaignId campaignId, String actor, String reason);

        LifecycleImpactPreview previewLifecycleImpact(CampaignId campaignId, String action);

        Campaign archiveCampaign(CampaignId campaignId);

        Campaign extendDeadline(CampaignId campaignId, Instant newEndDate);

        Campaign addAssignments(CampaignId campaignId, List<AssignmentEntry> assignments);

        DynamicAssignmentResult generateDynamicAssignments(CampaignId campaignId, DynamicAssignmentCommand command);

        Campaign getCampaign(CampaignId campaignId);

        List<Campaign> listCampaigns(String status, int page, int size);

        double getCampaignProgress(CampaignId campaignId);

        List<Campaign> listCampaignsForEvaluator(String evaluatorId);
}
