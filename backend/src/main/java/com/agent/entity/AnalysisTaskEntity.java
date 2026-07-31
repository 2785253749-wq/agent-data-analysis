package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_tasks")
public class AnalysisTaskEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId = 0L;

    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(length = 30)
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    @Column(name = "intent_json", columnDefinition = "JSON")
    private String intentJson;

    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "token_usage", columnDefinition = "JSON")
    private String tokenUsage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIntentJson() { return intentJson; }
    public void setIntentJson(String intentJson) { this.intentJson = intentJson; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
