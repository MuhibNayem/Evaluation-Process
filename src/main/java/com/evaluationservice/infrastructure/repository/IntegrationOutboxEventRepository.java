package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.IntegrationOutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IntegrationOutboxEventRepository extends JpaRepository<IntegrationOutboxEventEntity, Long> {

    long countByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    long deleteByStatusInAndCreatedAtBefore(List<String> statuses, Instant cutoff);

    @Query("""
            SELECT e
            FROM IntegrationOutboxEventEntity e
            WHERE e.status IN :statuses
              AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<IntegrationOutboxEventEntity> findDispatchCandidates(
            @Param("statuses") List<String> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
