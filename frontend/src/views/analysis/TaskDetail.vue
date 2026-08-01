<template>
  <div class="task-detail">
    <!-- Header -->
    <div class="detail-header">
      <div>
        <p class="question">{{ task.question }}</p>
        <p class="meta">
          数据集：{{ task.datasetName || '-' }} ·
          状态：<el-tag :type="statusType(task.status)" effect="light" size="small">{{ statusLabel(task.status) }}</el-tag>
          · 耗时：{{ fmtDuration(task.durationMs) }}
        </p>
      </div>
    </div>

    <el-alert
      v-if="task.errorMessage"
      type="warning"
      :closable="false"
      show-icon
      :title="'失败原因（已脱敏）：' + task.errorMessage"
      class="error-alert"
    />

    <!-- Steps timeline -->
    <div class="section">
      <h4>执行步骤</h4>
      <el-timeline v-if="task.steps?.length">
        <el-timeline-item
          v-for="(step, i) in task.steps"
          :key="i"
          :type="stepType(step.status)"
          :hollow="step.status !== 'COMPLETED'"
        >
          <div class="step-row">
            <span class="step-name">{{ stepLabel(step.stepType) }}</span>
            <el-tag size="small" :type="stepType(step.status)" effect="plain">
              {{ statusLabel(step.status) }}
            </el-tag>
            <span class="step-dur">{{ fmtDuration(step.durationMs) }}</span>
          </div>
          <p v-if="step.errorMessage" class="step-error">{{ step.errorMessage }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无步骤记录" :image-size="60" />
    </div>

    <!-- Results tabs -->
    <div class="section">
      <h4>分析结果</h4>
      <el-tabs v-model="activeTab">
        <el-tab-pane v-if="task.sqlText" label="SQL" name="sql">
          <SqlBlock :sql-result="{ sql: task.sqlText, parameters: task.parameters || {}, usedTables: [], usedFields: [], explanation: '' }" />
        </el-tab-pane>
        <el-tab-pane v-if="task.queryResult" label="查询结果" name="query">
          <QueryResultTable :query-result="task.queryResult as any" />
        </el-tab-pane>
        <el-tab-pane v-if="task.chartSpec" label="推荐图表" name="chart">
          <ChartRenderer :spec="task.chartSpec as any" />
        </el-tab-pane>
        <el-tab-pane v-if="task.interpretation" label="数据解读" name="interpret">
          <InterpretationBlock :interpretation="task.interpretation as any" />
        </el-tab-pane>
        <template v-if="!task.sqlText && !task.queryResult && !task.chartSpec && !task.interpretation">
          <el-empty description="该任务没有可展示的分析结果" :image-size="60" />
        </template>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SqlBlock from '@/components/analysis/SqlBlock.vue'
import QueryResultTable from '@/components/analysis/QueryResultTable.vue'
import InterpretationBlock from '@/components/analysis/InterpretationBlock.vue'
import ChartRenderer from '@/components/ChartRenderer.vue'
import type { TaskDetail } from '@/api/analysis'

const props = defineProps<{ task: TaskDetail }>()
defineEmits<{ (e: 'refresh'): void }>()

const activeTab = ref('sql')

function statusType(s: string): 'success' | 'danger' | 'warning' | 'info' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function statusLabel(s: string) {
  const m: Record<string, string> = {
    COMPLETED: '已成功', FAILED: '失败', RUNNING: '进行中', PENDING: '等待中', SKIPPED: '跳过',
  }
  return m[s] || s
}

function stepType(s: string): 'success' | 'danger' | 'warning' | 'info' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function stepLabel(t: string) {
  const m: Record<string, string> = {
    INTENT: '意图识别', SQL_GEN: 'SQL生成', SQL_VALIDATE: '安全校验',
    QUERY: '查询执行', INTERPRET: '数据解读', CHART: '图表生成',
  }
  return m[t] || t
}

function fmtDuration(ms: number | null) {
  if (ms == null) return '-'
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
}
</script>

<style scoped>
.task-detail { display: flex; flex-direction: column; gap: 20px; }

.detail-header .question {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.detail-header .meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.error-alert { margin-top: 4px; }

.section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-name { font-size: 14px; font-weight: 500; }

.step-dur { font-size: 12px; color: #909399; }

.step-error {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}
</style>
