package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
    @Query("""
            SELECT d FROM NotificationDeliveryEntity d
             WHERE (:campaignId IS NULL OR d.campaignId = :campaignId)
               AND (:ruleId IS NULL OR d.ruleId = :ruleId)
               AND (:status IS NULL OR d.status = :status)
             ORDER BY d.createdAt DESC
            """)
    List<NotificationDeliveryEntity> findFiltered(
            @Param("campaignId") String campaignId,
            @Param("ruleId") Long ruleId,
            @Param("status") String status);

    boolean existsByRuleIdAndCreatedAtBetween(Long ruleId, Instant from, Instant to);
}
