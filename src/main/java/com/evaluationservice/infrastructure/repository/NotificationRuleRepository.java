package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.NotificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, Long> {
    List<NotificationRuleEntity> findByCampaignIdOrderByUpdatedAtDesc(String campaignId);

    List<NotificationRuleEntity> findByEnabledTrueOrderByUpdatedAtDesc();

    List<NotificationRuleEntity> findByEnabledTrueAndTriggerTypeOrderByUpdatedAtDesc(String triggerType);

    Optional<NotificationRuleEntity> findByCampaignIdAndRuleCode(String campaignId, String ruleCode);
}
