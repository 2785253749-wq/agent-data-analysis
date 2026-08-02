package com.agent.service;

import com.agent.dto.UserDTO;
import com.agent.entity.UserEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management with org scoping + self-lock protection.
 *
 * Constraint 1: ADMIN manages only same-org users.
 * Constraint 4: cannot disable/demote/delete the LAST enabled ADMIN, nor self-disable/demote.
 * Passwords are BCrypt-hashed; never returned.
 */
@Service
public class UserService {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Page<UserDTO> list(Long orgId, String role, int page, int size) {
        Page<UserEntity> result = (role == null || role.isBlank())
                ? repo.findByOrgId(orgId, PageRequest.of(page, size))
                : repo.findByOrgIdAndRole(orgId, role, PageRequest.of(page, size));
        return result.map(this::toDTO);
    }

    @Transactional
    public UserDTO create(UserDTO.CreateRequest req, Long orgId) {
        if (repo.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserEntity u = new UserEntity();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(req.displayName());
        u.setRole(req.role() == null ? UserEntity.ROLE_ANALYST : req.role());
        u.setOrgId(orgId);
        u.setIsEnabled(req.isEnabled() != null ? req.isEnabled() : true);
        return toDTO(repo.save(u));
    }

    @Transactional
    public UserDTO update(Long id, UserDTO.UpdateRequest req, Long orgId, String currentAdmin) {
        UserEntity u = find(id);
        assertSameOrg(u, orgId);

        // Constraint 4: last enabled ADMIN cannot be demoted/disabled.
        if (u.isAdmin() && (req.role() != null && !UserEntity.ROLE_ADMIN.equals(req.role()))) {
            if (isLastEnabledAdmin(u)) throw new IllegalArgumentException("不能降级最后一个启用管理员");
        }
        if (u.isAdmin() && Boolean.FALSE.equals(req.isEnabled())) {
            if (isLastEnabledAdmin(u)) throw new IllegalArgumentException("不能禁用最后一个启用管理员");
        }
        // Self demote/disable guard
        if (currentAdmin.equals(u.getUsername()) && (Boolean.FALSE.equals(req.isEnabled())
                || (req.role() != null && !UserEntity.ROLE_ADMIN.equals(req.role())))) {
            throw new IllegalArgumentException("不能禁用或降级当前登录管理员");
        }

        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.role() != null) u.setRole(req.role());
        if (req.isEnabled() != null) u.setIsEnabled(req.isEnabled());
        return toDTO(repo.save(u));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, Long orgId) {
        UserEntity u = find(id);
        assertSameOrg(u, orgId);
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
    }

    @Transactional
    public void delete(Long id, Long orgId, String currentAdmin) {
        UserEntity u = find(id);
        assertSameOrg(u, orgId);
        if (u.isAdmin() && isLastEnabledAdmin(u)) {
            throw new IllegalArgumentException("不能删除最后一个启用管理员");
        }
        if (currentAdmin.equals(u.getUsername())) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }
        repo.delete(u);
    }

    public UserEntity find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void assertSameOrg(UserEntity u, Long orgId) {
        if (!u.getOrgId().equals(orgId)) {
            throw new IllegalArgumentException("只能管理本组织用户");
        }
    }

    private boolean isLastEnabledAdmin(UserEntity self) {
        long enabledAdmins = repo.countByOrgIdAndRoleAndIsEnabledTrue(self.getOrgId(), UserEntity.ROLE_ADMIN);
        boolean selfEnabled = Boolean.TRUE.equals(self.getIsEnabled());
        // If self is currently the only enabled admin → guard.
        return selfEnabled && enabledAdmins <= 1;
    }

    private UserDTO toDTO(UserEntity u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getDisplayName(),
                u.getRole(), u.getOrgId(), u.getIsEnabled(), u.getCreatedAt());
    }
}
