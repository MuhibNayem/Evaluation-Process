package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {
    List<NotificationTemplateEntity> findByCampaignIdOrderByUpdatedAtDesc(String campaignId);

    Optional<NotificationTemplateEntity> findByCampaignIdAndTemplateCode(String campaignId, String templateCode);

    Optional<NotificationTemplateEntity> findByTemplateCode(String templateCode);
}
