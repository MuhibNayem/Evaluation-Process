package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AudienceMappingProfileEventRepository extends JpaRepository<AudienceMappingProfileEventEntity, Long> {

    Page<AudienceMappingProfileEventEntity> findByTenantIdAndProfileIdOrderByCreatedAtDesc(
            String tenantId, Long profileId, Pageable pageable);

    long deleteByCreatedAtBefore(Instant cutoff);
}
