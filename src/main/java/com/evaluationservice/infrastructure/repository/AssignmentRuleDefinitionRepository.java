package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AssignmentRuleDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRuleDefinitionRepository extends JpaRepository<AssignmentRuleDefinitionEntity, Long> {

    List<AssignmentRuleDefinitionEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    List<AssignmentRuleDefinitionEntity> findByTenantIdAndStatusOrderByUpdatedAtDesc(String tenantId, String status);

    Optional<AssignmentRuleDefinitionEntity> findByIdAndTenantId(Long id, String tenantId);
}
