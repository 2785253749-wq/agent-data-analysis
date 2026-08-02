package com.agent.service;

import com.agent.annotation.Audit;
import com.agent.dto.*;
import com.agent.entity.*;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepo;
    private final DatasetFieldRepository fieldRepo;
    private final MetricsDefinitionRepository metricRepo;

    public DatasetService(DatasetRepository datasetRepo,
                          DatasetFieldRepository fieldRepo,
                          MetricsDefinitionRepository metricRepo) {
        this.datasetRepo = datasetRepo;
        this.fieldRepo = fieldRepo;
        this.metricRepo = metricRepo;
    }

    // ---- Datasets ----

    public PagedResponse<DatasetResponse> list(int page, int size, String search) {
        Page<DatasetEntity> result;
        if (search != null && !search.isBlank()) {
            result = datasetRepo.findByNameContainingIgnoreCase(search, PageRequest.of(page, size));
        } else {
            result = datasetRepo.findAll(PageRequest.of(page, size));
        }
        return PagedResponse.from(result.map(DatasetResponse::from));
    }

    public DatasetResponse getById(Long id) {
        return DatasetResponse.from(findDataset(id));
    }

    @Transactional
    @Audit(action = "DATASET_CREATE", resourceType = "DATASET")
    public DatasetResponse create(DatasetRequest request) {
        // Check uniqueness
        if (datasetRepo.existsByOrgIdAndTableName(
                request.orgId() != null ? request.orgId() : 0L, request.tableName())) {
            throw new DataIntegrityViolationException(
                    "Duplicate entry for key 'uk_org_table'");
        }

        DatasetEntity entity = new DatasetEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTableName(request.tableName());
        entity.setOrgId(request.orgId() != null ? request.orgId() : 0L);
        entity.setIsEnabled(request.isEnabled() != null ? request.isEnabled() : true);
        return DatasetResponse.from(datasetRepo.save(entity));
    }

    @Transactional
    @Audit(action = "DATASET_UPDATE", resourceType = "DATASET")
    public DatasetResponse update(Long id, DatasetRequest request) {
        DatasetEntity entity = findDataset(id);

        // Check uniqueness, excluding self
        if (datasetRepo.existsByOrgIdAndTableNameAndIdNot(
                request.orgId() != null ? request.orgId() : entity.getOrgId(),
                request.tableName(),
                id)) {
            throw new DataIntegrityViolationException(
                    "Duplicate entry for key 'uk_org_table'");
        }

        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTableName(request.tableName());
        entity.setOrgId(request.orgId() != null ? request.orgId() : entity.getOrgId());
        entity.setIsEnabled(request.isEnabled() != null ? request.isEnabled() : entity.getIsEnabled());
        return DatasetResponse.from(datasetRepo.save(entity));
    }

    @Transactional
    @Audit(action = "DATASET_DELETE", resourceType = "DATASET")
    public void delete(Long id) {
        if (!datasetRepo.existsById(id)) {
            throw new ResourceNotFoundException("Dataset", id);
        }
        // Fields and metrics cascade-deleted by the database FK ON DELETE CASCADE
        datasetRepo.deleteById(id);
    }

    // ---- Fields ----

    public PagedResponse<DatasetFieldResponse> listFields(Long datasetId, int page, int size) {
        ensureDatasetExists(datasetId);
        Page<DatasetFieldEntity> result = fieldRepo.findByDatasetId(datasetId, PageRequest.of(page, size));
        return PagedResponse.from(result.map(DatasetFieldResponse::from));
    }

    public List<DatasetFieldResponse> listAllFields(Long datasetId) {
        ensureDatasetExists(datasetId);
        return fieldRepo.findAllByDatasetId(datasetId).stream()
                .map(DatasetFieldResponse::from)
                .toList();
    }

    @Transactional
    @Audit(action = "FIELD_CREATE", resourceType = "FIELD")
    public DatasetFieldResponse createField(Long datasetId, DatasetFieldRequest request) {
        ensureDatasetExists(datasetId);

        if (fieldRepo.existsByDatasetIdAndFieldName(datasetId, request.fieldName())) {
            throw new DataIntegrityViolationException(
                    "Duplicate entry for key 'uk_dataset_field'");
        }

        FieldDataType dataType = FieldDataType.fromString(request.dataType());

        DatasetFieldEntity entity = new DatasetFieldEntity();
        entity.setDatasetId(datasetId);
        entity.setFieldName(request.fieldName());
        entity.setFieldAlias(request.fieldAlias());
        entity.setDataType(dataType);
        entity.setIsDimension(request.isDimension() != null ? request.isDimension() : false);
        entity.setIsMetric(request.isMetric() != null ? request.isMetric() : false);
        entity.setIsFilterable(request.isFilterable() != null ? request.isFilterable() : false);
        entity.setIsSensitive(request.isSensitive() != null ? request.isSensitive() : false);
        entity.setDescription(request.description());
        return DatasetFieldResponse.from(fieldRepo.save(entity));
    }

    @Transactional
    @Audit(action = "FIELD_UPDATE", resourceType = "FIELD")
    public DatasetFieldResponse updateField(Long datasetId, Long fieldId, DatasetFieldRequest request) {
        ensureDatasetExists(datasetId);
        DatasetFieldEntity entity = findField(fieldId);

        if (fieldRepo.existsByDatasetIdAndFieldNameAndIdNot(datasetId, request.fieldName(), fieldId)) {
            throw new DataIntegrityViolationException(
                    "Duplicate entry for key 'uk_dataset_field'");
        }

        FieldDataType dataType = FieldDataType.fromString(request.dataType());

        entity.setFieldName(request.fieldName());
        entity.setFieldAlias(request.fieldAlias());
        entity.setDataType(dataType);
        entity.setIsDimension(request.isDimension() != null ? request.isDimension() : false);
        entity.setIsMetric(request.isMetric() != null ? request.isMetric() : false);
        entity.setIsFilterable(request.isFilterable() != null ? request.isFilterable() : false);
        entity.setIsSensitive(request.isSensitive() != null ? request.isSensitive() : false);
        entity.setDescription(request.description());
        return DatasetFieldResponse.from(fieldRepo.save(entity));
    }

    @Transactional
    @Audit(action = "FIELD_DELETE", resourceType = "FIELD")
    public void deleteField(Long datasetId, Long fieldId) {
        ensureDatasetExists(datasetId);
        if (!fieldRepo.existsById(fieldId)) {
            throw new ResourceNotFoundException("DatasetField", fieldId);
        }
        fieldRepo.deleteById(fieldId);
    }

    // ---- Metrics ----

    public PagedResponse<MetricsDefinitionResponse> listMetrics(Long datasetId, int page, int size) {
        ensureDatasetExists(datasetId);
        Page<MetricsDefinitionEntity> result = metricRepo.findByDatasetId(datasetId, PageRequest.of(page, size));
        return PagedResponse.from(result.map(MetricsDefinitionResponse::from));
    }

    public List<MetricsDefinitionResponse> listAllMetrics(Long datasetId) {
        ensureDatasetExists(datasetId);
        return metricRepo.findAllByDatasetId(datasetId).stream()
                .map(MetricsDefinitionResponse::from)
                .toList();
    }

    @Transactional
    @Audit(action = "METRIC_CREATE", resourceType = "METRIC")
    public MetricsDefinitionResponse createMetric(Long datasetId, MetricsDefinitionRequest request) {
        ensureDatasetExists(datasetId);
        MetricsDefinitionEntity entity = new MetricsDefinitionEntity();
        entity.setDatasetId(datasetId);
        entity.setMetricName(request.metricName());
        entity.setFormula(request.formula());
        entity.setDescription(request.description());
        return MetricsDefinitionResponse.from(metricRepo.save(entity));
    }

    @Transactional
    @Audit(action = "METRIC_UPDATE", resourceType = "METRIC")
    public MetricsDefinitionResponse updateMetric(Long datasetId, Long metricId, MetricsDefinitionRequest request) {
        ensureDatasetExists(datasetId);
        MetricsDefinitionEntity entity = findMetric(metricId);
        entity.setMetricName(request.metricName());
        entity.setFormula(request.formula());
        entity.setDescription(request.description());
        return MetricsDefinitionResponse.from(metricRepo.save(entity));
    }

    @Transactional
    @Audit(action = "METRIC_DELETE", resourceType = "METRIC")
    public void deleteMetric(Long datasetId, Long metricId) {
        ensureDatasetExists(datasetId);
        if (!metricRepo.existsById(metricId)) {
            throw new ResourceNotFoundException("MetricsDefinition", metricId);
        }
        metricRepo.deleteById(metricId);
    }

    // ---- Context (for Agent pipeline) ----

    public DatasetContextResponse getContext(Long datasetId) {
        DatasetEntity dataset = findDataset(datasetId);
        List<DatasetFieldResponse> fields = listAllFields(datasetId);
        List<MetricsDefinitionResponse> metrics = listAllMetrics(datasetId);
        return new DatasetContextResponse(DatasetResponse.from(dataset), fields, metrics);
    }

    // ---- Private helpers ----

    private DatasetEntity findDataset(Long id) {
        return datasetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", id));
    }

    private DatasetFieldEntity findField(Long id) {
        return fieldRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetField", id));
    }

    private MetricsDefinitionEntity findMetric(Long id) {
        return metricRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MetricsDefinition", id));
    }

    private void ensureDatasetExists(Long datasetId) {
        if (!datasetRepo.existsById(datasetId)) {
            throw new ResourceNotFoundException("Dataset", datasetId);
        }
    }
}
