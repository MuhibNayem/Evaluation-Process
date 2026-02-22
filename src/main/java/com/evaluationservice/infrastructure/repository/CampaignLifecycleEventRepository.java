package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignLifecycleEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignLifecycleEventRepository extends JpaRepository<CampaignLifecycleEventEntity, Long> {
    List<CampaignLifecycleEventEntity> findTop100ByCampaignIdOrderByCreatedAtDesc(String campaignId);
}

