<template>
  <div class="metric-manager">
    <div class="toolbar">
      <h2>指标管理 — {{ store.currentDataset?.name }}</h2>
      <button class="btn btn-primary" @click="startAdd" v-if="!adding">+ 新增指标</button>
    </div>

    <div v-if="store.metricsLoading" class="loading">加载中...</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>指标名称</th>
          <th>计算公式</th>
          <th>描述</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="adding">
          <td><input v-model="addForm.metricName" type="text" class="cell-input" placeholder="指标名称" /></td>
          <td><textarea v-model="addForm.formula" class="cell-textarea" placeholder="例如：SUM(amount)" rows="2"></textarea></td>
          <td><input v-model="addForm.description" type="text" class="cell-input" placeholder="说明" /></td>
          <td>
            <button class="btn-sm btn-save" @click="onAdd">保存</button>
            <button class="btn-sm" @click="cancelAdd">取消</button>
          </td>
        </tr>
        <tr v-for="m in store.metrics" :key="m.id">
          <template v-if="editingId === m.id">
            <td><input v-model="editForm.metricName" type="text" class="cell-input" /></td>
            <td><textarea v-model="editForm.formula" class="cell-textarea" rows="2"></textarea></td>
            <td><input v-model="editForm.description" type="text" class="cell-input" /></td>
            <td>
              <button class="btn-sm btn-save" @click="onUpdate(m.id)">保存</button>
              <button class="btn-sm" @click="cancelEdit">取消</button>
            </td>
          </template>
          <template v-else>
            <td><strong>{{ m.metricName }}</strong></td>
            <td><code>{{ m.formula }}</code></td>
            <td class="desc-cell">{{ m.description || '-' }}</td>
            <td>
              <button class="btn-sm" @click="startEdit(m)">编辑</button>
              <button class="btn-sm btn-danger" @click="onDelete(m)">删除</button>
            </td>
          </template>
        </tr>
        <tr v-if="store.metrics.length === 0 && !adding">
          <td colspan="4" class="empty">暂无指标定义</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import type { MetricsDefinitionResponse } from '@/api/datasets'

const route = useRoute()
const store = useAdminStore()
const datasetId = Number(route.params.id)

const adding = ref(false)
const editingId = ref<number | null>(null)

const addForm = reactive({ metricName: '', formula: '', description: '' })
const editForm = reactive({ metricName: '', formula: '', description: '' })

onMounted(async () => {
  await store.fetchDataset(datasetId)
  await store.fetchMetrics(datasetId)
})

function startAdd() { adding.value = true }
function cancelAdd() { adding.value = false; Object.assign(addForm, { metricName: '', formula: '', description: '' }) }

async function onAdd() {
  await store.createMetric(datasetId, { ...addForm })
  cancelAdd()
}

function startEdit(m: MetricsDefinitionResponse) {
  editingId.value = m.id
  Object.assign(editForm, { metricName: m.metricName, formula: m.formula, description: m.description || '' })
}

function cancelEdit() { editingId.value = null }

async function onUpdate(metricId: number) {
  await store.updateMetricAction(datasetId, metricId, { ...editForm })
  editingId.value = null
}

async function onDelete(m: MetricsDefinitionResponse) {
  if (confirm(`确定删除指标"${m.metricName}"？`)) {
    await store.deleteMetricAction(datasetId, m.id)
  }
}
</script>

<style scoped>
.metric-manager { max-width: 1100px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar h2 { font-size: 20px; }

.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.data-table th { background: #f5f7fa; padding: 10px 12px; text-align: left; font-size: 13px; color: #666; font-weight: 600; }
.data-table td { padding: 8px 12px; font-size: 13px; border-top: 1px solid #f0f0f0; }
.data-table code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-size: 12px; }

.desc-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.cell-input { width: 100%; padding: 4px 6px; border: 1px solid #d0d0d0; border-radius: 3px; font-size: 13px; }
.cell-textarea { width: 100%; padding: 4px 6px; border: 1px solid #d0d0d0; border-radius: 3px; font-size: 13px; resize: vertical; font-family: monospace; }

.btn { padding: 8px 20px; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-primary { background: #1a1a2e; color: #fff; }
.btn-primary:hover { background: #2a2a4e; }
.btn-sm { padding: 4px 10px; border: 1px solid #d0d0d0; border-radius: 4px; background: #fff; cursor: pointer; font-size: 12px; }
.btn-sm:hover { background: #f0f2f5; }
.btn-save { background: #e6f7e6; border-color: #a5d6a7; color: #2e7d32; }
.btn-danger { color: #c62828; border-color: #e0b0b0; }
.btn-danger:hover { background: #fdecea; }

.empty { text-align: center; padding: 40px; color: #888; }
.loading { text-align: center; padding: 60px; color: #888; }
</style>
