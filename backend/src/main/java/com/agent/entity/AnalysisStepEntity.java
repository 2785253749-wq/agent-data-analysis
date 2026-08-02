package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One execution step within an analysis task (intent, sql-gen, validate, query, interpret, chart).
 * Persists per-step status/duration/output for the "Agent 执行追踪" feature.
 * Sensitive content is NOT stored here — only structured output summaries.
 */
@Entity
@Table(name = "analysis_steps")
public class AnalysisStepEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;   // INTENT / SQL_GEN / SQL_VALIDATE / QUERY / INTERPRET / CHART

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(length = 30)
    private String status = "PENDING";  // PENDING/RUNNING/COMPLETED/FAILED/SKIPPED

    @Column(name = "input_json", columnDefinition = "JSON")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "JSON")
    private String outputJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failure_category", length = 50)
    private String failureCategory; // SQL_VALIDATION / QUERY_EXECUTION / MODEL_TIMEOUT / MODEL_RESPONSE / UNEXPECTED

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void prePersist() { this.createdAt = LocalDateTime.now(); }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public String getOutputJson() { return outputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
