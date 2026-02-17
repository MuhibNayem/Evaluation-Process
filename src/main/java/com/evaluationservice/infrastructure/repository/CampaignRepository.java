package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {

    Page<CampaignEntity> findByStatus(String status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CampaignEntity c WHERE c.assignmentsJson LIKE %:pattern%")
    java.util.List<CampaignEntity> findByAssignmentsJsonLike(
            @org.springframework.data.repository.query.Param("pattern") String pattern);
}
