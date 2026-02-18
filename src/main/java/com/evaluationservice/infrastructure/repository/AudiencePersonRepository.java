package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudiencePersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudiencePersonRepository extends JpaRepository<AudiencePersonEntity, String> {

    Optional<AudiencePersonEntity> findByTenantIdAndExternalRef(String tenantId, String externalRef);
}
