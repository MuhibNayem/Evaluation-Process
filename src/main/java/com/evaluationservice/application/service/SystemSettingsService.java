package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.SystemSettingsUseCase;
import com.evaluationservice.application.port.out.CampaignSettingsPersistencePort;
import com.evaluationservice.application.port.out.SystemSettingsPersistencePort;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application service for admin CRUD operations on system settings
 * and campaign-level overrides.
 */
@Service
@Transactional
public class SystemSettingsService implements SystemSettingsUseCase {

    private final SystemSettingsPersistencePort systemSettingsPort;
    private final CampaignSettingsPersistencePort campaignSettingsPort;

    public SystemSettingsService(
            SystemSettingsPersistencePort systemSettingsPort,
            CampaignSettingsPersistencePort campaignSettingsPort) {
        this.systemSettingsPort = Objects.requireNonNull(systemSettingsPort);
        this.campaignSettingsPort = Objects.requireNonNull(campaignSettingsPort);
    }

    // --- System-wide Settings ---

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return systemSettingsPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getSettingsByCategory(SystemSettingCategory category) {
        return systemSettingsPort.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSetting getSettingByKey(String key) {
        return systemSettingsPort.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SystemSetting", key));
    }

    @Override
    public SystemSetting updateSetting(String key, String value, String updatedBy) {
        SystemSetting setting = systemSettingsPort.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SystemSetting", key));
        setting.updateValue(value, updatedBy);
        return systemSettingsPort.save(setting);
    }

    // --- Campaign Overrides ---

    @Override
    @Transactional(readOnly = true)
    public List<CampaignSettingOverride> getCampaignOverrides(CampaignId campaignId) {
        return campaignSettingsPort.findByCampaignId(campaignId);
    }

    @Override
    public CampaignSettingOverride setCampaignOverride(
            CampaignId campaignId, String key, String value, String updatedBy) {
        // Validate the key exists in system settings
        if (systemSettingsPort.findByKey(key).isEmpty()) {
            throw new EntityNotFoundException("SystemSetting", key);
        }

        CampaignSettingOverride override = campaignSettingsPort
                .findByCampaignIdAndKey(campaignId, key)
                .map(existing -> {
                    existing.updateValue(value, updatedBy);
                    return existing;
                })
                .orElseGet(() -> new CampaignSettingOverride(
                        campaignId, key, value, updatedBy, null));

        return campaignSettingsPort.save(override);
    }

    @Override
    public void removeCampaignOverride(CampaignId campaignId, String key) {
        campaignSettingsPort.deleteByCampaignIdAndKey(campaignId, key);
    }
}
