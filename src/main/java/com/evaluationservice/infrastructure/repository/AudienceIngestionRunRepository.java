package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceIngestionRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudienceIngestionRunRepository extends JpaRepository<AudienceIngestionRunEntity, String> {

    Page<AudienceIngestionRunEntity> findByTenantIdOrderByStartedAtDesc(String tenantId, Pageable pageable);

    Page<AudienceIngestionRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);
}
