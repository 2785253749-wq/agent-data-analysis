package com.agent.repository;

import com.agent.entity.AnalysisTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * Queries for the analysis history / trace list.
 * Isolation is applied at query time via user/org/status/dataset conditions.
 */
public interface TaskHistoryRepository extends JpaRepository<AnalysisTaskEntity, Long> {

    @Query("""
        SELECT t FROM AnalysisTaskEntity t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:keyword IS NULL OR t.question LIKE %:keyword%)
          AND (:userOnly = false OR t.userId = :userId)
          AND (:datasetIds IS NULL OR t.datasetId IN :datasetIds)
        ORDER BY t.createdAt DESC
    """)
    Page<AnalysisTaskEntity> search(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("userOnly") boolean userOnly,
            @Param("userId") Long userId,
            @Param("datasetIds") Collection<Long> datasetIds,
            Pageable pageable);
}
