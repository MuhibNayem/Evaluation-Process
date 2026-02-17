package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for system settings persistence.
 */
public interface SystemSettingsPersistencePort {

    List<SystemSetting> findAll();

    Optional<SystemSetting> findByKey(String settingKey);

    List<SystemSetting> findByCategory(SystemSettingCategory category);

    SystemSetting save(SystemSetting setting);

    void deleteByKey(String settingKey);
}
