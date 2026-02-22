package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.QuestionBankSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionBankSetRepository extends JpaRepository<QuestionBankSetEntity, Long> {
    List<QuestionBankSetEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    List<QuestionBankSetEntity> findByStatusOrderByUpdatedAtDesc(String status);
}
