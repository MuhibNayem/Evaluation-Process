package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignAssignmentRepository extends JpaRepository<CampaignAssignmentEntity, String> {

    List<CampaignAssignmentEntity> findByCampaignId(String campaignId);

    @Query("SELECT DISTINCT a.campaignId FROM CampaignAssignmentEntity a WHERE a.evaluatorId = :evaluatorId")
    List<String> findDistinctCampaignIdsByEvaluatorId(@Param("evaluatorId") String evaluatorId);

    List<CampaignAssignmentEntity> findByEvaluatorId(String evaluatorId);

    long countByEvaluatorId(String evaluatorId);

    long countByEvaluatorIdAndCompletedTrue(String evaluatorId);

    @Query("""
            SELECT a FROM CampaignAssignmentEntity a
             WHERE (:campaignId IS NULL OR a.campaignId = :campaignId)
               AND (:stepType IS NULL OR a.stepType = :stepType)
               AND (:sectionId IS NULL OR a.sectionId = :sectionId)
               AND (:facultyId IS NULL OR a.facultyId = :facultyId)
               AND (:status IS NULL OR a.status = :status)
               AND (:evaluatorId IS NULL OR a.evaluatorId = :evaluatorId)
               AND (:evaluateeId IS NULL OR a.evaluateeId = :evaluateeId)
            """)
    List<CampaignAssignmentEntity> findFiltered(
            @Param("campaignId") String campaignId,
            @Param("stepType") String stepType,
            @Param("sectionId") String sectionId,
            @Param("facultyId") String facultyId,
            @Param("status") String status,
            @Param("evaluatorId") String evaluatorId,
            @Param("evaluateeId") String evaluateeId);

    @Query("""
            SELECT a FROM CampaignAssignmentEntity a
             WHERE (:campaignId IS NULL OR a.campaignId = :campaignId)
               AND (:stepType IS NULL OR a.stepType = :stepType)
               AND (:sectionId IS NULL OR a.sectionId = :sectionId)
               AND (:facultyId IS NULL OR a.facultyId = :facultyId)
               AND (:status IS NULL OR a.status = :status)
               AND (:evaluatorId IS NULL OR a.evaluatorId = :evaluatorId)
               AND (:evaluateeId IS NULL OR a.evaluateeId = :evaluateeId)
            """)
    Page<CampaignAssignmentEntity> findFilteredPage(
            @Param("campaignId") String campaignId,
            @Param("stepType") String stepType,
            @Param("sectionId") String sectionId,
            @Param("facultyId") String facultyId,
            @Param("status") String status,
            @Param("evaluatorId") String evaluatorId,
            @Param("evaluateeId") String evaluateeId,
            Pageable pageable);

    boolean existsByCampaignIdAndEvaluatorIdAndEvaluateeIdAndEvaluatorRole(
            String campaignId,
            String evaluatorId,
            String evaluateeId,
            String evaluatorRole);

    Optional<CampaignAssignmentEntity> findByCampaignIdAndEvaluatorIdAndEvaluateeIdAndEvaluatorRole(
            String campaignId,
            String evaluatorId,
            String evaluateeId,
            String evaluatorRole);

    long countByCompletedTrue();

    @Modifying
    @Query("DELETE FROM CampaignAssignmentEntity a WHERE a.campaignId = :campaignId")
    void deleteByCampaignId(@Param("campaignId") String campaignId);

    @Modifying
    @Query("""
            UPDATE CampaignAssignmentEntity a
               SET a.completed = true,
                   a.evaluationId = :evaluationId,
                   a.status = 'COMPLETED',
                   a.updatedAt = :updatedAt
             WHERE a.id = :assignmentId
            """)
    int markCompleted(@Param("assignmentId") String assignmentId,
            @Param("evaluationId") String evaluationId,
            @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            UPDATE CampaignAssignmentEntity a
               SET a.completed = false,
                   a.evaluationId = null,
                   a.status = 'ACTIVE',
                   a.updatedAt = :updatedAt
             WHERE a.id = :assignmentId
            """)
    int markReopened(@Param("assignmentId") String assignmentId, @Param("updatedAt") Instant updatedAt);
}
