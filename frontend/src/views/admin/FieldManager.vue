<template>
  <div class="field-manager">
    <div class="toolbar">
      <h2>字段管理 — {{ store.currentDataset?.name }}</h2>
      <button class="btn btn-primary" @click="startAdd" v-if="!adding">+ 新增字段</button>
    </div>

    <div v-if="store.fieldsLoading" class="loading">加载中...</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>字段名</th>
          <th>别名</th>
          <th>数据类型</th>
          <th>维度</th>
          <th>指标</th>
          <th>可过滤</th>
          <th>描述</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <!-- Add new row -->
        <tr v-if="adding">
          <td><input v-model="addForm.fieldName" type="text" class="cell-input" placeholder="字段名" /></td>
          <td><input v-model="addForm.fieldAlias" type="text" class="cell-input" placeholder="别名" /></td>
          <td>
            <select v-model="addForm.dataType" class="cell-select">
              <option v-for="dt in dataTypes" :key="dt" :value="dt">{{ dt }}</option>
            </select>
          </td>
          <td><input v-model="addForm.isDimension" type="checkbox" /></td>
          <td><input v-model="addForm.isMetric" type="checkbox" /></td>
          <td><input v-model="addForm.isFilterable" type="checkbox" /></td>
          <td><input v-model="addForm.description" type="text" class="cell-input" placeholder="说明" /></td>
          <td>
            <button class="btn-sm btn-save" @click="onAdd">保存</button>
            <button class="btn-sm" @click="cancelAdd">取消</button>
          </td>
        </tr>
        <!-- Edit rows -->
        <tr v-for="f in store.fields" :key="f.id">
          <template v-if="editingId === f.id">
            <td><input v-model="editForm.fieldName" type="text" class="cell-input" /></td>
            <td><input v-model="editForm.fieldAlias" type="text" class="cell-input" /></td>
            <td>
              <select v-model="editForm.dataType" class="cell-select">
                <option v-for="dt in dataTypes" :key="dt" :value="dt">{{ dt }}</option>
              </select>
            </td>
            <td><input v-model="editForm.isDimension" type="checkbox" /></td>
            <td><input v-model="editForm.isMetric" type="checkbox" /></td>
            <td><input v-model="editForm.isFilterable" type="checkbox" /></td>
            <td><input v-model="editForm.description" type="text" class="cell-input" /></td>
            <td>
              <button class="btn-sm btn-save" @click="onUpdate(f.id)">保存</button>
              <button class="btn-sm" @click="cancelEdit">取消</button>
            </td>
          </template>
          <template v-else>
            <td><code>{{ f.fieldName }}</code></td>
            <td>{{ f.fieldAlias || '-' }}</td>
            <td><span class="type-badge">{{ f.dataType }}</span></td>
            <td>{{ f.isDimension ? '✅' : '-' }}</td>
            <td>{{ f.isMetric ? '✅' : '-' }}</td>
            <td>{{ f.isFilterable ? '✅' : '-' }}</td>
            <td class="desc-cell">{{ f.description || '-' }}</td>
            <td>
              <button class="btn-sm" @click="startEdit(f)">编辑</button>
              <button class="btn-sm btn-danger" @click="onDelete(f)">删除</button>
            </td>
          </template>
        </tr>
        <tr v-if="store.fields.length === 0 && !adding">
          <td colspan="8" class="empty">暂无字段定义</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import type { DatasetFieldResponse } from '@/api/datasets'

const route = useRoute()
const store = useAdminStore()
const datasetId = Number(route.params.id)

const dataTypes = ['varchar', 'int', 'bigint', 'decimal', 'datetime', 'date', 'text', 'boolean']

const adding = ref(false)
const editingId = ref<number | null>(null)

const addForm = reactive({
  fieldName: '', fieldAlias: '', dataType: 'varchar',
  isDimension: false, isMetric: false, isFilterable: false, description: '',
})

const editForm = reactive({
  fieldName: '', fieldAlias: '', dataType: 'varchar',
  isDimension: false, isMetric: false, isFilterable: false, description: '',
})

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
}

async function onDelete(f: DatasetFieldResponse) {
  if (confirm(`确定删除字段"${f.fieldName}"？`)) {
    await store.deleteFieldAction(datasetId, f.id)
  }
}
</script>

<style scoped>
.field-manager { max-width: 1300px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar h2 { font-size: 20px; }

.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.06); }
.data-table th { background: #f5f7fa; padding: 10px 12px; text-align: left; font-size: 13px; color: #666; font-weight: 600; white-space: nowrap; }
.data-table td { padding: 8px 12px; font-size: 13px; border-top: 1px solid #f0f0f0; }
.data-table code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-size: 12px; }

.type-badge { background: #e8edf5; color: #1a1a2e; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }

.desc-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.cell-input { width: 100%; padding: 4px 6px; border: 1px solid #d0d0d0; border-radius: 3px; font-size: 13px; }
.cell-select { padding: 4px 6px; border: 1px solid #d0d0d0; border-radius: 3px; font-size: 13px; }

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
