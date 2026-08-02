package com.agent.service;

import com.agent.entity.UserEntity;
import com.agent.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the current authenticated user's role, org, and dataset-access scope from the DB.
 *
 * Constraint 5: dataset access is checked via real-time repository lookups,
 * never by loading the full dataset list into memory.
 */
@Component
public class UserAccessContext {

    public static final Long DEFAULT_ORG_ID = 0L;

    private final UserRepository userRepo;
    private final DatasetAccessService accessService;

    public UserAccessContext(UserRepository userRepo, DatasetAccessService accessService) {
        this.userRepo = userRepo;
        this.accessService = accessService;
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    /** Current DB user entity (null if anonymous). */
    public UserEntity currentUser() {
        String name = currentUsername();
        if (name == null) return null;
        return userRepo.findByUsername(name).orElse(null);
    }

    public boolean isAdmin() {
        UserEntity u = currentUser();
        return u != null && u.isAdmin();
    }

    /** Stable DB user id (admin gets its users.id, not the old hardcoded 0). */
    public Long currentUserId() {
        UserEntity u = currentUser();
        if (u == null) return -1L;
        return u.getId();
    }

    public Long currentOrgId() {
        UserEntity u = currentUser();
        return u != null ? u.getOrgId() : DEFAULT_ORG_ID;
    }

    /** Constraint 5: real-time canAccessDataset — ADMIN sees all org datasets. */
    public boolean canAccessDataset(Long datasetId) {
        if (datasetId == null) return false;
        if (isAdmin()) return true;
        Long userId = currentUserId();
        if (userId < 0) return false;
        return accessService.canAccess(userId, datasetId);
    }

    /** Constraint 5: dataset ids for filtering (DB-backed, not full-list load). */
    public List<Long> accessibleDatasetIds() {
        if (isAdmin()) return List.of(); // admin → all-org semantics handled by callers
        Long userId = currentUserId();
        if (userId < 0) return List.of();
        return accessService.authorizedDatasetIds(userId);
    }

    /** Intersect requested datasets with the user's accessible set (ADMIN = all). */
    public List<Long> intersectDatasets(List<Long> requested) {
        if (isAdmin()) {
            // Admin: all datasets; return requested if given, else empty list (never null).
            return requested == null ? List.of() : requested;
        }
        Long userId = currentUserId();
        if (userId < 0) return List.of();
        var accessible = accessService.authorizedDatasetIds(userId);
        if (requested == null || requested.isEmpty()) return accessible;
        return requested.stream().filter(accessible::contains).toList();
    }

    /** Whether a task's dataset is within this user's scope (task ownership too). */
    public boolean canAccessTask(Long taskDatasetId, Long taskUserId) {
        if (taskDatasetId == null) return false;
        if (isAdmin()) return true;
        return canAccessDataset(taskDatasetId) && currentUserId().equals(taskUserId);
    }
}
