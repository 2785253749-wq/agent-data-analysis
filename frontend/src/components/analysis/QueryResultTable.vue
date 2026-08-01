<template>
  <div class="query-result">
    <div class="query-meta">
      <el-text size="small">
        共 {{ queryResult.rowCount }} 行 · 耗时 {{ queryResult.executionTimeMs }}ms
      </el-text>
      <el-tag v-if="queryResult.truncated" type="warning" size="small" class="truncate-tag">
        结果已截断
      </el-tag>
      <el-text v-if="queryResult.summary" type="info" size="small" class="summary-text">
        {{ queryResult.summary }}
      </el-text>
    </div>
    <el-table
      :data="queryResult.rows"
      stripe
      :max-height="420"
      size="small"
      class="result-table"
    >
      <el-table-column
        v-for="col in queryResult.columns"
        :key="col"
        :prop="col"
        :label="col"
        show-overflow-tooltip
        min-width="100"
      />
      <template #empty>无数据</template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { AnalysisResponse } from '@/api/datasets'

defineProps<{
  queryResult: NonNullable<AnalysisResponse['queryResult']>
}>()
</script>

<style scoped>
.query-result { padding: 4px 0; }

.query-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.truncate-tag { flex-shrink: 0; }

.summary-text { flex: 1; min-width: 120px; }

.result-table { width: 100%; }
</style>
