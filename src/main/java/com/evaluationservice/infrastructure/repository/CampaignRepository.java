package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {

    Page<CampaignEntity> findByStatus(String status, Pageable pageable);
}
