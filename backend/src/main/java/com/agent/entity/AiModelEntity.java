package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AI model configuration.
 * Security constraints:
 * - apiKeyRef is a WHITELIST env var name (e.g. DEEPSEEK_API_KEY), never a plaintext key.
 * - Base URL restricted to HTTPS + allowlisted domains (enforced in service).
 * - Global default: only one model may be enabled+default system-wide.
 */
@Entity
@Table(name = "ai_models")
public class AiModelEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;                    // display name

    @Column(nullable = false, length = 50)
    private String provider = "deepseek";

    @Column(name = "base_url", nullable = false, length = 300)
    private String baseUrl;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 60000;

    @Column(nullable = false)
    private Double temperature = 0.0;

    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens = 2048;

    @Column(name = "api_key_ref", nullable = false, length = 200)
    private String apiKeyRef;               // whitelist env var name only

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

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
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public String getApiKeyRef() { return apiKeyRef; }
    public void setApiKeyRef(String apiKeyRef) { this.apiKeyRef = apiKeyRef; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
