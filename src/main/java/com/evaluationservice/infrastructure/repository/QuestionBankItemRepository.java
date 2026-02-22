package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.QuestionBankItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionBankItemRepository extends JpaRepository<QuestionBankItemEntity, Long> {
    List<QuestionBankItemEntity> findBySetIdOrderByUpdatedAtDesc(Long setId);

    List<QuestionBankItemEntity> findBySetIdAndStatusOrderByUpdatedAtDesc(Long setId, String status);

    Optional<QuestionBankItemEntity> findBySetIdAndStableKey(Long setId, String stableKey);
}
