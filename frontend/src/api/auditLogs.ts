import apiClient from './client'
import type { Paged } from './adminModels'

export interface AuditLog {
  id: number
  operatorName: string | null
  userId: number | null
  action: string
  resourceType: string | null
  resourceId: number | null
  result: string
  detail: string | null
  ipAddress: string | null
  createdAt: string
}

export interface AuditLogFilters {
  operator?: string
  action?: string
  start?: string
  end?: string
  page?: number
  size?: number
}

export async function getAuditLogs(filters: AuditLogFilters = {}): Promise<Paged<AuditLog>> {
  const params: Record<string, string | number> = {
    page: filters.page ?? 0,
    size: filters.size ?? 20,
  }
  if (filters.operator) params.operator = filters.operator
  if (filters.action) params.action = filters.action
  if (filters.start) params.start = filters.start
  if (filters.end) params.end = filters.end
  const { data } = await apiClient.get('/admin/audit-logs', { params })
  return data
}
