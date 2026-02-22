package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.QuestionBankItemVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionBankItemVersionRepository extends JpaRepository<QuestionBankItemVersionEntity, Long> {
    List<QuestionBankItemVersionEntity> findByQuestionItemIdOrderByVersionNoDesc(Long questionItemId);

    List<QuestionBankItemVersionEntity> findByQuestionItemIdAndStatusOrderByVersionNoDesc(Long questionItemId, String status);

    Optional<QuestionBankItemVersionEntity> findByQuestionItemIdAndVersionNo(Long questionItemId, int versionNo);
}
