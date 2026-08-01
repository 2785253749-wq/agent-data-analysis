<template>
  <div class="metric-manager">
    <FilterToolbar :title="`指标口径管理${store.currentDataset?.name ? ' — ' + store.currentDataset.name : ''}`">
      <template #actions>
        <el-button type="danger" plain :disabled="!selected.length" @click="batchDelete">
          <el-icon><Delete /></el-icon>&nbsp;批量删除
        </el-button>
        <el-button type="primary" :icon="Plus" @click="startAdd">新增指标</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="指标列表">
      <el-table :data="displayRows" v-loading="store.metricsLoading" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="44" />
        <el-table-column label="指标名称" min-width="160">
          <template #default="{ row }">
            <el-input
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).metricName"
              placeholder="指标名称"
              @update:model-value="setForm(row, 'metricName', $event)"
            />
            <strong v-else>{{ row.metricName }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="计算公式" min-width="200">
          <template #default="{ row }">
            <el-input
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).formula"
              type="textarea"
              :rows="2"
              placeholder="例如：SUM(amount)"
              class="formula-input"
              @update:model-value="setForm(row, 'formula', $event)"
            />
            <code v-else>{{ row.formula }}</code>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="160">
          <template #default="{ row }">
            <el-input
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).description"
              placeholder="说明"
              @update:model-value="setForm(row, 'description', $event)"
            />
            <span v-else>{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="row.isAddRow">
              <el-button link type="primary" @click="onAdd">保存</el-button>
              <el-button link @click="cancelAdd">取消</el-button>
            </template>
            <template v-else-if="editingId === row.id">
              <el-button link type="primary" @click="onUpdate(row.id)">保存</el-button>
              <el-button link @click="cancelEdit">取消</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" :icon="Edit" @click="startEdit(row as MetricsDefinitionResponse)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="onDelete(row as MetricsDefinitionResponse)">删除</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>暂无指标定义</template>
      </el-table>

      <template #footer>
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="store.metrics.length"
          :current-page="page + 1"
          :page-size="pageSize"
          @current-change="onPageChange"
        />
      </template>
    </DataTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useAdminStore } from '@/stores/admin'
import type { MetricsDefinitionResponse } from '@/api/datasets'

interface MetricForm {
  metricName: string
  formula: string
  description: string
}

const route = useRoute()
const store = useAdminStore()
const datasetId = Number(route.params.id)

const adding = ref(false)
const editingId = ref<number | null>(null)
const selected = ref<MetricsDefinitionResponse[]>([])
const page = ref(0)
const pageSize = ref(20)

const addForm = reactive<MetricForm>({ metricName: '', formula: '', description: '' })
const editForm = reactive<MetricForm>({ metricName: '', formula: '', description: '' })

const ADD_ROW = { id: '__add__', isAddRow: true } as const

const displayRows = computed(() => {
  const start = page.value * pageSize.value
  const end = start + pageSize.value
  const base = store.metrics.slice(start, end)
  return adding.value ? [ADD_ROW, ...base] : base
})

function activeForm(row: { isAddRow?: boolean }): MetricForm {
  return row.isAddRow ? addForm : editForm
}

function setForm(row: { isAddRow?: boolean }, key: keyof MetricForm, value: unknown) {
  const form = row.isAddRow ? addForm : editForm
  ;(form as unknown as Record<string, unknown>)[key] = value
}

onMounted(async () => {
  await store.fetchDataset(datasetId)
  await store.fetchMetrics(datasetId)
})

function startAdd() { adding.value = true }
function cancelAdd() { adding.value = false; Object.assign(addForm, { metricName: '', formula: '', description: '' }) }

async function onAdd() {
  await store.createMetric(datasetId, { ...addForm })
  cancelAdd()
  ElMessage.success('指标已创建')
}

function startEdit(m: MetricsDefinitionResponse) {
  editingId.value = m.id
  Object.assign(editForm, { metricName: m.metricName, formula: m.formula, description: m.description || '' })
}

function cancelEdit() { editingId.value = null }

async function onUpdate(metricId: number) {
  await store.updateMetricAction(datasetId, metricId, { ...editForm })
  editingId.value = null
  ElMessage.success('指标已更新')
}

async function onDelete(m: MetricsDefinitionResponse) {
  await ElMessageBox.confirm(`确定删除指标"${m.metricName}"？`, '删除确认', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  await store.deleteMetricAction(datasetId, m.id)
  ElMessage.success('已删除')
}

function onPageChange(p: number) { page.value = p - 1 }

function onSelectionChange(rows: (MetricsDefinitionResponse | { isAddRow?: boolean })[]) {
  selected.value = rows.filter((r) => !('isAddRow' in r)) as MetricsDefinitionResponse[]
}

async function batchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selected.value.length} 个指标吗？`, '批量删除', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  for (const m of [...selected.value]) {
    await store.deleteMetricAction(datasetId, m.id)
  }
  selected.value = []
  ElMessage.success('批量删除完成')
}
</script>

<style scoped>
.metric-manager { padding: 0; }
</style>
