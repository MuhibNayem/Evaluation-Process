package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.value.CampaignId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for campaign-level setting overrides persistence.
 */
public interface CampaignSettingsPersistencePort {

    List<CampaignSettingOverride> findByCampaignId(CampaignId campaignId);

    Optional<CampaignSettingOverride> findByCampaignIdAndKey(CampaignId campaignId, String settingKey);

    CampaignSettingOverride save(CampaignSettingOverride override);

    void deleteByCampaignIdAndKey(CampaignId campaignId, String settingKey);
}
