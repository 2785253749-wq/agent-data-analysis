<template>
  <div class="interpretation-block">
    <div class="conclusion-row">
      <el-text size="large" tag="p" class="conclusion">
        {{ interpretation.conclusion }}
      </el-text>
      <el-tag v-if="interpretation.confidence" size="small" :type="confidenceType" effect="light">
        置信度：{{ confidenceLabel }}
      </el-tag>
    </div>

    <div v-if="interpretation.points?.length" class="points">
      <div
        v-for="(p, i) in interpretation.points"
        :key="i"
        class="point"
      >
        <el-tag size="small" :type="pointType(p.type)" effect="light" class="point-type">
          {{ pointLabel(p.type) }}
        </el-tag>
        <span class="point-text">{{ p.statement }}</span>
        <span v-if="p.evidence" class="point-evidence">证据：{{ p.evidence }}</span>
      </div>
    </div>

    <div v-if="interpretation.caveats?.length" class="caveats">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="注意事项"
      >
        <ul class="caveats-list">
          <li v-for="c in interpretation.caveats" :key="c">{{ c }}</li>
        </ul>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AnalysisResponse } from '@/api/datasets'

const props = defineProps<{
  interpretation: NonNullable<AnalysisResponse['interpretation']>
}>()

const confidenceType = computed(() => {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    high: 'success',
    medium: 'warning',
    low: 'info',
  }
  return map[props.interpretation.confidence] || 'info'
})

const confidenceLabel = computed(() => {
  const map: Record<string, string> = { high: '高', medium: '中', low: '低' }
  return map[props.interpretation.confidence] || props.interpretation.confidence
})

function pointType(type: string): 'primary' | 'success' | 'warning' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
    fact: 'primary',
    inference: 'warning',
    suggestion: 'success',
  }
  return map[type] || 'info'
}

function pointLabel(type: string) {
  const map: Record<string, string> = { fact: '事实', inference: '推断', suggestion: '建议' }
  return map[type] || type
}
</script>

<style scoped>
.interpretation-block { padding: 4px 0; }

.conclusion-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.conclusion {
  flex: 1;
  font-weight: 600;
  color: #303133;
  line-height: 1.6;
}

.points { margin-bottom: 12px; }

.point {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.point-type { flex-shrink: 0; }

.point-text { flex: 1; }

.point-evidence {
  width: 100%;
  font-size: 12px;
  color: #909399;
}

.caveats-list {
  margin-left: 18px;
  margin-top: 4px;
}

.caveats-list li {
  margin: 4px 0;
  font-size: 13px;
}
</style>
