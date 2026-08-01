import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const modelMocks = vi.hoisted(() => ({
  getModels: vi.fn(),
  createModel: vi.fn(),
  updateModel: vi.fn(),
  deleteModel: vi.fn(),
  setDefaultModel: vi.fn(),
}))
const promptMocks = vi.hoisted(() => ({
  getPrompts: vi.fn(),
  createPrompt: vi.fn(),
  enablePrompt: vi.fn(),
  disablePrompt: vi.fn(),
}))

vi.mock('@/api/adminModels', () => ({
  getModels: modelMocks.getModels,
  createModel: modelMocks.createModel,
  updateModel: modelMocks.updateModel,
  deleteModel: modelMocks.deleteModel,
  setDefaultModel: modelMocks.setDefaultModel,
}))
vi.mock('@/api/prompts', () => ({
  getPrompts: promptMocks.getPrompts,
  createPrompt: promptMocks.createPrompt,
  updatePromptMeta: vi.fn(),
  enablePrompt: promptMocks.enablePrompt,
  disablePrompt: promptMocks.disablePrompt,
  archivePrompt: vi.fn(),
}))

import { useAiModelStore } from '@/stores/aiModel'
import { usePromptStore } from '@/stores/prompt'

describe('aiModel store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads models', async () => {
    modelMocks.getModels.mockResolvedValue({
      content: [{ id: 1, name: 'DeepSeek', isDefault: true, apiKeyConfigured: true }],
      totalElements: 1,
    })
    const store = useAiModelStore()
    await store.fetchModels()
    expect(store.models).toHaveLength(1)
    expect(store.models[0].name).toBe('DeepSeek')
  })

  it('marks default model', async () => {
    modelMocks.setDefaultModel.mockResolvedValue({ id: 2, isDefault: true })
    const store = useAiModelStore()
    await store.setDefault(2)
    expect(modelMocks.setDefaultModel).toHaveBeenCalledWith(2)
  })
})

describe('prompt store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('filters by type', async () => {
    promptMocks.getPrompts.mockResolvedValue({ content: [], totalElements: 0 })
    const store = usePromptStore()
    await store.setType('SQL_GENERATION')
    expect(promptMocks.getPrompts).toHaveBeenCalledWith('SQL_GENERATION', 0, 20)
    expect(store.typeFilter).toBe('SQL_GENERATION')
  })

  it('enables a prompt', async () => {
    promptMocks.getPrompts.mockResolvedValue({ content: [], totalElements: 0 })
    const store = usePromptStore()
    await store.enable(5)
    expect(promptMocks.enablePrompt).toHaveBeenCalledWith(5)
  })
})
