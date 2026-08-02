import apiClient from './client'

export interface TrendPoint {
  date: string
  count: number
}

export interface RecentTask {
  taskId: number
  question: string
  status: string
  datasetName: string | null
  createdAt: string
  durationMs: number | null
}

export interface FailureCount {
  reason: string
  count: number
}

export interface DashboardSummary {
  datasetCount: number
  analysisCount: number
  successRate: number | null
  last7DaysTrend: TrendPoint[]
  recentTasks: RecentTask[]
  commonFailures: FailureCount[]
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await apiClient.get('/dashboard/summary')
  return data
}
