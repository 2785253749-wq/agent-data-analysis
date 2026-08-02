package com.agent.repository;

import com.agent.entity.DatasetAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatasetAccessRepository extends JpaRepository<DatasetAccessEntity, Long> {

    Optional<DatasetAccessEntity> findByUserIdAndDatasetId(Long userId, Long datasetId);

    List<DatasetAccessEntity> findByUserId(Long userId);

    /** Constraint 5: JOIN-based filter — no full-list load. */
    @Query("""
        SELECT da.datasetId FROM DatasetAccessEntity da
        WHERE da.userId = :userId
    """)
    List<Long> findAccessibleDatasetIds(@Param("userId") Long userId);

    /** Real-time existence check for canAccessDataset. */
    boolean existsByUserIdAndDatasetId(Long userId, Long datasetId);

    void deleteByUserIdAndDatasetId(Long userId, Long datasetId);
}
