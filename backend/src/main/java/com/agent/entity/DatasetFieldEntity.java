package com.agent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_fields")
public class DatasetFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "field_name", nullable = false, length = 200)
    private String fieldName;

    @Column(name = "field_alias", length = 200)
    private String fieldAlias;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private FieldDataType dataType;

    @Column(name = "is_dimension", nullable = false)
    private Boolean isDimension = false;

    @Column(name = "is_metric", nullable = false)
    private Boolean isMetric = false;

    @Column(name = "is_filterable", nullable = false)
    private Boolean isFilterable = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldAlias() { return fieldAlias; }
    public void setFieldAlias(String fieldAlias) { this.fieldAlias = fieldAlias; }

    public FieldDataType getDataType() { return dataType; }
    public void setDataType(FieldDataType dataType) { this.dataType = dataType; }

    public Boolean getIsDimension() { return isDimension; }
    public void setIsDimension(Boolean isDimension) { this.isDimension = isDimension; }

    public Boolean getIsMetric() { return isMetric; }
    public void setIsMetric(Boolean isMetric) { this.isMetric = isMetric; }

    public Boolean getIsFilterable() { return isFilterable; }
    public void setIsFilterable(Boolean isFilterable) { this.isFilterable = isFilterable; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
