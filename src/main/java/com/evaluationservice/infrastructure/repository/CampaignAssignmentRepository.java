package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CampaignAssignmentRepository extends JpaRepository<CampaignAssignmentEntity, String> {

    List<CampaignAssignmentEntity> findByCampaignId(String campaignId);

    @Query("SELECT DISTINCT a.campaignId FROM CampaignAssignmentEntity a WHERE a.evaluatorId = :evaluatorId")
    List<String> findDistinctCampaignIdsByEvaluatorId(@Param("evaluatorId") String evaluatorId);

    long countByCompletedTrue();

    @Modifying
    @Query("DELETE FROM CampaignAssignmentEntity a WHERE a.campaignId = :campaignId")
    void deleteByCampaignId(@Param("campaignId") String campaignId);

    @Modifying
    @Query("""
            UPDATE CampaignAssignmentEntity a
               SET a.completed = true,
                   a.evaluationId = :evaluationId,
                   a.updatedAt = :updatedAt
             WHERE a.id = :assignmentId
            """)
    int markCompleted(@Param("assignmentId") String assignmentId,
            @Param("evaluationId") String evaluationId,
            @Param("updatedAt") Instant updatedAt);
}
