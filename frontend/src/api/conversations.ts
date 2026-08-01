import apiClient from './client'
import type { AnalysisResponse } from './datasets'

export interface ConversationSummary {
  id: number
  title: string
  status: string
  datasetId: number | null
  taskCount: number
  createdAt: string
  updatedAt: string
}

export interface ConversationTurn {
  taskId: number
  question: string
  status: string
  durationMs: number | null
  createdAt: string
}

export interface ConversationDetail {
  id: number
  title: string
  status: string
  datasetId: number | null
  taskCount: number
  contextSummary: Record<string, unknown>
  turns: ConversationTurn[]
  createdAt: string
  updatedAt: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ---- API ----

export async function getConversationList(status = 'ACTIVE', page = 0, size = 50): Promise<PagedResponse<ConversationSummary>> {
  const { data } = await apiClient.get('/conversations', { params: { status, page, size } })
  return data
}

export async function getConversation(id: number): Promise<ConversationDetail> {
  const { data } = await apiClient.get(`/conversations/${id}`)
  return data
}

export async function createConversation(title: string, datasetId: number | null): Promise<ConversationSummary> {
  const { data } = await apiClient.post('/conversations', { title, datasetId })
  return data
}

export async function updateConversation(id: number, title: string, datasetId: number | null): Promise<ConversationSummary> {
  const { data } = await apiClient.put(`/conversations/${id}`, { title, datasetId })
  return data
}

export async function archiveConversation(id: number): Promise<void> {
  await apiClient.delete(`/conversations/${id}`)
}

export async function sendMessage(id: number, question: string, datasetId: number | null): Promise<AnalysisResponse> {
  const { data } = await apiClient.post(`/conversations/${id}/messages`, { question, datasetId })
  return data
}
