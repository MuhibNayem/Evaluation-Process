package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceIngestionRejectionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudienceIngestionRejectionRepository extends JpaRepository<AudienceIngestionRejectionEntity, Long> {

    Page<AudienceIngestionRejectionEntity> findByRunIdOrderByRowNumberAsc(String runId, Pageable pageable);
}
