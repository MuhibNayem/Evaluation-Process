package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudienceMappingProfileRepository extends JpaRepository<AudienceMappingProfileEntity, Long> {

    Optional<AudienceMappingProfileEntity> findByIdAndTenantIdAndActiveTrue(Long id, String tenantId);

    List<AudienceMappingProfileEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
