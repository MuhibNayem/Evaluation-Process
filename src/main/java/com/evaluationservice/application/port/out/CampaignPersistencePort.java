package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.value.CampaignId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for campaign persistence operations.
 */
public interface CampaignPersistencePort {

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(CampaignId campaignId);

    List<Campaign> findByStatus(CampaignStatus status, int page, int size);

    List<Campaign> findAll(int page, int size);

    boolean existsById(CampaignId campaignId);

    List<Campaign> findByEvaluatorId(String evaluatorId);
}
