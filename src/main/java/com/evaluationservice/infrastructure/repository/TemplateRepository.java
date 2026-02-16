package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.TemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, String> {

    Page<TemplateEntity> findByCategory(String category, Pageable pageable);
}
