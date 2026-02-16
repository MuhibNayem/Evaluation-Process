package com.evaluationservice.application.port.in;

import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.value.CampaignId;

import java.util.List;

/**
 * Inbound port for system settings management (admin operations).
 */
public interface SystemSettingsUseCase {

    // --- System-wide Settings ---

    List<SystemSetting> getAllSettings();

    List<SystemSetting> getSettingsByCategory(SystemSettingCategory category);

    SystemSetting getSettingByKey(String key);

    SystemSetting updateSetting(String key, String value, String updatedBy);

    // --- Campaign Overrides ---

    List<CampaignSettingOverride> getCampaignOverrides(CampaignId campaignId);

    CampaignSettingOverride setCampaignOverride(CampaignId campaignId, String key, String value, String updatedBy);

    void removeCampaignOverride(CampaignId campaignId, String key);
}
