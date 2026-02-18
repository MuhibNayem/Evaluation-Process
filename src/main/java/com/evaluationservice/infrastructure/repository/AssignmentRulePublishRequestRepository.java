package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AssignmentRulePublishRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRulePublishRequestRepository extends JpaRepository<AssignmentRulePublishRequestEntity, Long> {

    List<AssignmentRulePublishRequestEntity> findByTenantIdAndStatusOrderByRequestedAtDesc(String tenantId, String status);

    boolean existsByRuleDefinitionIdAndStatus(Long ruleDefinitionId, String status);
}
