package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.CampaignSettingsPersistencePort;
import com.evaluationservice.application.port.out.SystemSettingsPersistencePort;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.entity.CampaignSettingOverrideEntity;
import com.evaluationservice.infrastructure.entity.SystemSettingEntity;
import com.evaluationservice.infrastructure.repository.CampaignSettingsRepository;
import com.evaluationservice.infrastructure.repository.SystemSettingsRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing both system settings and campaign settings persistence
 * ports.
 * Maps between domain entities and JPA entities.
 */
@Component
public class SystemSettingsAdapter
        implements SystemSettingsPersistencePort, CampaignSettingsPersistencePort {

    private final SystemSettingsRepository systemRepo;
    private final CampaignSettingsRepository campaignRepo;

    public SystemSettingsAdapter(
            SystemSettingsRepository systemRepo,
            CampaignSettingsRepository campaignRepo) {
        this.systemRepo = systemRepo;
        this.campaignRepo = campaignRepo;
    }

    // --- SystemSettingsPersistencePort ---

    @Override
    public List<SystemSetting> findAll() {
        return systemRepo.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SystemSetting> findByKey(String settingKey) {
        return systemRepo.findById(settingKey).map(this::toDomain);
    }

    @Override
    public List<SystemSetting> findByCategory(SystemSettingCategory category) {
        return systemRepo.findByCategory(category.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public SystemSetting save(SystemSetting setting) {
        SystemSettingEntity entity = toEntity(setting);
        return toDomain(systemRepo.save(entity));
    }

    @Override
    public void deleteByKey(String settingKey) {
        systemRepo.deleteById(settingKey);
    }

    // --- CampaignSettingsPersistencePort ---

    @Override
    public List<CampaignSettingOverride> findByCampaignId(CampaignId campaignId) {
        return campaignRepo.findByCampaignId(campaignId.value()).stream()
                .map(this::toOverrideDomain)
                .toList();
    }

    @Override
    public Optional<CampaignSettingOverride> findByCampaignIdAndKey(CampaignId campaignId, String settingKey) {
        return campaignRepo.findByCampaignIdAndSettingKey(campaignId.value(), settingKey)
                .map(this::toOverrideDomain);
    }

    @Override
    public CampaignSettingOverride save(CampaignSettingOverride override) {
        CampaignSettingOverrideEntity entity = toOverrideEntity(override);
        return toOverrideDomain(campaignRepo.save(entity));
    }

    @Override
    public void deleteByCampaignIdAndKey(CampaignId campaignId, String settingKey) {
        campaignRepo.deleteByCampaignIdAndSettingKey(campaignId.value(), settingKey);
    }

    // --- Mappers: SystemSetting ---

    private SystemSetting toDomain(SystemSettingEntity entity) {
        return new SystemSetting(
                entity.getSettingKey(),
                entity.getSettingValue(),
                SystemSettingCategory.valueOf(entity.getCategory()),
                entity.getDescription(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt());
    }

    private SystemSettingEntity toEntity(SystemSetting setting) {
        SystemSettingEntity entity = new SystemSettingEntity();
        entity.setSettingKey(setting.getSettingKey());
        entity.setSettingValue(setting.getSettingValue());
        entity.setCategory(setting.getCategory().name());
        entity.setDescription(setting.getDescription());
        entity.setUpdatedBy(setting.getUpdatedBy());
        entity.setUpdatedAt(setting.getUpdatedAt());
        return entity;
    }

    // --- Mappers: CampaignSettingOverride ---

    private CampaignSettingOverride toOverrideDomain(CampaignSettingOverrideEntity entity) {
        return new CampaignSettingOverride(
                CampaignId.of(entity.getCampaignId()),
                entity.getSettingKey(),
                entity.getSettingValue(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt());
    }

    private CampaignSettingOverrideEntity toOverrideEntity(CampaignSettingOverride override) {
        CampaignSettingOverrideEntity entity = new CampaignSettingOverrideEntity();
        entity.setCampaignId(override.getCampaignId().value());
        entity.setSettingKey(override.getSettingKey());
        entity.setSettingValue(override.getSettingValue());
        entity.setUpdatedBy(override.getUpdatedBy());
        entity.setUpdatedAt(override.getUpdatedAt());
        return entity;
    }
}
