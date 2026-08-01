import apiClient from './client'

// ---- Types ----

export interface TaskSummary {
  taskId: number
  question: string
  datasetId: number | null
  datasetName: string | null
  status: string
  durationMs: number | null
  createdAt: string
  completedAt: string | null
}

export interface TaskStep {
  stepType: string
  status: string
  durationMs: number | null
  errorMessage: string | null
}

export interface TaskDetail {
  taskId: number
  question: string
  datasetId: number | null
  datasetName: string | null
  status: string
  durationMs: number | null
  createdAt: string
  completedAt: string | null
  intent: unknown
  sqlText: string | null
  parameters: Record<string, string> | null
  validation: unknown
  queryResult: unknown
  interpretation: unknown
  chartSpec: unknown
  errorMessage: string | null
  steps: TaskStep[]
}

export interface TaskListParams {
  page?: number
  size?: number
  status?: string
  datasetIds?: number[]
  keyword?: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ---- API ----

export async function getTaskList(params: TaskListParams = {}): Promise<PagedResponse<TaskSummary>> {
  const query: Record<string, string | number> = {
    page: params.page ?? 0,
    size: params.size ?? 20,
  }
  if (params.status) query.status = params.status
  if (params.keyword) query.keyword = params.keyword
  if (params.datasetIds?.length) query.datasetIds = params.datasetIds.join(',')
  const { data } = await apiClient.get('/analysis/tasks', { params: query })
  return data
}

export async function getTaskDetail(id: number): Promise<TaskDetail> {
  const { data } = await apiClient.get(`/analysis/tasks/${id}`)
  return data
}
