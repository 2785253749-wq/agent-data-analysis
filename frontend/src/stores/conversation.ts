import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ConversationSummary, ConversationDetail } from '@/api/conversations'
import {
  getConversationList, getConversation, createConversation,
  updateConversation, archiveConversation, sendMessage,
} from '@/api/conversations'
import type { AnalysisResponse } from '@/api/datasets'

export const useConversationStore = defineStore('conversation', () => {
  // ---- List ----
  const conversations = ref<ConversationSummary[]>([])
  const loading = ref(false)
  const activeId = ref<number | null>(null)

  // ---- Detail ----
  const current = ref<ConversationDetail | null>(null)
  const currentResult = ref<AnalysisResponse | null>(null)
  const sending = ref(false)

  async function fetchList() {
    loading.value = true
    try {
      const resp = await getConversationList('ACTIVE', 0, 50)
      conversations.value = resp.content
    } finally {
      loading.value = false
    }
  }

  async function open(id: number) {
    activeId.value = id
    current.value = await getConversation(id)
    currentResult.value = null
  }

  async function create(title: string, datasetId: number | null) {
    const conv = await createConversation(title, datasetId)
    await fetchList()
    await open(conv.id)
    return conv
  }

  async function rename(id: number, title: string) {
    const conv = await updateConversation(id, title, current.value?.datasetId ?? null)
    await fetchList()
    return conv
  }

  async function archive(id: number) {
    await archiveConversation(id)
    if (activeId.value === id) {
      activeId.value = null
      current.value = null
    }
    await fetchList()
  }

  async function send(question: string) {
    if (!activeId.value || !current.value) return null
    sending.value = true
    try {
      const result = await sendMessage(activeId.value, question, current.value.datasetId)
      currentResult.value = result
      // Refresh detail to pick up new turn + updated context
      current.value = await getConversation(activeId.value)
      await fetchList()
      return result
    } finally {
      sending.value = false
    }
  }

  function reset() {
    activeId.value = null
    current.value = null
    currentResult.value = null
  }

  return {
    conversations, loading, activeId, current, currentResult, sending,
    fetchList, open, create, rename, archive, send, reset,
  }
})
