import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  DatasetResponse, DatasetRequest,
  DatasetFieldResponse, DatasetFieldRequest,
  MetricsDefinitionResponse, MetricsDefinitionRequest,
  PagedResponse,
} from '@/api/datasets'
import * as api from '@/api/datasets'

export const useAdminStore = defineStore('admin', () => {
  // ---- Datasets ----
  const datasets = ref<DatasetResponse[]>([])
  const datasetsLoading = ref(false)
  const datasetsPage = ref(0)
  const datasetsSize = ref(20)
  const datasetsTotal = ref(0)
  const datasetsSearch = ref('')

  const currentDataset = ref<DatasetResponse | null>(null)

  async function fetchDatasets(page = datasetsPage.value, size = datasetsSize.value) {
    datasetsLoading.value = true
    try {
      const result: PagedResponse<DatasetResponse> = await api.getDatasetList(page, size, datasetsSearch.value || undefined)
      datasets.value = result.content
      datasetsPage.value = result.page
      datasetsTotal.value = result.totalElements
    } finally {
      datasetsLoading.value = false
    }
  }

  async function fetchDataset(id: number) {
    currentDataset.value = await api.getDataset(id)
    return currentDataset.value
  }

  async function createDataset(req: DatasetRequest) {
    const created = await api.createDataset(req)
    await fetchDatasets()
    return created
  }

  async function updateDatasetAction(id: number, req: DatasetRequest) {
    const updated = await api.updateDataset(id, req)
    currentDataset.value = updated
    await fetchDatasets()
    return updated
  }

  async function deleteDatasetAction(id: number) {
    await api.deleteDataset(id)
    if (currentDataset.value?.id === id) currentDataset.value = null
    await fetchDatasets()
  }

  // ---- Fields ----
  const fields = ref<DatasetFieldResponse[]>([])
  const fieldsLoading = ref(false)

  async function fetchFields(datasetId: number) {
    fieldsLoading.value = true
    try {
      const result = await api.getFieldList(datasetId, 0, 100)
      fields.value = result.content
    } finally {
      fieldsLoading.value = false
    }
  }

  async function createField(datasetId: number, req: DatasetFieldRequest) {
    const created = await api.createField(datasetId, req)
    await fetchFields(datasetId)
    return created
  }

  async function updateFieldAction(datasetId: number, fieldId: number, req: DatasetFieldRequest) {
    const updated = await api.updateField(datasetId, fieldId, req)
    await fetchFields(datasetId)
    return updated
  }

  async function deleteFieldAction(datasetId: number, fieldId: number) {
    await api.deleteField(datasetId, fieldId)
    await fetchFields(datasetId)
  }

  // ---- Metrics ----
  const metrics = ref<MetricsDefinitionResponse[]>([])
  const metricsLoading = ref(false)

  async function fetchMetrics(datasetId: number) {
    metricsLoading.value = true
    try {
      const result = await api.getMetricList(datasetId, 0, 100)
      metrics.value = result.content
    } finally {
      metricsLoading.value = false
    }
  }

  async function createMetric(datasetId: number, req: MetricsDefinitionRequest) {
    const created = await api.createMetric(datasetId, req)
    await fetchMetrics(datasetId)
    return created
  }

  async function updateMetricAction(datasetId: number, metricId: number, req: MetricsDefinitionRequest) {
    const updated = await api.updateMetric(datasetId, metricId, req)
    await fetchMetrics(datasetId)
    return updated
  }

  async function deleteMetricAction(datasetId: number, metricId: number) {
    await api.deleteMetric(datasetId, metricId)
    await fetchMetrics(datasetId)
  }

  return {
    datasets, datasetsLoading, datasetsPage, datasetsSize, datasetsTotal, datasetsSearch,
    currentDataset,
    fetchDatasets, fetchDataset, createDataset, updateDatasetAction, deleteDatasetAction,
    fields, fieldsLoading,
    fetchFields, createField, updateFieldAction, deleteFieldAction,
    metrics, metricsLoading,
    fetchMetrics, createMetric, updateMetricAction, deleteMetricAction,
  }
})
