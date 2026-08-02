package com.agent.repository;

import com.agent.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Page<UserEntity> findByOrgIdAndRole(Long orgId, String role, Pageable pageable);

    Page<UserEntity> findByOrgId(Long orgId, Pageable pageable);

    long countByOrgIdAndRoleAndIsEnabledTrue(Long orgId, String role);

    boolean existsByOrgIdAndRole(Long orgId, String role);
}
