import { ref } from 'vue'
import type { AnalysisResponse } from '@/api/datasets'
import apiClient from '@/api/client'
import { saveLatestAnalysis } from '@/utils/reportStorage'

export interface StepEvent {
  type: string
  status: string
  durationMs?: number
}

export function useAnalysis() {
  const loading = ref(false)
  const result = ref<AnalysisResponse | null>(null)
  const error = ref<string | null>(null)
  const steps = ref<StepEvent[]>([])

  async function analyze(question: string, datasetId: number | null) {
    loading.value = true
    error.value = null
    result.value = null
    steps.value = []

    try {
      const { data } = await apiClient.post('/analysis/tasks', {
        question,
        datasetId,
      })
      result.value = data
      saveLatestAnalysis(data)
    } catch (e: any) {
      error.value = e.response?.data?.error || e.message || '分析失败'
    } finally {
      loading.value = false
    }
  }

  function reset() {
    loading.value = false
    result.value = null
    error.value = null
    steps.value = []
  }

  return { loading, result, error, steps, analyze, reset }
}
