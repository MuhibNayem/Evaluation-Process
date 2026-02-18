package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AdminActionAuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionAuditLogRepository extends JpaRepository<AdminActionAuditLogEntity, Long> {

    List<AdminActionAuditLogEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
