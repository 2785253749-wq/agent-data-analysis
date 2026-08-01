<template>
  <div class="field-manager">
    <FilterToolbar :title="`字段语义管理${store.currentDataset?.name ? ' — ' + store.currentDataset.name : ''}`">
      <template #actions>
        <el-button type="danger" plain :disabled="!selected.length" @click="batchDelete">
          <el-icon><Delete /></el-icon>&nbsp;批量删除
        </el-button>
        <el-button type="primary" :icon="Plus" @click="startAdd">新增字段</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="字段列表">
      <el-table :data="displayRows" v-loading="store.fieldsLoading" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="44" />
        <el-table-column label="字段名" min-width="140">
          <template #default="{ row }">
            <el-input
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).fieldName"
              placeholder="字段名"
              @update:model-value="setForm(row, 'fieldName', $event)"
            />
            <code v-else>{{ row.fieldName }}</code>
          </template>
        </el-table-column>
        <el-table-column label="别名" min-width="120">
          <template #default="{ row }">
            <el-input
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).fieldAlias"
              placeholder="别名"
              @update:model-value="setForm(row, 'fieldAlias', $event)"
            />
            <span v-else>{{ row.fieldAlias || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据类型" width="120">
          <template #default="{ row }">
            <el-select
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).dataType"
              @update:model-value="setForm(row, 'dataType', $event)"
            >
              <el-option v-for="dt in dataTypes" :key="dt" :label="dt" :value="dt" />
            </el-select>
            <el-tag v-else effect="plain">{{ row.dataType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="维度" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).isDimension"
              @update:model-value="setForm(row, 'isDimension', $event)"
            />
            <el-icon v-else :color="row.isDimension ? '#409eff' : '#c0c4cc'">
              <component :is="row.isDimension ? Check : Close" />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column label="指标" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).isMetric"
              @update:model-value="setForm(row, 'isMetric', $event)"
            />
            <el-icon v-else :color="row.isMetric ? '#409eff' : '#c0c4cc'">
              <component :is="row.isMetric ? Check : Close" />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column label="可过滤" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox
              v-if="row.isAddRow || editingId === row.id"
              :model-value="activeForm(row).isFilterable"
              @update:model-value="setForm(row, 'isFilterable', $event)"
            />
            <el-icon v-else :color="row.isFilterable ? '#409eff' : '#c0c4cc'">
              <component :is="row.isFilterable ? Check : Close" />
            </el-icon>
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
              <el-button link type="primary" :icon="Edit" @click="startEdit(row as DatasetFieldResponse)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="onDelete(row as DatasetFieldResponse)">删除</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>暂无字段定义</template>
      </el-table>

      <template #footer>
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="store.fields.length"
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
import { Plus, Delete, Edit, Check, Close } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useAdminStore } from '@/stores/admin'
import type { DatasetFieldResponse } from '@/api/datasets'

interface FieldForm {
  fieldName: string
  fieldAlias: string
  dataType: string
  isDimension: boolean
  isMetric: boolean
  isFilterable: boolean
  description: string
}

const route = useRoute()
const store = useAdminStore()
const datasetId = Number(route.params.id)

const dataTypes = ['varchar', 'int', 'bigint', 'decimal', 'datetime', 'date', 'text', 'boolean']

const adding = ref(false)
const editingId = ref<number | null>(null)
const selected = ref<DatasetFieldResponse[]>([])
const page = ref(0)
const pageSize = ref(20)

const addForm = reactive<FieldForm>({
  fieldName: '', fieldAlias: '', dataType: 'varchar',
  isDimension: false, isMetric: false, isFilterable: false, description: '',
})

const editForm = reactive<FieldForm>({
  fieldName: '', fieldAlias: '', dataType: 'varchar',
  isDimension: false, isMetric: false, isFilterable: false, description: '',
})

const ADD_ROW = { id: '__add__', isAddRow: true } as const

const displayRows = computed(() => {
  const start = page.value * pageSize.value
  const end = start + pageSize.value
  const base = store.fields.slice(start, end)
  return adding.value ? [ADD_ROW, ...base] : base
})

function activeForm(row: { isAddRow?: boolean }): FieldForm {
  return row.isAddRow ? addForm : editForm
}

function setForm(row: { isAddRow?: boolean }, key: keyof FieldForm, value: unknown) {
  const form = row.isAddRow ? addForm : editForm
  ;(form as unknown as Record<string, unknown>)[key] = value
}

onMounted(async () => {
  await store.fetchDataset(datasetId)
  await store.fetchFields(datasetId)
})

function startAdd() { adding.value = true }
function cancelAdd() {
  adding.value = false
  Object.assign(addForm, { fieldName: '', fieldAlias: '', dataType: 'varchar', isDimension: false, isMetric: false, isFilterable: false, description: '' })
}

async function onAdd() {
  await store.createField(datasetId, { ...addForm })
  cancelAdd()
  ElMessage.success('字段已创建')
}

function startEdit(f: DatasetFieldResponse) {
  editingId.value = f.id
  Object.assign(editForm, {
    fieldName: f.fieldName, fieldAlias: f.fieldAlias || '', dataType: f.dataType,
    isDimension: f.isDimension, isMetric: f.isMetric, isFilterable: f.isFilterable,
    description: f.description || '',
  })
}

function cancelEdit() { editingId.value = null }

async function onUpdate(fieldId: number) {
  await store.updateFieldAction(datasetId, fieldId, { ...editForm })
  editingId.value = null
  ElMessage.success('字段已更新')
}

async function onDelete(f: DatasetFieldResponse) {
  await ElMessageBox.confirm(`确定删除字段"${f.fieldName}"？`, '删除确认', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  await store.deleteFieldAction(datasetId, f.id)
  ElMessage.success('已删除')
}

function onPageChange(p: number) { page.value = p - 1 }

function onSelectionChange(rows: (DatasetFieldResponse | { isAddRow?: boolean })[]) {
  selected.value = rows.filter((r) => !('isAddRow' in r)) as DatasetFieldResponse[]
}

async function batchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selected.value.length} 个字段吗？`, '批量删除', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  for (const f of [...selected.value]) {
    await store.deleteFieldAction(datasetId, f.id)
  }
  selected.value = []
  ElMessage.success('批量删除完成')
}
</script>

<style scoped>
.field-manager { padding: 0; }
</style>
