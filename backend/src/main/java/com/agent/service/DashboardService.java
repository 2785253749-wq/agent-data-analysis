package com.agent.service;

import com.agent.dto.DashboardSummaryDTO;
import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.DatasetEntity;
import com.agent.repository.DashboardRepository;
import com.agent.repository.DatasetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Aggregates the read-only dashboard summary.
 * - Success rate: COMPLETED / (COMPLETED + FAILED + CANCELLED) — only terminal statuses.
 * - Trend: fixed continuous 7 days (DB returns present dates; service fills missing with 0).
 * - Isolation: admin sees whole org; regular users only their accessible datasets.
 */
@Service
public class DashboardService {

    /** Business timezone — avoids UTC day-boundary drift (constraint 3). */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int TREND_DAYS = 7;
    private static final int RECENT_LIMIT = 10;
    private static final int FAILURE_LIMIT = 5;

    private final DashboardRepository dashRepo;
    private final DatasetRepository datasetRepo;
    private final UserAccessContext access;

    public DashboardService(DashboardRepository dashRepo, DatasetRepository datasetRepo,
                            UserAccessContext access) {
        this.dashRepo = dashRepo;
        this.datasetRepo = datasetRepo;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO summary() {
        // Constraint 2: dataset count respects the caller's permission scope.
        long datasetCount;
        boolean allDatasets;
        List<Long> datasetIds;

        if (access.isAdmin()) {
            allDatasets = true;
            datasetIds = List.of();
            datasetCount = dashRepo.countDatasets(access.currentOrgId());
        } else {
            allDatasets = false;
            datasetIds = access.accessibleDatasetIds().stream().sorted().toList();
            datasetCount = datasetIds.size();
        }

        // Terminal status counts
        Map<String, Long> terminal = new HashMap<>();
        long analysisCount = 0;
        for (Object[] row : dashRepo.countByTerminalStatus(allDatasets, datasetIds)) {
            String status = String.valueOf(row[0]);
            long n = ((Number) row[1]).longValue();
            terminal.put(status, n);
            analysisCount += n;
        }

        Double successRate = computeSuccessRate(terminal);

        List<DashboardSummaryDTO.TrendPoint> trend = last7DaysTrend(allDatasets, datasetIds);

        List<DashboardSummaryDTO.RecentTaskDTO> recent = recentTasks(allDatasets, datasetIds);

        List<DashboardSummaryDTO.FailureCount> failures = commonFailures(allDatasets, datasetIds);

        return new DashboardSummaryDTO(datasetCount, analysisCount, successRate,
                trend, recent, failures);
    }

    /** successRate = COMPLETED / (COMPLETED + FAILED + CANCELLED); null when denominator 0. */
    private Double computeSuccessRate(Map<String, Long> terminal) {
        long completed = terminal.getOrDefault("COMPLETED", 0L);
        long failed = terminal.getOrDefault("FAILED", 0L);
        long cancelled = terminal.getOrDefault("CANCELLED", 0L);
        long denom = completed + failed + cancelled;
        if (denom == 0) return null;
        return Math.round(completed * 10000.0 / denom) / 100.0; // 2-decimal percent
    }

    /** Continuous 7 days ending today (Asia/Shanghai); fill missing dates with 0. */
    private List<DashboardSummaryDTO.TrendPoint> last7DaysTrend(boolean all, List<Long> ids) {
        LocalDate today = LocalDate.now(BIZ_ZONE);
        LocalDate from = today.minusDays(TREND_DAYS - 1);
        LocalDateTime since = from.atStartOfDay(BIZ_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : dashRepo.trendSince(since, all, ids)) {
            Object dateObj = row[0];
            LocalDate date = toLocalDate(dateObj);
            if (date != null) {
                counts.put(date, ((Number) row[1]).longValue());
            }
        }

        List<DashboardSummaryDTO.TrendPoint> out = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate d = from.plusDays(i);
            out.add(new DashboardSummaryDTO.TrendPoint(d.toString(), counts.getOrDefault(d, 0L)));
        }
        return out;
    }

    private LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (o instanceof LocalDateTime ldt) return ldt.toLocalDate();
        return null;
    }

    private List<DashboardSummaryDTO.RecentTaskDTO> recentTasks(boolean all, List<Long> ids) {
        List<DashboardSummaryDTO.RecentTaskDTO> out = new ArrayList<>();
        Map<Long, String> datasetNames = new HashMap<>();
        for (AnalysisTaskEntity t : dashRepo.recentTasks(all, ids, PageRequest.of(0, RECENT_LIMIT))) {
            String name = datasetNames.computeIfAbsent(t.getDatasetId(), this::datasetName);
            out.add(new DashboardSummaryDTO.RecentTaskDTO(
                    t.getId(), t.getQuestion(), t.getStatus(), name, t.getCreatedAt(), durationMs(t)));
        }
        return out;
    }

    private List<DashboardSummaryDTO.FailureCount> commonFailures(boolean all, List<Long> ids) {
        List<DashboardSummaryDTO.FailureCount> out = new ArrayList<>();
        for (Object[] row : dashRepo.commonFailures(all, ids, PageRequest.of(0, FAILURE_LIMIT))) {
            out.add(new DashboardSummaryDTO.FailureCount(
                    String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }
        return out;
    }

    private String datasetName(Long id) {
        if (id == null) return null;
        return datasetRepo.findById(id).map(DatasetEntity::getName).orElse(null);
    }

    private Long durationMs(AnalysisTaskEntity t) {
        if (t.getStartedAt() == null || t.getCompletedAt() == null) return null;
        return java.time.Duration.between(t.getStartedAt(), t.getCompletedAt()).toMillis();
    }
}
