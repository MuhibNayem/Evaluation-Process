package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {

    Page<CampaignEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query(value = """
            SELECT * FROM campaigns c
            WHERE EXISTS (
              SELECT 1
              FROM jsonb_array_elements(COALESCE(c.assignments_json, '[]')::jsonb) AS a
              WHERE a->>'evaluatorId' = :evaluatorId
            )
            """, nativeQuery = true)
    List<CampaignEntity> findByEvaluatorId(@Param("evaluatorId") String evaluatorId);

    @Query(value = """
            SELECT COALESCE(SUM(jsonb_array_length(COALESCE(c.assignments_json, '[]')::jsonb)), 0)
            FROM campaigns c
            """, nativeQuery = true)
    long countTotalAssignments();

    @Query(value = """
            SELECT COALESCE(SUM((
              SELECT COUNT(*)
              FROM jsonb_array_elements(COALESCE(c.assignments_json, '[]')::jsonb) AS a
              WHERE COALESCE((a->>'completed')::boolean, false) = true
            )), 0)
            FROM campaigns c
            """, nativeQuery = true)
    long countCompletedAssignments();

    @Query(value = "SELECT assignments_json FROM campaigns WHERE id = :campaignId", nativeQuery = true)
    String findAssignmentsJsonByCampaignId(@Param("campaignId") String campaignId);

    List<CampaignEntity> findByIdIn(Collection<String> ids);

    List<CampaignEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
