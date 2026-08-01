package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable prompt template version.
 * content/variables cannot be edited after creation — a new version is created instead
 * (old versions only enable/disable/archive). Content hash recorded so history replay
 * can verify which exact content ran.
 */
@Entity
@Table(name = "prompt_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_version", columnNames = {"type", "version"}))
public class PromptTemplateEntity {

    public static final String TYPE_INTENT = "INTENT_RECOGNITION";
    public static final String TYPE_SQL_GEN = "SQL_GENERATION";
    public static final String TYPE_INTERPRET = "INTERPRETATION";
    public static final String TYPE_SQL_REPAIR = "SQL_REPAIR";
    public static final java.util.Set<String> REQUIRED_TYPES = java.util.Set.of(
            TYPE_INTENT, TYPE_SQL_GEN, TYPE_INTERPRET);

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;            // INTENT_RECOGNITION / SQL_GENERATION / INTERPRETATION / SQL_REPAIR

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String variables;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(length = 500)
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    @PreUpdate void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
