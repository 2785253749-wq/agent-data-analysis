import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const mocks = vi.hoisted(() => ({ getAuditLogs: vi.fn() }))
vi.mock('@/api/auditLogs', () => ({ getAuditLogs: mocks.getAuditLogs }))

import { useAuditLogStore } from '@/stores/auditLog'

describe('audit log store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads logs with filters', async () => {
    mocks.getAuditLogs.mockResolvedValue({
      content: [{ id: 1, operatorName: 'admin', action: 'DATASET_CREATE', result: 'SUCCESS', createdAt: '2026-08-02T10:00:00' }],
      totalElements: 1,
    })
    const store = useAuditLogStore()
    store.operator = 'admin'
    store.action = 'DATASET_CREATE'
    await store.fetchLogs()

    expect(mocks.getAuditLogs).toHaveBeenCalledWith(expect.objectContaining({
      operator: 'admin', action: 'DATASET_CREATE',
    }))
    expect(store.logs).toHaveLength(1)
    expect(store.logs[0].action).toBe('DATASET_CREATE')
  })

  it('resets filters and page', async () => {
    mocks.getAuditLogs.mockResolvedValue({ content: [], totalElements: 0 })
    const store = useAuditLogStore()
    store.operator = 'x'
    store.action = 'y'
    store.page = 3
    await store.reset()

    expect(store.operator).toBe('')
    expect(store.action).toBe('')
    expect(store.page).toBe(0)
  })
})
