package com.evaluationservice.infrastructure.repository;

import com.evaluationservice.infrastructure.entity.AudienceMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudienceMembershipRepository extends JpaRepository<AudienceMembershipEntity, Long> {

    Optional<AudienceMembershipEntity> findByTenantIdAndPersonIdAndGroupIdAndMembershipRole(
            String tenantId, String personId, String groupId, String membershipRole);

    Optional<AudienceMembershipEntity> findByTenantIdAndPersonIdAndGroupIdAndMembershipRoleIsNull(
            String tenantId, String personId, String groupId);
}
