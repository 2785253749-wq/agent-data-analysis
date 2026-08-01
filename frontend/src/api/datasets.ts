import apiClient from './client'

// ---- Types ----

export interface ChartSpecDTO {
  type: string
  title: string | null
  labels: string[]
  datasets: { label: string; data: number[]; color: string | null }[]
  options?: Record<string, unknown>
}

export interface IntentDTO {
  intentType: string
  metrics: string[]
  dimensions: string[]
  filters: { field: string; operator: string; value: string; value2?: string }[]
  timeRange: { type: string; start: string | null; end: string | null } | null
  comparison: string | null
  needsClarification: boolean
  clarificationQuestions: string[]
}

export interface AnalysisResponse {
  taskId: number
  question: string
  status: string
  intent?: IntentDTO
  sqlResult?: { sql: string; explanation: string; usedTables: string[]; usedFields: string[]; parameters?: Record<string, string> }
  validationResult?: { passed: boolean; reason: string; violations: string[] }
  queryResult?: { columns: string[]; rows: Record<string, unknown>[]; rowCount: number; executionTimeMs: number; truncated: boolean; summary: string }
  interpretation?: {
    conclusion: string
    points: { statement: string; type: string; evidence: string; confidence: number }[]
    dataSufficient: boolean
    confidence: string
    caveats: string[]
  }
  chartSpec?: { type: string; title: string; labels: string[]; datasets: { label: string; data: number[]; color: string | null }[] }
  errorMessage?: string
  steps: { stepType: string; status: string; durationMs: number }[]
  createdAt: string
  completedAt: string | null
}

export interface DatasetResponse {
  id: number
  name: string
  description: string | null
  tableName: string
  orgId: number
  isEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface DatasetRequest {
  name: string
  description?: string
  tableName: string
  orgId: number
  isEnabled?: boolean
}

export interface DatasetFieldResponse {
  id: number
  datasetId: number
  fieldName: string
  fieldAlias: string | null
  dataType: string
  isDimension: boolean
  isMetric: boolean
  isFilterable: boolean
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface DatasetFieldRequest {
  fieldName: string
  fieldAlias?: string
  dataType: string
  isDimension?: boolean
  isMetric?: boolean
  isFilterable?: boolean
  description?: string
}

export interface MetricsDefinitionResponse {
  id: number
  datasetId: number
  metricName: string
  formula: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface MetricsDefinitionRequest {
  metricName: string
  formula: string
  description?: string
}

export interface DatasetContextResponse {
  dataset: DatasetResponse
  fields: DatasetFieldResponse[]
  metrics: MetricsDefinitionResponse[]
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ---- Dataset API ----

export async function getDatasetList(
  page = 0, size = 20, search?: string
): Promise<PagedResponse<DatasetResponse>> {
  const params: Record<string, string | number> = { page, size }
  if (search) params.search = search
  const { data } = await apiClient.get('/admin/datasets', { params })
  return data
}

export async function getDataset(id: number): Promise<DatasetResponse> {
  const { data } = await apiClient.get(`/admin/datasets/${id}`)
  return data
}

export async function createDataset(req: DatasetRequest): Promise<DatasetResponse> {
  const { data } = await apiClient.post('/admin/datasets', req)
  return data
}

export async function updateDataset(id: number, req: DatasetRequest): Promise<DatasetResponse> {
  const { data } = await apiClient.put(`/admin/datasets/${id}`, req)
  return data
}

export async function deleteDataset(id: number): Promise<void> {
  await apiClient.delete(`/admin/datasets/${id}`)
}

// ---- Field API ----

export async function getFieldList(
  datasetId: number, page = 0, size = 20
): Promise<PagedResponse<DatasetFieldResponse>> {
  const { data } = await apiClient.get(`/admin/datasets/${datasetId}/fields`, { params: { page, size } })
  return data
}

export async function createField(
  datasetId: number, req: DatasetFieldRequest
): Promise<DatasetFieldResponse> {
  const { data } = await apiClient.post(`/admin/datasets/${datasetId}/fields`, req)
  return data
}

export async function updateField(
  datasetId: number, fieldId: number, req: DatasetFieldRequest
): Promise<DatasetFieldResponse> {
  const { data } = await apiClient.put(`/admin/datasets/${datasetId}/fields/${fieldId}`, req)
  return data
}

export async function deleteField(datasetId: number, fieldId: number): Promise<void> {
  await apiClient.delete(`/admin/datasets/${datasetId}/fields/${fieldId}`)
}

// ---- Metric API ----

export async function getMetricList(
  datasetId: number, page = 0, size = 20
): Promise<PagedResponse<MetricsDefinitionResponse>> {
  const { data } = await apiClient.get(`/admin/datasets/${datasetId}/metrics`, { params: { page, size } })
  return data
}

export async function createMetric(
  datasetId: number, req: MetricsDefinitionRequest
): Promise<MetricsDefinitionResponse> {
  const { data } = await apiClient.post(`/admin/datasets/${datasetId}/metrics`, req)
  return data
}

export async function updateMetric(
  datasetId: number, metricId: number, req: MetricsDefinitionRequest
): Promise<MetricsDefinitionResponse> {
  const { data } = await apiClient.put(`/admin/datasets/${datasetId}/metrics/${metricId}`, req)
  return data
}

export async function deleteMetric(datasetId: number, metricId: number): Promise<void> {
  await apiClient.delete(`/admin/datasets/${datasetId}/metrics/${metricId}`)
}

// ---- Context API ----

export async function getDatasetContext(id: number): Promise<DatasetContextResponse> {
  const { data } = await apiClient.get(`/datasets/${id}/context`)
  return data
}
