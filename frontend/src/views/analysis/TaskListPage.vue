<template>
  <div class="task-list-page">
    <FilterToolbar :title="mode === 'trace' ? 'Agent 执行追踪' : '分析历史'">
      <template #left-extra>
        <el-select
          v-model="statusFilter"
          placeholder="状态"
          clearable
          style="width: 120px"
          @change="onFilter"
        >
          <el-option label="已成功" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
          <el-option label="进行中" value="RUNNING" />
        </el-select>
        <el-select
          v-model="datasetFilter"
          placeholder="数据集"
          clearable
          multiple
          collapse-tags
          style="width: 200px"
          @change="onFilter"
        >
          <el-option v-for="ds in datasets" :key="ds.id" :label="ds.name" :value="ds.id" />
        </el-select>
        <el-input
          v-model="keywordFilter"
          placeholder="搜索问题..."
          clearable
          style="width: 180px"
          @keyup.enter="onFilter"
          @clear="onFilter"
        />
        <el-button type="primary" :icon="Search" @click="onFilter">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard :title="mode === 'trace' ? '任务执行追踪' : '历史任务列表'">
      <el-table :data="store.tasks" v-loading="store.loading" stripe>
        <el-table-column label="问题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.question }}</template>
        </el-table-column>

        <!-- /history focuses on result; /trace focuses on status + steps -->
        <template v-if="mode === 'history'">
          <el-table-column label="数据集" width="120">
            <template #default="{ row }">{{ row.datasetName || '-' }}</template>
          </el-table-column>
        </template>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <template v-if="mode === 'trace'">
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">{{ fmtDuration(row.durationMs) }}</template>
          </el-table-column>
          <el-table-column label="步骤数" width="80">
            <template #default="{ row }">{{ stepCounts[row.taskId] ?? '-' }}</template>
          </el-table-column>
        </template>

        <template v-if="mode === 'history'">
          <el-table-column label="创建时间" width="150">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
        </template>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.taskId)">查看</el-button>
            <el-button link type="primary" @click="openDetail(row.taskId)">追踪</el-button>
          </template>
        </el-table-column>
        <template #empty>{{ store.loading ? '加载中...' : '暂无任务' }}</template>
      </el-table>

      <template #footer>
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="store.total"
          :current-page="store.page + 1"
          :page-size="store.size"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="(p: number) => store.setPage(p - 1)"
          @size-change="(s: number) => store.setSize(s)"
        />
      </template>
    </DataTableCard>

    <!-- Detail drawer -->
    <el-drawer v-model="drawerVisible" size="60%" :title="drawerTitle">
      <TaskDetail
        v-if="store.currentTask"
        :task="store.currentTask"
        @refresh="store.fetchTasks()"
      />
      <el-skeleton v-else :rows="6" animated />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import TaskDetail from './TaskDetail.vue'
import { useHistoryStore } from '@/stores/history'
import { getDatasetList } from '@/api/datasets'
import type { DatasetResponse } from '@/api/datasets'

const props = defineProps<{ mode: 'history' | 'trace' }>()

const store = useHistoryStore()
const datasets = ref<DatasetResponse[]>([])
const statusFilter = ref('')
const datasetFilter = ref<number[]>([])
const keywordFilter = ref('')

const drawerVisible = ref(false)
const drawerTitle = computed(() => (props.mode === 'trace' ? '执行追踪详情' : '任务详情'))

const stepCounts = ref<Record<number, number>>({})

function onFilter() {
  store.applyFilters({
    status: statusFilter.value || undefined,
    keyword: keywordFilter.value || undefined,
    datasetIds: datasetFilter.value.length ? datasetFilter.value : undefined,
  })
}

function onReset() {
  statusFilter.value = ''
  datasetFilter.value = []
  keywordFilter.value = ''
  store.resetFilters()
}

async function openDetail(id: number) {
  drawerVisible.value = true
  await store.fetchDetail(id)
  stepCounts.value[id] = store.currentTask?.steps?.length ?? 0
}

function statusType(s: string): 'success' | 'danger' | 'warning' | 'info' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function statusLabel(s: string) {
  const m: Record<string, string> = {
    COMPLETED: '已成功', FAILED: '失败', RUNNING: '进行中', PENDING: '等待中', CANCELLED: '已取消',
  }
  return m[s] || s
}

function fmtDuration(ms: number | null) {
  if (ms == null) return '-'
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(async () => {
  try {
    const resp = await getDatasetList(0, 100)
    datasets.value = resp.content
  } catch {
    datasets.value = []
  }
  store.fetchTasks()
})
</script>

<style scoped>
.task-list-page { display: flex; flex-direction: column; }
</style>
