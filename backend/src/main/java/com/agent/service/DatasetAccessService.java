package com.agent.service;

import com.agent.entity.DatasetAccessEntity;
import com.agent.entity.DatasetEntity;
import com.agent.entity.UserEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.DatasetAccessRepository;
import com.agent.repository.DatasetRepository;
import com.agent.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dataset authorization for ANALYST users.
 * Constraint 1: grant validates user.org_id == dataset.org_id (no cross-org grant).
 */
@Service
public class DatasetAccessService {

    private final DatasetAccessRepository accessRepo;
    private final UserRepository userRepo;
    private final DatasetRepository datasetRepo;

    public DatasetAccessService(DatasetAccessRepository accessRepo,
                                UserRepository userRepo,
                                DatasetRepository datasetRepo) {
        this.accessRepo = accessRepo;
        this.userRepo = userRepo;
        this.datasetRepo = datasetRepo;
    }

    public List<Long> authorizedDatasetIds(Long userId) {
        return accessRepo.findAccessibleDatasetIds(userId);
    }

    /** Real-time canAccess check (constraint 5). */
    public boolean canAccess(Long userId, Long datasetId) {
        if (datasetId == null) return false;
        return accessRepo.existsByUserIdAndDatasetId(userId, datasetId);
    }

    @Transactional
    public void grant(Long userId, Long datasetId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        DatasetEntity ds = datasetRepo.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", datasetId));

        // Constraint 1: no cross-org grant.
        if (!user.getOrgId().equals(ds.getOrgId())) {
            throw new IllegalArgumentException("不能跨组织授权数据集");
        }
        if (!accessRepo.findByUserIdAndDatasetId(userId, datasetId).isPresent()) {
            DatasetAccessEntity a = new DatasetAccessEntity();
            a.setUserId(userId);
            a.setDatasetId(datasetId);
            accessRepo.save(a);
        }
    }

    @Transactional
    public void revoke(Long userId, Long datasetId) {
        accessRepo.deleteByUserIdAndDatasetId(userId, datasetId);
    }
}
