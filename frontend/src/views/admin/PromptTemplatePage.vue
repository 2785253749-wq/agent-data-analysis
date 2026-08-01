<template>
  <div class="prompt-page">
    <FilterToolbar title="Prompt 模板管理">
      <template #left-extra>
        <el-select v-model="store.typeFilter" placeholder="全部类型" clearable style="width: 160px" @change="onTypeChange">
          <el-option v-for="t in PROMPT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增版本</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="模板版本列表">
      <el-table :data="store.prompts" v-loading="store.loading" stripe>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="正文摘要" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ (row.content || '').slice(0, 60) }}</template>
        </el-table-column>
        <el-table-column prop="contentHash" label="ContentHash" width="90" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.isEnabled" type="success" size="small">启用中</el-tag>
            <el-tag v-else-if="row.isArchived" type="info" size="small">已归档</el-tag>
            <el-tag v-else type="warning" size="small">未启用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isEnabled && !row.isArchived" link type="primary" @click="onEnable(row as PromptTemplate)">启用</el-button>
            <el-button v-if="row.isEnabled" link type="warning" @click="onDisable(row as PromptTemplate)">停用</el-button>
            <el-button link type="primary" @click="openDescEdit(row as PromptTemplate)">改说明</el-button>
            <el-button v-if="!row.isArchived" link type="danger" @click="onArchive(row as PromptTemplate)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-pagination
          background layout="total, prev, pager, next"
          :total="store.total" :current-page="store.page + 1" :page-size="store.size"
          @current-change="(p: number) => { store.page = p - 1; store.fetchPrompts() }"
        />
      </template>
    </DataTableCard>

    <!-- Create new version dialog (immutable content) -->
    <el-dialog v-model="createVisible" title="新增模板版本" width="600px">
      <el-alert type="info" :closable="false" show-icon title="版本一旦创建，正文不可修改；修改正文需新建版本" class="create-alert" />
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="名称"><el-input v-model="createForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option v-for="t in PROMPT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="createForm.content" type="textarea" :rows="8" class="mono" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="createForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Description edit dialog -->
    <el-dialog v-model="descVisible" title="编辑说明" width="440px">
      <el-input v-model="descText" type="textarea" :rows="3" />
      <template #footer>
        <el-button @click="descVisible = false">取消</el-button>
        <el-button type="primary" @click="onDescSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { usePromptStore } from '@/stores/prompt'
import { PROMPT_TYPES } from '@/api/prompts'
import type { PromptTemplate } from '@/api/prompts'

const store = usePromptStore()
const createVisible = ref(false)
const descVisible = ref(false)
const descText = ref('')
const descTarget = ref<number | null>(null)

const createForm = reactive({
  name: '', type: 'INTENT_RECOGNITION', content: '', description: '',
})

function typeLabel(t: string) {
  return PROMPT_TYPES.find((x) => x.value === t)?.label || t
}

function onTypeChange() {
  store.setType(store.typeFilter)
}

function openCreate() {
  Object.assign(createForm, { name: '', type: store.typeFilter || 'INTENT_RECOGNITION', content: '', description: '' })
  createVisible.value = true
}

async function onCreate() {
  try {
    await store.create({ ...createForm })
    createVisible.value = false
    ElMessage.success('版本已创建（未自动启用）')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '创建失败')
  }
}

async function onEnable(row: PromptTemplate) {
  try {
    await store.enable(row.id)
    ElMessage.success('已启用（同类型其他版本自动停用）')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '启用失败')
  }
}

async function onDisable(row: PromptTemplate) {
  try {
    await store.disable(row.id)
    ElMessage.success('已停用')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '停用失败')
  }
}

function openDescEdit(row: PromptTemplate) {
  descTarget.value = row.id
  descText.value = row.description || ''
  descVisible.value = true
}

async function onDescSave() {
  if (descTarget.value) {
    await store.updateDescription(descTarget.value, descText.value)
    descVisible.value = false
    ElMessage.success('说明已更新')
  }
}

async function onArchive(row: PromptTemplate) {
  await ElMessageBox.confirm('归档后该版本不可启用，确定归档？', '归档确认', { type: 'warning' })
  await store.archive(row.id)
  ElMessage.success('已归档')
}

onMounted(() => store.fetchPrompts())
</script>

<style scoped>
.create-alert { margin-bottom: 16px; }
.mono :deep(textarea) { font-family: 'Courier New', monospace; }
</style>
