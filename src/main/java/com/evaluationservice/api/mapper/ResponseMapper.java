package com.evaluationservice.api.mapper;

import com.evaluationservice.api.dto.response.CampaignResponse;
import com.evaluationservice.api.dto.response.EvaluationResponse;
import com.evaluationservice.api.dto.response.TemplateResponse;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.entity.Template;

import org.springframework.stereotype.Component;

/**
 * Maps domain entities to API response DTOs.
 */
@Component
public class ResponseMapper {

    public TemplateResponse toResponse(Template template) {
        return new TemplateResponse(
                template.getId().value(),
                template.getName(),
                template.getDescription(),
                template.getCategory(),
                template.getStatus(),
                template.getCurrentVersion(),
                template.getScoringMethod(),
                template.getTotalQuestionCount(),
                template.getCreatedBy(),
                template.getCreatedAt().value(),
                template.getUpdatedAt().value());
    }

    public CampaignResponse toResponse(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId().value(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getTemplateId().value(),
                campaign.getTemplateVersion(),
                campaign.getStatus(),
                campaign.getDateRange().startDate(),
                campaign.getDateRange().endDate(),
                campaign.getScoringMethod(),
                campaign.isAnonymousMode(),
                campaign.getAssignments().size(),
                campaign.getCompletedAssignmentCount(),
                campaign.getCompletionPercentage(),
                campaign.getCreatedBy(),
                campaign.getCreatedAt().value(),
                campaign.getUpdatedAt().value());
    }

    public EvaluationResponse toResponse(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId().value(),
                evaluation.getCampaignId().value(),
                evaluation.getAssignmentId(),
                evaluation.getEvaluatorId(),
                evaluation.getEvaluateeId(),
                evaluation.getStatus(),
                evaluation.getTotalScore() != null ? evaluation.getTotalScore().value().doubleValue() : null,
                evaluation.getAnswers().size(),
                evaluation.getCreatedAt().value(),
                evaluation.getSubmittedAt() != null ? evaluation.getSubmittedAt().value() : null);
    }
}
