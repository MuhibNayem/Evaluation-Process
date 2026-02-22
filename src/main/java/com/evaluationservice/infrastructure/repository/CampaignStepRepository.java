package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignStepRepository extends JpaRepository<CampaignStepEntity, Long> {
    List<CampaignStepEntity> findByCampaignIdOrderByDisplayOrderAsc(String campaignId);

    Optional<CampaignStepEntity> findByCampaignIdAndStepType(String campaignId, String stepType);

    void deleteByCampaignId(String campaignId);
}
