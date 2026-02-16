package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.SystemSettingEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingEntity, String> {

    List<SystemSettingEntity> findByCategory(String category);
}
