import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TaskSummary, TaskDetail, TaskListParams } from '@/api/analysis'
import { getTaskList, getTaskDetail } from '@/api/analysis'

export const useHistoryStore = defineStore('history', () => {
  // ---- List ----
  const tasks = ref<TaskSummary[]>([])
  const loading = ref(false)
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)

  // Filters
  const status = ref<string>('')
  const keyword = ref<string>('')
  const datasetIds = ref<number[]>([])

  // Detail
  const currentTask = ref<TaskDetail | null>(null)
  const detailLoading = ref(false)

  async function fetchTasks() {
    loading.value = true
    try {
      const params: TaskListParams = {
        page: page.value,
        size: size.value,
        status: status.value || undefined,
        keyword: keyword.value || undefined,
        datasetIds: datasetIds.value.length ? datasetIds.value : undefined,
      }
      const result = await getTaskList(params)
      tasks.value = result.content
      page.value = result.page
      total.value = result.totalElements
    } finally {
      loading.value = false
    }
  }

  function setPage(p: number) {
    page.value = p
    return fetchTasks()
  }

  function setSize(s: number) {
    size.value = s
    page.value = 0
    return fetchTasks()
  }

  function applyFilters(f: { status?: string; keyword?: string; datasetIds?: number[] }) {
    if (f.status !== undefined) status.value = f.status
    if (f.keyword !== undefined) keyword.value = f.keyword
    if (f.datasetIds !== undefined) datasetIds.value = f.datasetIds
    page.value = 0
    return fetchTasks()
  }

  function resetFilters() {
    status.value = ''
    keyword.value = ''
    datasetIds.value = []
    page.value = 0
    return fetchTasks()
  }

  async function fetchDetail(id: number) {
    detailLoading.value = true
    try {
      currentTask.value = await getTaskDetail(id)
      return currentTask.value
    } finally {
      detailLoading.value = false
    }
  }

  return {
    tasks, loading, page, size, total,
    status, keyword, datasetIds,
    currentTask, detailLoading,
    fetchTasks, setPage, setSize, applyFilters, resetFilters, fetchDetail,
  }
})
