package com.agent.service;

import com.agent.dto.*;
import com.agent.entity.AnalysisStepEntity;
import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.DatasetEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.AnalysisStepRepository;
import com.agent.repository.DatasetRepository;
import com.agent.repository.TaskHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analysis history + agent trace. Enforces data isolation (hard constraint 1) and
 * sanitized error exposure (constraint 3). Detail ACL: only owner or admin.
 */
@Service
public class TaskHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TaskHistoryService.class);

    private final TaskHistoryRepository taskRepo;
    private final AnalysisStepRepository stepRepo;
    private final DatasetRepository datasetRepo;
    private final UserAccessContext access;
    private final ErrorMessageSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    public TaskHistoryService(TaskHistoryRepository taskRepo,
                              AnalysisStepRepository stepRepo,
                              DatasetRepository datasetRepo,
                              UserAccessContext access,
                              ErrorMessageSanitizer sanitizer,
                              ObjectMapper objectMapper) {
        this.taskRepo = taskRepo;
        this.stepRepo = stepRepo;
        this.datasetRepo = datasetRepo;
        this.access = access;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
    }

    /**
     * List tasks visible to the current user.
     * - Admin: all tasks in org, any user.
     * - Regular user: only own tasks in org.
     * - datasetIds intersected with the user's accessible datasets.
     */
    public PagedResponse<TaskSummaryDTO> list(int page, int size, String status,
                                               List<Long> requestedDatasets, String keyword) {
        List<Long> accessible = access.intersectDatasets(requestedDatasets);
        boolean userOnly = !access.isAdmin();

        // If datasets were requested but the intersection is empty → nothing visible.
        boolean requestedAny = requestedDatasets != null && !requestedDatasets.isEmpty();
        if (requestedAny && accessible.isEmpty()) {
            return PagedResponse.from(Page.empty(PageRequest.of(page, size)));
        }

        Page<AnalysisTaskEntity> result = taskRepo.search(
                blankToNull(status), blankToNull(keyword),
                userOnly, access.currentUserId(),
                accessible.isEmpty() ? null : accessible,
                PageRequest.of(page, size));

        return PagedResponse.from(result.map(this::toSummary));
    }

    /**
     * Task detail with ACL — returns 404 (ResourceNotFoundException) when the caller
     * cannot access the task, so existence is never leaked.
     * sqlText + parameters only returned to the owner/admin.
     */
    public TaskDetailResponse detail(Long taskId) {
        AnalysisTaskEntity task = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AnalysisTask", taskId));

        if (!access.canAccessTask(task.getDatasetId(), task.getUserId())) {
            throw new ResourceNotFoundException("AnalysisTask", taskId);
        }

        String datasetName = datasetRepo.findById(task.getDatasetId())
                .map(DatasetEntity::getName).orElse(null);

        Map<String, Object> snap = parseSnapshot(task.getResultJson());

        boolean isOwnerOrAdmin = access.isAdmin() || access.currentUserId().equals(task.getUserId());

        return new TaskDetailResponse(
                task.getId(), task.getQuestion(), task.getDatasetId(), datasetName,
                task.getStatus(), durationMs(task), task.getCreatedAt(), task.getCompletedAt(),
                snap.get("intent"),
                isOwnerOrAdmin ? (String) snap.get("sqlText") : null,
                isOwnerOrAdmin ? (Map) snap.get("parameters") : null,
                snap.get("validation"),
                snap.get("queryResult"),
                snap.get("interpretation"),
                snap.get("chartSpec"),
                sanitizer.sanitize(task.getErrorMessage()),
                toStepDtos(taskId)
        );
    }

    // ---- Helpers ----

    private TaskSummaryDTO toSummary(AnalysisTaskEntity t) {
        String datasetName = datasetRepo.findById(t.getDatasetId())
                .map(DatasetEntity::getName).orElse(null);
        return new TaskSummaryDTO(
                t.getId(), t.getQuestion(), t.getDatasetId(), datasetName,
                t.getStatus(), durationMs(t), t.getCreatedAt(), t.getCompletedAt());
    }

    private Long durationMs(AnalysisTaskEntity t) {
        if (t.getStartedAt() == null || t.getCompletedAt() == null) return null;
        return java.time.Duration.between(t.getStartedAt(), t.getCompletedAt()).toMillis();
    }

    private List<TaskStepDTO> toStepDtos(Long taskId) {
        List<AnalysisStepEntity> steps = stepRepo.findByTaskIdOrderByStepOrderAsc(taskId);
        List<TaskStepDTO> dtos = new ArrayList<>();
        for (AnalysisStepEntity s : steps) {
            dtos.add(new TaskStepDTO(s.getStepType(), s.getStatus(), s.getDurationMs(),
                    s.getErrorMessage() == null ? null : sanitizer.sanitize(s.getErrorMessage())));
        }
        return dtos;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSnapshot(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Object o = objectMapper.readValue(json, Object.class);
            return (Map<String, Object>) o;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse result snapshot: {}", e.getMessage());
            return Map.of();
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
