import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const mocks = vi.hoisted(() => ({ getDashboardSummary: vi.fn() }))
vi.mock('@/api/dashboard', () => ({ getDashboardSummary: mocks.getDashboardSummary }))

import { useDashboardStore } from '@/stores/dashboard'

describe('dashboard store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads summary', async () => {
    mocks.getDashboardSummary.mockResolvedValue({
      datasetCount: 3, analysisCount: 45, successRate: 82.3,
      last7DaysTrend: [{ date: '2026-07-27', count: 4 }],
      recentTasks: [{ taskId: 1, question: 'q', status: 'COMPLETED', datasetName: '销售数据', createdAt: '', durationMs: 100 }],
      commonFailures: [{ reason: 'SQL_VALIDATION', count: 5 }],
    })
    const store = useDashboardStore()
    await store.fetchSummary()

    expect(store.summary?.datasetCount).toBe(3)
    expect(store.summary?.successRate).toBe(82.3)
    expect(store.summary?.recentTasks).toHaveLength(1)
  })

  it('detects empty state', async () => {
    mocks.getDashboardSummary.mockResolvedValue({
      datasetCount: 0, analysisCount: 0, successRate: null,
      last7DaysTrend: [], recentTasks: [], commonFailures: [],
    })
    const store = useDashboardStore()
    await store.fetchSummary()

    expect(store.isEmpty()).toBe(true)
  })
})
