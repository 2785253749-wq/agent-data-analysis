import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PromptTemplate, PromptCreateRequest } from '@/api/prompts'
import { getPrompts, createPrompt, updatePromptMeta, enablePrompt, disablePrompt, archivePrompt } from '@/api/prompts'

export const usePromptStore = defineStore('prompt', () => {
  const prompts = ref<PromptTemplate[]>([])
  const loading = ref(false)
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)
  const typeFilter = ref('')

  async function fetchPrompts() {
    loading.value = true
    try {
      const r = await getPrompts(typeFilter.value || undefined, page.value, size.value)
      prompts.value = r.content
      total.value = r.totalElements
    } finally {
      loading.value = false
    }
  }

  async function create(req: PromptCreateRequest) {
    await createPrompt(req)
    await fetchPrompts()
  }

  async function updateDescription(id: number, desc: string) {
    await updatePromptMeta(id, desc)
    await fetchPrompts()
  }

  async function enable(id: number) {
    await enablePrompt(id)
    await fetchPrompts()
  }

  async function disable(id: number) {
    await disablePrompt(id)
    await fetchPrompts()
  }

  async function archive(id: number) {
    await archivePrompt(id)
    await fetchPrompts()
  }

  function setType(t: string) {
    typeFilter.value = t
    page.value = 0
    return fetchPrompts()
  }

  return {
    prompts, loading, page, size, total, typeFilter,
    fetchPrompts, create, updateDescription, enable, disable, archive, setType,
  }
})
