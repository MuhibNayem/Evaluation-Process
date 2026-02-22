package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.EvaluationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationJpaRepository extends JpaRepository<EvaluationEntity, String> {

    Optional<EvaluationEntity> findByAssignmentId(String assignmentId);

    Page<EvaluationEntity> findByCampaignId(String campaignId, Pageable pageable);

    Page<EvaluationEntity> findByEvaluateeId(String evaluateeId, Pageable pageable);

    List<EvaluationEntity> findByEvaluatorId(String evaluatorId);

    List<EvaluationEntity> findByCampaignIdAndEvaluateeIdAndStatus(String campaignId, String evaluateeId,
            String status);

    boolean existsByAssignmentId(String assignmentId);

    long countByStatus(String status);

    List<EvaluationEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
