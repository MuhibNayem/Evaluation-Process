package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignSettingOverrideEntity;
import com.evaluationservice.infrastructure.entity.CampaignSettingOverrideEntity.CampaignSettingId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignSettingsRepository
        extends JpaRepository<CampaignSettingOverrideEntity, CampaignSettingId> {

    List<CampaignSettingOverrideEntity> findByCampaignId(String campaignId);

    Optional<CampaignSettingOverrideEntity> findByCampaignIdAndSettingKey(String campaignId, String settingKey);

    void deleteByCampaignIdAndSettingKey(String campaignId, String settingKey);
}
