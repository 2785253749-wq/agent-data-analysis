<template>
  <div class="analysis-result">
    <!-- Steps Timeline -->
    <div class="steps-bar">
      <span
        v-for="step in steps" :key="step.stepType"
        :class="['step', step.status.toLowerCase()]"
        :title="step.stepType + ': ' + step.status + ' (' + (step.durationMs || 0) + 'ms)'"
      >
        {{ stepLabels[step.stepType] || step.stepType }}
      </span>
    </div>

    <!-- Clarification Needed -->
    <div v-if="result.intent?.needsClarification" class="clarify-box">
      <h4>需要确认以下信息：</h4>
      <ul>
        <li v-for="q in result.intent.clarificationQuestions" :key="q">{{ q }}</li>
      </ul>
    </div>

    <!-- Chart -->
    <ChartRenderer v-if="result.chartSpec" :spec="result.chartSpec" />

    <!-- Interpretation -->
    <div v-if="result.interpretation" class="interpretation">
      <h3>分析结论</h3>
      <p class="conclusion">{{ result.interpretation.conclusion }}</p>
      <div v-if="result.interpretation.points?.length" class="points">
        <div
          v-for="(p, i) in result.interpretation.points"
          :key="i"
          :class="['point', p.type]"
        >
          <span class="point-type">{{ typeLabel(p.type) }}</span>
          <span class="point-text">{{ p.statement }}</span>
          <span v-if="p.evidence" class="point-evidence">证据：{{ p.evidence }}</span>
        </div>
      </div>
      <div v-if="result.interpretation.caveats?.length" class="caveats">
        <strong>注意事项：</strong>
        <ul>
          <li v-for="c in result.interpretation.caveats" :key="c">{{ c }}</li>
        </ul>
      </div>
    </div>

    <!-- SQL Display -->
    <details v-if="result.sqlResult?.sql" class="sql-detail">
      <summary>查看生成的 SQL</summary>
      <pre><code>{{ result.sqlResult.sql }}</code></pre>
      <p v-if="result.sqlResult.explanation" class="sql-explain">
        {{ result.sqlResult.explanation }}
      </p>
    </details>

    <!-- Error -->
    <div v-if="result.errorMessage" class="error-box">
      分析失败：{{ result.errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ChartRenderer from './ChartRenderer.vue'
import type { AnalysisResponse } from '@/api/datasets'

const props = defineProps<{ result: AnalysisResponse }>()

const stepLabels: Record<string, string> = {
  INTENT: '意图识别',
  SQL_GEN: 'SQL生成',
  SQL_VALIDATE: '安全校验',
  QUERY: '查询执行',
  INTERPRET: '数据解释',
  CHART: '图表生成',
}

const steps = computed(() => props.result.steps || [])

function typeLabel(type: string) {
  const map: Record<string, string> = { fact: '事实', inference: '推断', suggestion: '建议' }
  return map[type] || type
}
</script>

<style scoped>
.analysis-result > * { margin-bottom: 16px; }

.steps-bar {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.step {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  background: #f0f0f0;
  color: #888;
}

.step.completed { background: #e6f7e6; color: #2e7d32; }
.step.failed { background: #fdecea; color: #c62828; }

.clarify-box {
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: 8px;
  padding: 16px;
}

.clarify-box h4 { margin-bottom: 8px; }
.clarify-box li { margin-left: 20px; font-size: 14px; }

.interpretation {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}

.interpretation h3 { font-size: 16px; margin-bottom: 12px; }

.conclusion {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
}

.point {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.point-type {
  padding: 1px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.point.fact .point-type { background: #e3f2fd; color: #1565c0; }
.point.inference .point-type { background: #f3e5f5; color: #7b1fa2; }
.point.suggestion .point-type { background: #fff3e0; color: #e65100; }

.point-text { flex: 1; }

.point-evidence {
  width: 100%;
  font-size: 12px;
  color: #888;
}

.caveats {
  margin-top: 12px;
  font-size: 13px;
  color: #666;
}

.caveats ul { margin-left: 20px; }

.sql-detail {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 16px;
}

.sql-detail summary {
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.sql-detail pre {
  margin-top: 8px;
  background: #1a1a2e;
  color: #a0d0a0;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
}

.sql-explain {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
}

.error-box {
  background: #fdecea;
  color: #c62828;
  padding: 16px;
  border-radius: 8px;
}
</style>
