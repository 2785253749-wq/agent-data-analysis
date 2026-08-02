import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AuditLog, AuditLogFilters } from '@/api/auditLogs'
import { getAuditLogs } from '@/api/auditLogs'

export const useAuditLogStore = defineStore('auditLog', () => {
  const logs = ref<AuditLog[]>([])
  const loading = ref(false)
  const page = ref(0)
  const size = ref(20)
  const total = ref(0)

  const operator = ref('')
  const action = ref('')
  const start = ref('')
  const end = ref('')

  async function fetchLogs() {
    loading.value = true
    try {
      const filters: AuditLogFilters = {
        page: page.value,
        size: size.value,
        operator: operator.value || undefined,
        action: action.value || undefined,
        start: start.value || undefined,
        end: end.value || undefined,
      }
      const r = await getAuditLogs(filters)
      logs.value = r.content
      total.value = r.totalElements
    } finally {
      loading.value = false
    }
  }

  function applyFilters() {
    page.value = 0
    return fetchLogs()
  }

  function reset() {
    operator.value = ''
    action.value = ''
    start.value = ''
    end.value = ''
    page.value = 0
    return fetchLogs()
  }

  return {
    logs, loading, page, size, total, operator, action, start, end,
    fetchLogs, applyFilters, reset,
  }
})
