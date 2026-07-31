package com.agent.controller;

import com.agent.dto.*;
import com.agent.service.DatasetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin CRUD endpoints for datasets, fields, and metrics.
 *
 * All endpoints under /api/admin/datasets require authentication.
 *
 * Contracts:
 *   GET    /api/admin/datasets                  → paginated list
 *   POST   /api/admin/datasets                  → create dataset (201)
 *   GET    /api/admin/datasets/{id}             → single dataset
 *   PUT    /api/admin/datasets/{id}             → update dataset
 *   DELETE /api/admin/datasets/{id}             → delete dataset (204)
 *
 *   GET    /api/admin/datasets/{id}/fields      → paginated fields
 *   POST   /api/admin/datasets/{id}/fields      → create field (201)
 *   PUT    /api/admin/datasets/{id}/fields/{fid} → update field
 *   DELETE /api/admin/datasets/{id}/fields/{fid} → delete field (204)
 *
 *   GET    /api/admin/datasets/{id}/metrics     → paginated metrics
 *   POST   /api/admin/datasets/{id}/metrics     → create metric (201)
 *   PUT    /api/admin/datasets/{id}/metrics/{mid} → update metric
 *   DELETE /api/admin/datasets/{id}/metrics/{mid} → delete metric (204)
 */
@RestController
@RequestMapping("/api/admin/datasets")
public class DatasetAdminController {

    private final DatasetService service;

    public DatasetAdminController(DatasetService service) {
        this.service = service;
    }

    // ========== Datasets ==========

    @GetMapping
    public ResponseEntity<PagedResponse<DatasetResponse>> listDatasets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.list(page, size, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasetResponse> getDataset(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<DatasetResponse> createDataset(@Valid @RequestBody DatasetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatasetResponse> updateDataset(@PathVariable Long id,
                                                          @Valid @RequestBody DatasetRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== Fields ==========

    @GetMapping("/{datasetId}/fields")
    public ResponseEntity<PagedResponse<DatasetFieldResponse>> listFields(
            @PathVariable Long datasetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listFields(datasetId, page, size));
    }

    @PostMapping("/{datasetId}/fields")
    public ResponseEntity<DatasetFieldResponse> createField(
            @PathVariable Long datasetId,
            @Valid @RequestBody DatasetFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createField(datasetId, request));
    }

    @PutMapping("/{datasetId}/fields/{fieldId}")
    public ResponseEntity<DatasetFieldResponse> updateField(
            @PathVariable Long datasetId,
            @PathVariable Long fieldId,
            @Valid @RequestBody DatasetFieldRequest request) {
        return ResponseEntity.ok(service.updateField(datasetId, fieldId, request));
    }

    @DeleteMapping("/{datasetId}/fields/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long datasetId,
                                             @PathVariable Long fieldId) {
        service.deleteField(datasetId, fieldId);
        return ResponseEntity.noContent().build();
    }

    // ========== Metrics ==========

    @GetMapping("/{datasetId}/metrics")
    public ResponseEntity<PagedResponse<MetricsDefinitionResponse>> listMetrics(
            @PathVariable Long datasetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listMetrics(datasetId, page, size));
    }

    @PostMapping("/{datasetId}/metrics")
    public ResponseEntity<MetricsDefinitionResponse> createMetric(
            @PathVariable Long datasetId,
            @Valid @RequestBody MetricsDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createMetric(datasetId, request));
    }

    @PutMapping("/{datasetId}/metrics/{metricId}")
    public ResponseEntity<MetricsDefinitionResponse> updateMetric(
            @PathVariable Long datasetId,
            @PathVariable Long metricId,
            @Valid @RequestBody MetricsDefinitionRequest request) {
        return ResponseEntity.ok(service.updateMetric(datasetId, metricId, request));
    }

    @DeleteMapping("/{datasetId}/metrics/{metricId}")
    public ResponseEntity<Void> deleteMetric(@PathVariable Long datasetId,
                                              @PathVariable Long metricId) {
        service.deleteMetric(datasetId, metricId);
        return ResponseEntity.noContent().build();
    }
}
