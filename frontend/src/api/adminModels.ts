import apiClient from './client'

export interface AiModel {
  id: number
  name: string
  provider: string
  baseUrl: string
  modelName: string
  timeoutMs: number
  temperature: number
  maxTokens: number
  isEnabled: boolean
  isDefault: boolean
  apiKeyConfigured: boolean
  createdAt: string
  updatedAt: string
}

export interface AiModelRequest {
  name: string
  provider: string
  baseUrl: string
  modelName: string
  timeoutMs: number
  temperature: number
  maxTokens: number
  apiKeyRef: string
  isEnabled: boolean
  isDefault: boolean
}

export interface Paged<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function getModels(page = 0, size = 20): Promise<Paged<AiModel>> {
  const { data } = await apiClient.get('/admin/models', { params: { page, size } })
  return data
}

export async function getActiveModels(): Promise<AiModel[]> {
  const { data } = await apiClient.get('/admin/models/active')
  return data
}

export async function createModel(req: AiModelRequest): Promise<AiModel> {
  const { data } = await apiClient.post('/admin/models', req)
  return data
}

export async function updateModel(id: number, req: AiModelRequest): Promise<AiModel> {
  const { data } = await apiClient.put(`/admin/models/${id}`, req)
  return data
}

export async function deleteModel(id: number): Promise<void> {
  await apiClient.delete(`/admin/models/${id}`)
}

export async function setDefaultModel(id: number): Promise<AiModel> {
  const { data } = await apiClient.post(`/admin/models/${id}/set-default`)
  return data
}
