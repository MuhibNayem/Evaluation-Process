package com.evaluationservice.infrastructure.persistence;

import com.evaluationservice.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("SELECT p.name FROM RolePermission rp JOIN rp.permission p JOIN rp.role r WHERE r.name IN :roleNames")
    Set<String> findPermissionsByRoleNames(@Param("roleNames") List<String> roleNames);
}
