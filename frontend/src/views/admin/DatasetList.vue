<template>
  <div class="dataset-list">
    <div class="toolbar">
      <h2>数据集管理</h2>
      <div class="actions">
        <input
          v-model="search"
          type="text"
          placeholder="搜索数据集名称..."
          class="search-input"
          @keyup.enter="onSearch"
        />
        <button class="btn btn-primary" @click="goNew">+ 新建数据集</button>
      </div>
    </div>

    <div v-if="store.datasetsLoading" class="loading">加载中...</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>表名</th>
          <th>组织ID</th>
          <th>状态</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ds in store.datasets" :key="ds.id">
          <td>{{ ds.id }}</td>
          <td>{{ ds.name }}</td>
          <td><code>{{ ds.tableName }}</code></td>
          <td>{{ ds.orgId }}</td>
          <td>
            <span :class="['badge', ds.isEnabled ? 'enabled' : 'disabled']">
              {{ ds.isEnabled ? '启用' : '禁用' }}
            </span>
          </td>
          <td>{{ formatDate(ds.createdAt) }}</td>
          <td class="actions-cell">
            <button class="btn-sm" @click="goEdit(ds.id)">编辑</button>
            <button class="btn-sm" @click="goFields(ds)">字段</button>
            <button class="btn-sm" @click="goMetrics(ds)">指标</button>
            <button class="btn-sm btn-danger" @click="confirmDelete(ds)">删除</button>
          </td>
        </tr>
        <tr v-if="store.datasets.length === 0">
          <td colspan="7" class="empty">暂无数据集</td>
        </tr>
      </tbody>
    </table>

    <div class="pagination" v-if="store.datasetsTotal > store.datasetsSize">
      <button
        :disabled="store.datasetsPage === 0"
        @click="goPage(store.datasetsPage - 1)"
      >
        上一页
      </button>
      <span>第 {{ store.datasetsPage + 1 }} 页 / 共 {{ totalPages }} 页</span>
      <button
        :disabled="store.datasetsPage >= totalPages - 1"
        @click="goPage(store.datasetsPage + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import type { DatasetResponse } from '@/api/datasets'

const router = useRouter()
const store = useAdminStore()
const search = ref('')

const totalPages = computed(() =>
  Math.max(1, Math.ceil(store.datasetsTotal / store.datasetsSize))
)

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('zh-CN')
}

function onSearch() {
  store.datasetsSearch = search.value
  store.datasetsPage = 0
  store.fetchDatasets(0)
}

function goPage(page: number) {
  store.fetchDatasets(page)
}

function goNew() {
  router.push('/admin/datasets/new')
}

function goEdit(id: number) {
  router.push(`/admin/datasets/${id}`)
}

function goFields(ds: DatasetResponse) {
  store.currentDataset = ds
  router.push(`/admin/datasets/${ds.id}/fields`)
}

function goMetrics(ds: DatasetResponse) {
  store.currentDataset = ds
  router.push(`/admin/datasets/${ds.id}/metrics`)
}

async function confirmDelete(ds: DatasetResponse) {
  if (confirm(`确定要删除数据集"${ds.name}"吗？\n\n此操作将同时删除该数据集的所有字段和指标定义，不可恢复。`)) {
    await store.deleteDatasetAction(ds.id)
  }
}

onMounted(() => {
  store.fetchDatasets()
})
</script>

<style scoped>
.dataset-list { max-width: 1200px; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar h2 { font-size: 20px; }

.actions {
  display: flex;
  gap: 12px;
}

.search-input {
  padding: 6px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
  width: 200px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}

.data-table th {
  background: #f5f7fa;
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  color: #666;
  font-weight: 600;
  white-space: nowrap;
}

.data-table td {
  padding: 10px 16px;
  font-size: 14px;
  border-top: 1px solid #f0f0f0;
}

.data-table code {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.badge {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.badge.enabled { background: #e6f7e6; color: #2e7d32; }
.badge.disabled { background: #fdecea; color: #c62828; }

.actions-cell {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1a1a2e;
  color: #fff;
}

.btn-primary:hover { background: #2a2a4e; }

.btn-sm {
  padding: 4px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}

.btn-sm:hover { background: #f0f2f5; }

.btn-danger { color: #c62828; border-color: #e0b0b0; }
.btn-danger:hover { background: #fdecea; }

.empty {
  text-align: center;
  padding: 40px;
  color: #888;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #888;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  font-size: 14px;
}

.pagination button {
  padding: 6px 16px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
}

.pagination button:disabled { opacity: 0.4; cursor: default; }
</style>
