import apiClient from './client'
import type { Paged } from './adminModels'

export interface PromptTemplate {
  id: number
  name: string
  type: string
  version: number
  content: string | null
  variables: string | null
  contentHash: string
  description: string | null
  isEnabled: boolean
  isArchived: boolean
  createdAt: string
  updatedAt: string
}

export interface PromptCreateRequest {
  name: string
  type: string
  content: string
  variables?: string
  description?: string
}

export const PROMPT_TYPES = [
  { value: 'INTENT_RECOGNITION', label: '意图识别' },
  { value: 'SQL_GENERATION', label: 'SQL 生成' },
  { value: 'INTERPRETATION', label: '数据解读' },
  { value: 'SQL_REPAIR', label: 'SQL 修复' },
] as const

export async function getPrompts(type?: string, page = 0, size = 20): Promise<Paged<PromptTemplate>> {
  const params: Record<string, string | number> = { page, size }
  if (type) params.type = type
  const { data } = await apiClient.get('/admin/prompts', { params })
  return data
}

export async function getActivePrompt(type: string): Promise<PromptTemplate> {
  const { data } = await apiClient.get('/admin/prompts/active', { params: { type } })
  return data
}

export async function createPrompt(req: PromptCreateRequest): Promise<PromptTemplate> {
  const { data } = await apiClient.post('/admin/prompts', req)
  return data
}

export async function updatePromptMeta(id: number, description: string): Promise<PromptTemplate> {
  const { data } = await apiClient.put(`/admin/prompts/${id}`, null, { params: { description } })
  return data
}

export async function enablePrompt(id: number): Promise<PromptTemplate> {
  const { data } = await apiClient.post(`/admin/prompts/${id}/enable`)
  return data
}

export async function disablePrompt(id: number): Promise<PromptTemplate> {
  const { data } = await apiClient.post(`/admin/prompts/${id}/disable`)
  return data
}

export async function archivePrompt(id: number): Promise<void> {
  await apiClient.post(`/admin/prompts/${id}/archive`)
}
