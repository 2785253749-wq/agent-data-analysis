<template>
  <div class="dataset-list">
    <FilterToolbar
      title="数据集管理"
      v-model="search"
      search-placeholder="搜索数据集名称..."
      @search="onSearch"
    >
      <template #actions>
        <el-button type="danger" plain :disabled="!selected.length" @click="batchDelete">
          <el-icon><Delete /></el-icon>&nbsp;批量删除
        </el-button>
        <el-button type="primary" @click="goNew">
          <el-icon><Plus /></el-icon>&nbsp;新建数据集
        </el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="数据集列表">
      <el-table
        :data="store.datasets"
        v-loading="store.datasetsLoading"
        stripe
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="表名" min-width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.tableName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgId" label="组织ID" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" effect="light">
              {{ row.isEnabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="110">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="goEdit(row.id)">编辑</el-button>
            <el-button link type="primary" :icon="Grid" @click="goFields(row as DatasetResponse)">字段</el-button>
            <el-button link type="primary" :icon="TrendCharts" @click="goMetrics(row as DatasetResponse)">指标</el-button>
            <el-button link type="danger" :icon="Delete" @click="confirmDelete(row as DatasetResponse)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无数据集</template>
      </el-table>

      <template #footer>
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="store.datasetsTotal"
          :current-page="store.datasetsPage + 1"
          :page-size="store.datasetsSize"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="goPage"
          @size-change="onSizeChange"
        />
      </template>
    </DataTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Grid, TrendCharts } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useAdminStore } from '@/stores/admin'
import type { DatasetResponse } from '@/api/datasets'

const router = useRouter()
const store = useAdminStore()
const search = ref('')
const selected = ref<DatasetResponse[]>([])

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('zh-CN')
}

function onSearch(val: string) {
  store.datasetsSearch = val
  store.datasetsPage = 0
  store.fetchDatasets(0, store.datasetsSize)
}

function goPage(page: number) {
  store.fetchDatasets(page - 1, store.datasetsSize)
}

function onSizeChange(size: number) {
  store.datasetsSize = size
  store.fetchDatasets(0, size)
}

function goNew() {
  router.push('/datasets/new')
}

function goEdit(id: number) {
  router.push(`/datasets/${id}`)
}

function goFields(ds: DatasetResponse) {
  store.currentDataset = ds
  router.push(`/datasets/${ds.id}/fields`)
}

function goMetrics(ds: DatasetResponse) {
  store.currentDataset = ds
  router.push(`/datasets/${ds.id}/metrics`)
}

function onSelectionChange(rows: DatasetResponse[]) {
  selected.value = rows
}

async function confirmDelete(ds: DatasetResponse) {
  await ElMessageBox.confirm(
    `确定要删除数据集"${ds.name}"吗？\n\n此操作将同时删除该数据集的所有字段和指标定义，不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  await store.deleteDatasetAction(ds.id)
  ElMessage.success('已删除')
}

async function batchDelete() {
  await ElMessageBox.confirm(
    `确定要删除选中的 ${selected.value.length} 个数据集吗？`,
    '批量删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  for (const ds of [...selected.value]) {
    await store.deleteDatasetAction(ds.id)
  }
  selected.value = []
  ElMessage.success('批量删除完成')
}

onMounted(() => {
  store.fetchDatasets(0, store.datasetsSize)
})
</script>

<style scoped>
.dataset-list { padding: 0; }
</style>
