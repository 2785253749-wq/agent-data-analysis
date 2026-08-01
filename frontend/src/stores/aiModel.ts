import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AiModel, AiModelRequest } from '@/api/adminModels'
import { getModels, createModel, updateModel, deleteModel, setDefaultModel } from '@/api/adminModels'

export const useAiModelStore = defineStore('aiModel', () => {
  const models = ref<AiModel[]>([])
  const loading = ref(false)
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)

  async function fetchModels() {
    loading.value = true
    try {
      const r = await getModels(page.value, size.value)
      models.value = r.content
      total.value = r.totalElements
    } finally {
      loading.value = false
    }
  }

  async function create(req: AiModelRequest) {
    await createModel(req)
    await fetchModels()
  }

  async function update(id: number, req: AiModelRequest) {
    await updateModel(id, req)
    await fetchModels()
  }

  async function remove(id: number) {
    await deleteModel(id)
    await fetchModels()
  }

  async function setDefault(id: number) {
    await setDefaultModel(id)
    await fetchModels()
  }

  return { models, loading, page, size, total, fetchModels, create, update, remove, setDefault }
})
