<template>
  <div class="ai-model-page">
    <FilterToolbar title="AI 模型配置">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增模型</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="模型列表">
      <el-table :data="store.models" v-loading="store.loading" stripe>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="provider" label="供应商" width="90" />
        <el-table-column prop="modelName" label="模型名" min-width="130" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="timeoutMs" label="超时(ms)" width="100" />
        <el-table-column label="密钥" width="100">
          <template #default="{ row }">
            <el-tag :type="row.apiKeyConfigured ? 'success' : 'warning'" effect="light" size="small">
              {{ row.apiKeyConfigured ? '已配置' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" effect="light" size="small">
              {{ row.isEnabled ? '启用' : '禁用' }}
            </el-tag>
            <el-tag v-if="row.isDefault" type="primary" effect="dark" size="small" class="default-tag">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row as AiModel)">编辑</el-button>
            <el-button link type="primary" :disabled="row.isDefault" @click="onSetDefault(row as AiModel)">设为默认</el-button>
            <el-button link type="danger" :disabled="row.isDefault" @click="onDelete(row as AiModel)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-pagination
          background layout="total, prev, pager, next"
          :total="store.total" :current-page="store.page + 1" :page-size="store.size"
          @current-change="(p: number) => { store.page = p - 1; store.fetchModels() }"
        />
      </template>
    </DataTableCard>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑模型' : '新增模型'" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.provider" /></el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
          <div class="hint">仅允许 HTTPS + 白名单域名（如 api.deepseek.com）</div>
        </el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="超时(ms)"><el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" /></el-form-item>
        <el-form-item label="温度"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
        <el-form-item label="最大Token"><el-input-number v-model="form.maxTokens" :min="1" :step="256" /></el-form-item>
        <el-form-item label="密钥引用">
          <el-select v-model="form.apiKeyRef" style="width: 100%">
            <el-option label="DEEPSEEK_API_KEY" value="DEEPSEEK_API_KEY" />
          </el-select>
          <div class="hint">仅允许白名单环境变量，绝不存储明文 Key</div>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.isEnabled" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
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
import { useAiModelStore } from '@/stores/aiModel'
import type { AiModel } from '@/api/adminModels'

const store = useAiModelStore()
const dialogVisible = ref(false)
const editing = ref<number | null>(null)

const form = reactive({
  name: '', provider: 'deepseek', baseUrl: 'https://api.deepseek.com/v1',
  modelName: 'deepseek-chat', timeoutMs: 60000, temperature: 0, maxTokens: 2048,
  apiKeyRef: 'DEEPSEEK_API_KEY', isEnabled: true, isDefault: false,
})

function openDialog(model?: AiModel) {
  editing.value = model?.id ?? null
  if (model) {
    Object.assign(form, {
      name: model.name, provider: model.provider, baseUrl: model.baseUrl,
      modelName: model.modelName, timeoutMs: model.timeoutMs, temperature: model.temperature,
      maxTokens: model.maxTokens, apiKeyRef: 'DEEPSEEK_API_KEY',
      isEnabled: model.isEnabled, isDefault: model.isDefault,
    })
  } else {
    Object.assign(form, {
      name: '', provider: 'deepseek', baseUrl: 'https://api.deepseek.com/v1',
      modelName: 'deepseek-chat', timeoutMs: 60000, temperature: 0, maxTokens: 2048,
      apiKeyRef: 'DEEPSEEK_API_KEY', isEnabled: true, isDefault: false,
    })
  }
  dialogVisible.value = true
}

async function onSave() {
  try {
    const req = { ...form }
    if (editing.value) await store.update(editing.value, req)
    else await store.create(req)
    dialogVisible.value = false
    ElMessage.success('已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  }
}

async function onSetDefault(model: AiModel) {
  await store.setDefault(model.id)
  ElMessage.success('已设为默认')
}

async function onDelete(model: AiModel) {
  await ElMessageBox.confirm(`确定删除模型"${model.name}"？`, '删除确认', { type: 'warning' })
  await store.remove(model.id)
  ElMessage.success('已删除')
}

onMounted(() => store.fetchModels())
</script>

<style scoped>
.hint { font-size: 12px; color: #909399; line-height: 1.5; }
.default-tag { margin-left: 6px; }
</style>
