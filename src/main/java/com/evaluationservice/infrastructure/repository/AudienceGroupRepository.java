package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudienceGroupRepository extends JpaRepository<AudienceGroupEntity, String> {
}
