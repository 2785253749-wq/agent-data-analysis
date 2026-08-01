import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const mocks = vi.hoisted(() => ({
  getConversationList: vi.fn(),
  getConversation: vi.fn(),
  createConversation: vi.fn(),
  updateConversation: vi.fn(),
  archiveConversation: vi.fn(),
  sendMessage: vi.fn(),
}))

vi.mock('@/api/conversations', () => ({
  getConversationList: mocks.getConversationList,
  getConversation: mocks.getConversation,
  createConversation: mocks.createConversation,
  updateConversation: mocks.updateConversation,
  archiveConversation: mocks.archiveConversation,
  sendMessage: mocks.sendMessage,
}))

import { useConversationStore } from '@/stores/conversation'

describe('conversation store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads conversation list', async () => {
    mocks.getConversationList.mockResolvedValue({
      content: [{ id: 1, title: 'Q2销售', status: 'ACTIVE', taskCount: 3, datasetId: 1, createdAt: '', updatedAt: '' }],
      page: 0, size: 50, totalElements: 1, totalPages: 1,
    })
    const store = useConversationStore()
    await store.fetchList()
    expect(store.conversations).toHaveLength(1)
    expect(store.conversations[0].title).toBe('Q2销售')
  })

  it('creates conversation and opens it', async () => {
    mocks.getConversationList.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })
    mocks.createConversation.mockResolvedValue({ id: 9, title: '新会话', status: 'ACTIVE', taskCount: 0, datasetId: 1, createdAt: '', updatedAt: '' })
    mocks.getConversation.mockResolvedValue({ id: 9, title: '新会话', status: 'ACTIVE', datasetId: 1, taskCount: 0, contextSummary: {}, turns: [], createdAt: '', updatedAt: '' })

    const store = useConversationStore()
    await store.create('新会话', 1)

    expect(store.activeId).toBe(9)
    expect(store.current?.title).toBe('新会话')
  })

  it('archives conversation and clears active', async () => {
    mocks.getConversationList.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })
    const store = useConversationStore()
    store.activeId = 5
    store.current = { id: 5, title: 'x', status: 'ACTIVE', datasetId: null, taskCount: 0, contextSummary: {}, turns: [], createdAt: '', updatedAt: '' }

    await store.archive(5)
    expect(store.activeId).toBeNull()
    expect(mocks.archiveConversation).toHaveBeenCalledWith(5)
  })

  it('sends a message and refreshes turns', async () => {
    mocks.getConversationList.mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 })
    mocks.getConversation.mockResolvedValue({
      id: 1, title: 'x', status: 'ACTIVE', datasetId: 1, taskCount: 1,
      contextSummary: {}, turns: [{ taskId: 10, question: 'q', status: 'COMPLETED', durationMs: 100, createdAt: '' }],
      createdAt: '', updatedAt: '',
    })
    mocks.sendMessage.mockResolvedValue({ taskId: 10, question: 'q', status: 'COMPLETED', steps: [] })

    const store = useConversationStore()
    store.activeId = 1
    store.current = { id: 1, title: 'x', status: 'ACTIVE', datasetId: 1, taskCount: 0, contextSummary: {}, turns: [], createdAt: '', updatedAt: '' }

    await store.send('追问')
    expect(mocks.sendMessage).toHaveBeenCalledWith(1, '追问', 1)
    expect(store.current?.taskCount).toBe(1)
  })
})
