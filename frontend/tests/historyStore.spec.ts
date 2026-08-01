import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock the analysis API module used by the store.
const mocks = vi.hoisted(() => ({
  getTaskList: vi.fn(),
  getTaskDetail: vi.fn(),
}))

vi.mock('@/api/analysis', () => ({
  getTaskList: mocks.getTaskList,
  getTaskDetail: mocks.getTaskDetail,
}))

import { useHistoryStore } from '@/stores/history'

describe('history store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads a task list with pagination', async () => {
    mocks.getTaskList.mockResolvedValue({
      content: [{ taskId: 1, question: '按地区汇总', status: 'COMPLETED', durationMs: 1200, datasetId: 1, datasetName: '销售数据', createdAt: '2026-08-02T10:00:00', completedAt: null }],
      page: 0, size: 20, totalElements: 1, totalPages: 1,
    })

    const store = useHistoryStore()
    await store.fetchTasks()

    expect(store.tasks).toHaveLength(1)
    expect(store.tasks[0].question).toBe('按地区汇总')
    expect(store.total).toBe(1)
    expect(store.loading).toBe(false)
  })

  it('applies filters and resets page to 0', async () => {
    mocks.getTaskList.mockResolvedValue({
      content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
    })

    const store = useHistoryStore()
    store.page = 3
    await store.applyFilters({ status: 'FAILED', keyword: '销售', datasetIds: [1] })

    expect(store.page).toBe(0)
    expect(store.status).toBe('FAILED')
    expect(store.keyword).toBe('销售')
    expect(mocks.getTaskList).toHaveBeenCalledWith(expect.objectContaining({
      status: 'FAILED', keyword: '销售', datasetIds: [1],
    }))
  })

  it('resets filters on resetFilters', async () => {
    mocks.getTaskList.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })

    const store = useHistoryStore()
    store.status = 'FAILED'
    store.keyword = 'x'
    store.datasetIds = [1]
    await store.resetFilters()

    expect(store.status).toBe('')
    expect(store.keyword).toBe('')
    expect(store.datasetIds).toEqual([])
  })

  it('fetches task detail', async () => {
    mocks.getTaskDetail.mockResolvedValue({
      taskId: 5, question: 'q', status: 'COMPLETED', steps: [{ stepType: 'INTENT', status: 'COMPLETED', durationMs: 100, errorMessage: null }],
    })

    const store = useHistoryStore()
    await store.fetchDetail(5)

    expect(store.currentTask?.taskId).toBe(5)
    expect(store.currentTask?.steps).toHaveLength(1)
  })
})
