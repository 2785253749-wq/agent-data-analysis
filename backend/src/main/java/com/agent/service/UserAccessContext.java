package com.agent.service;

import com.agent.entity.DatasetEntity;
import com.agent.repository.DatasetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the current authenticated user's data-access scope.
 *
 * Isolation rules (hard constraint 1):
 * - Admin (username "admin") may query tasks within the org only (org_id == current org).
 * - Regular users may only query their own tasks (user_id == self) within the org.
 * - datasetIds filter is intersected with the user's accessible dataset ids.
 * - Accessing a task the user cannot see returns 404 (never leaks existence).
 */
@Component
public class UserAccessContext {

    public static final String ADMIN_USERNAME = "admin";
    public static final Long DEFAULT_ORG_ID = 0L;

    private final DatasetRepository datasetRepo;

    public UserAccessContext(DatasetRepository datasetRepo) {
        this.datasetRepo = datasetRepo;
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    public boolean isAdmin() {
        String name = currentUsername();
        return ADMIN_USERNAME.equals(name);
    }

    /**
     * Current user id. Without a real user table we derive a stable id from the username hash.
     * Admin resolves to the shared system user id 0 (tasks created so far use userId 0).
     */
    public Long currentUserId() {
        if (isAdmin()) return 0L;
        String name = currentUsername();
        if (name == null) return -1L;
        return (long) (name.hashCode() & 0x7fffffff);
    }

    public Long currentOrgId() {
        return DEFAULT_ORG_ID;
    }

    /**
     * Dataset ids the current user may access (same org). Admin sees all org datasets.
     */
    public Set<Long> accessibleDatasetIds() {
        return datasetRepo.findAll().stream()
                .filter(d -> d.getOrgId().equals(currentOrgId()))
                .map(DatasetEntity::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Intersect the requested datasetIds filter with the user's accessible set.
     * If requested list is empty/null → all accessible. Returns empty when no overlap.
     */
    public List<Long> intersectDatasets(List<Long> requested) {
        Set<Long> accessible = accessibleDatasetIds();
        if (requested == null || requested.isEmpty()) {
            return accessible.stream().sorted().toList();
        }
        return requested.stream().filter(accessible::contains).sorted().toList();
    }

    /** Whether a task's dataset is within this user's accessible scope. */
    public boolean canAccessTask(Long taskDatasetId, Long taskUserId) {
        if (taskDatasetId == null) return false;
        if (!accessibleDatasetIds().contains(taskDatasetId)) return false;
        // Regular user must own the task
        if (!isAdmin() && !currentUserId().equals(taskUserId)) return false;
        return true;
    }
}
