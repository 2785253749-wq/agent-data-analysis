package com.agent.repository;

import com.agent.entity.AnalysisStepEntity;
import com.agent.entity.AnalysisTaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DB-side aggregation queries for the dashboard. All counts are computed in the DB —
 * no full-history download to the frontend.
 */
public interface DashboardRepository {

    /** Dataset count visible to the caller (org + optional user auth scope). */
    @Query("SELECT COUNT(d) FROM DatasetEntity d WHERE d.orgId = :orgId AND d.isEnabled = true")
    long countDatasets(@Param("orgId") Long orgId);

    /** Analysis count by terminal status — filtered to caller's dataset scope. */
    @Query("""
        SELECT t.status, COUNT(t) FROM AnalysisTaskEntity t
        WHERE t.status IN ('COMPLETED','FAILED','CANCELLED')
          AND (:allDatasets = true OR t.datasetId IN :datasetIds)
        GROUP BY t.status
    """)
    List<Object[]> countByTerminalStatus(@Param("allDatasets") boolean allDatasets,
                                         @Param("datasetIds") List<Long> datasetIds);

    /** Trend: task counts grouped by date for the last N days, in caller scope. */
    @Query("""
        SELECT FUNCTION('DATE', t.createdAt), COUNT(t) FROM AnalysisTaskEntity t
        WHERE t.createdAt >= :since
          AND (:allDatasets = true OR t.datasetId IN :datasetIds)
        GROUP BY FUNCTION('DATE', t.createdAt)
    """)
    List<Object[]> trendSince(@Param("since") LocalDateTime since,
                              @Param("allDatasets") boolean allDatasets,
                              @Param("datasetIds") List<Long> datasetIds);

    /** Recent tasks in caller scope. */
    @Query("""
        SELECT t FROM AnalysisTaskEntity t
        WHERE :allDatasets = true OR t.datasetId IN :datasetIds
        ORDER BY t.createdAt DESC
    """)
    List<AnalysisTaskEntity> recentTasks(@Param("allDatasets") boolean allDatasets,
                                         @Param("datasetIds") List<Long> datasetIds,
                                         Pageable pageable);

    /** Common failure reasons — stable category aggregation from FAILED steps. */
    @Query("""
        SELECT s.failureCategory, COUNT(s) FROM AnalysisStepEntity s
        WHERE s.status = 'FAILED' AND s.failureCategory IS NOT NULL
          AND s.taskId IN (
              SELECT t.id FROM AnalysisTaskEntity t
              WHERE :allDatasets = true OR t.datasetId IN :datasetIds
          )
        GROUP BY s.failureCategory
        ORDER BY COUNT(s) DESC
    """)
    List<Object[]> commonFailures(@Param("allDatasets") boolean allDatasets,
                                  @Param("datasetIds") List<Long> datasetIds,
                                  Pageable pageable);
}
