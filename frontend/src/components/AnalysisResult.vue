<template>
  <div class="analysis-result">
    <el-alert
      v-if="result.errorMessage"
      type="error"
      :closable="false"
      :title="result.errorMessage"
      show-icon
      class="block-gap"
    />

    <el-alert
      v-if="result.intent?.needsClarification"
      type="warning"
      :closable="false"
      show-icon
      :title="'需要确认：' + (result.intent?.clarificationQuestions || []).join('；')"
      class="block-gap"
    />

    <!-- Agent execution steps -->
    <div class="steps-bar">
      <span
        v-for="step in result.steps"
        :key="step.stepType"
        :class="['step', step.status.toLowerCase()]"
        :title="step.stepType + ': ' + step.status + ' (' + (step.durationMs || 0) + 'ms)'"
      >
        {{ stepLabels[step.stepType] || step.stepType }}
        <span v-if="step.durationMs" class="step-ms">{{ step.durationMs }}ms</span>
      </span>
    </div>

    <div class="result-actions">
      <el-button type="primary" plain :icon="Document" @click="saveToReport">
        保存到报告
      </el-button>
    </div>

    <el-card v-if="hasAnyResult" shadow="never" class="result-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane v-if="result.sqlResult?.sql" label="SQL" name="sql">
          <SqlBlock :sql-result="result.sqlResult" />
        </el-tab-pane>
        <el-tab-pane v-if="result.queryResult" label="查询结果" name="query">
          <QueryResultTable :query-result="result.queryResult" />
        </el-tab-pane>
        <el-tab-pane v-if="result.chartSpec" label="推荐图表" name="chart">
          <ChartRenderer :spec="result.chartSpec" />
        </el-tab-pane>
        <el-tab-pane v-if="result.interpretation" label="数据解读" name="interpret">
          <InterpretationBlock :interpretation="result.interpretation" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import ChartRenderer from './ChartRenderer.vue'
import SqlBlock from './analysis/SqlBlock.vue'
import QueryResultTable from './analysis/QueryResultTable.vue'
import InterpretationBlock from './analysis/InterpretationBlock.vue'
import { saveReport, makeId } from '@/utils/reportStorage'
import type { AnalysisResponse } from '@/api/datasets'

const props = defineProps<{ result: AnalysisResponse }>()

const activeTab = ref('sql')

const stepLabels: Record<string, string> = {
  INTENT: '意图识别',
  SQL_GEN: 'SQL生成',
  SQL_VALIDATE: '安全校验',
  QUERY: '查询执行',
  INTERPRET: '数据解释',
  CHART: '图表生成',
}

const hasAnyResult = computed(
  () => !!(props.result.sqlResult?.sql || props.result.queryResult ||
    props.result.chartSpec || props.result.interpretation),
)

function saveToReport() {
  saveReport({
    id: makeId(),
    title: props.result.question.slice(0, 30),
    notes: '',
    question: props.result.question,
    createdAt: new Date().toISOString(),
    result: props.result,
  })
  ElMessage.success('已保存到报告历史')
}
</script>

<style scoped>
.analysis-result { display: flex; flex-direction: column; }

.block-gap { margin-bottom: 16px; }

.steps-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.step {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #f0f2f5;
  color: #606266;
}

.step-ms { opacity: 0.7; margin-left: 4px; }

.step.completed { background: #e1f3d8; color: #529b2e; }
.step.failed { background: #fef0f0; color: #f56c6c; }
.step.running { background: #ecf5ff; color: #409eff; }

.result-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.result-card {
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}
</style>
