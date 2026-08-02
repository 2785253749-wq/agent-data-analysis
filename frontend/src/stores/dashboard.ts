import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DashboardSummary } from '@/api/dashboard'
import { getDashboardSummary } from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSummary() {
    loading.value = true
    error.value = null
    try {
      summary.value = await getDashboardSummary()
    } catch (e: any) {
      error.value = e.response?.data?.error || '加载失败'
    } finally {
      loading.value = false
    }
  }

  const isEmpty = () =>
    summary.value && summary.value.analysisCount === 0 && summary.value.datasetCount === 0

  return { summary, loading, error, fetchSummary, isEmpty }
})
