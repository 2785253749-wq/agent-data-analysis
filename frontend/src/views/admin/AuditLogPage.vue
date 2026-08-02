<template>
  <div class="audit-log-page">
    <FilterToolbar title="操作日志">
      <template #left-extra>
        <el-input v-model="store.operator" placeholder="操作者" clearable style="width: 140px" @keyup.enter="store.applyFilters" />
        <el-select v-model="store.action" placeholder="动作" clearable style="width: 180px" @change="store.applyFilters">
          <el-option v-for="a in actionOptions" :key="a" :label="actionLabel(a)" :value="a" />
        </el-select>
        <el-date-picker
          v-model="range" type="datetimerange" range-separator="至" start-placeholder="开始时间"
          end-placeholder="结束时间" style="width: 360px" @change="onRangeChange"
        />
        <el-button type="primary" @click="store.applyFilters">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </template>
    </FilterToolbar>

    <DataTableCard title="审计日志（只追加，不可修改）">
      <el-table :data="store.logs" v-loading="store.loading" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作者" width="110" />
        <el-table-column label="动作" width="160">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column label="资源类型" width="110">
          <template #default="{ row }">{{ row.resourceType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resourceId" label="资源ID" width="90">
          <template #default="{ row }">{{ row.resourceId ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="来源IP" width="130" />
        <el-table-column label="详情" min-width="200">
          <template #default="{ row }">
            <el-text size="small" type="info" class="detail-text">{{ row.detail || '-' }}</el-text>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-pagination
          background layout="total, prev, pager, next"
          :total="store.total" :current-page="store.page + 1" :page-size="store.size"
          @current-change="(p: number) => { store.page = p - 1; store.fetchLogs() }"
        />
      </template>
    </DataTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import FilterToolbar from '@/components/common/FilterToolbar.vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useAuditLogStore } from '@/stores/auditLog'

const store = useAuditLogStore()
const range = ref<[string, string] | null>(null)

const actionOptions = [
  'LOGIN', 'LOGOUT',
  'DATASET_CREATE', 'DATASET_UPDATE', 'DATASET_DELETE',
  'FIELD_CREATE', 'FIELD_UPDATE', 'FIELD_DELETE',
  'METRIC_CREATE', 'METRIC_UPDATE', 'METRIC_DELETE',
  'MODEL_CREATE', 'MODEL_UPDATE', 'MODEL_DELETE', 'MODEL_SET_DEFAULT',
  'PROMPT_CREATE', 'PROMPT_ENABLE', 'PROMPT_DISABLE', 'PROMPT_ARCHIVE',
  'ANALYSIS_SUBMIT', 'ANALYSIS_COMPLETED', 'ANALYSIS_FAILED',
]

const actionLabels: Record<string, string> = {
  LOGIN: '登录', LOGOUT: '退出',
  DATASET_CREATE: '新建数据集', DATASET_UPDATE: '更新数据集', DATASET_DELETE: '删除数据集',
  FIELD_CREATE: '新建字段', FIELD_UPDATE: '更新字段', FIELD_DELETE: '删除字段',
  METRIC_CREATE: '新建指标', METRIC_UPDATE: '更新指标', METRIC_DELETE: '删除指标',
  MODEL_CREATE: '新建模型', MODEL_UPDATE: '更新模型', MODEL_DELETE: '删除模型', MODEL_SET_DEFAULT: '设置默认模型',
  PROMPT_CREATE: '新建Prompt', PROMPT_ENABLE: '启用Prompt', PROMPT_DISABLE: '停用Prompt', PROMPT_ARCHIVE: '归档Prompt',
  ANALYSIS_SUBMIT: '分析提交', ANALYSIS_COMPLETED: '分析成功', ANALYSIS_FAILED: '分析失败',
}

function actionLabel(a: string) {
  return actionLabels[a] || a
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

function onRangeChange(val: [string, string] | null) {
  if (val) {
    store.start = val[0]
    store.end = val[1]
  } else {
    store.start = ''
    store.end = ''
  }
  store.applyFilters()
}

function onReset() {
  range.value = null
  store.reset()
}

onMounted(() => store.fetchLogs())
</script>

<style scoped>
.detail-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  word-break: break-all;
}
</style>
