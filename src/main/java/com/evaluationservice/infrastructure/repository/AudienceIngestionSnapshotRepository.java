package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceIngestionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AudienceIngestionSnapshotRepository extends JpaRepository<AudienceIngestionSnapshotEntity, String> {

    long deleteByCreatedAtBefore(Instant cutoff);
}
