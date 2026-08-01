<template>
  <div class="sql-block">
    <div class="sql-head">
      <span class="sql-label">生成的 SQL</span>
      <el-button size="small" :icon="CopyDocument" @click="copy">复制</el-button>
    </div>
    <pre class="sql-pre"><code>{{ sqlResult.sql }}</code></pre>
    <p v-if="sqlResult.explanation" class="sql-explain">{{ sqlResult.explanation }}</p>
    <div v-if="sqlResult.usedTables?.length || sqlResult.usedFields?.length" class="sql-meta">
      <span v-if="sqlResult.usedTables?.length" class="meta-item">
        表：
        <el-tag v-for="t in sqlResult.usedTables" :key="t" size="small" effect="plain" class="meta-tag">
          {{ t }}
        </el-tag>
      </span>
      <span v-if="sqlResult.usedFields?.length" class="meta-item">
        字段：
        <el-tag v-for="f in sqlResult.usedFields" :key="f" size="small" effect="plain" class="meta-tag">
          {{ f }}
        </el-tag>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import type { AnalysisResponse } from '@/api/datasets'

const props = defineProps<{
  sqlResult: NonNullable<AnalysisResponse['sqlResult']>
}>()

async function copy() {
  try {
    await navigator.clipboard.writeText(props.sqlResult.sql)
    ElMessage.success('已复制 SQL')
  } catch {
    ElMessage.warning('复制失败')
  }
}
</script>

<style scoped>
.sql-block { padding: 4px 0; }

.sql-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sql-label { font-size: 13px; color: #606266; font-weight: 600; }

.sql-pre {
  background: #1a1a2e;
  color: #a0d0a0;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  font-family: 'Courier New', monospace;
  line-height: 1.5;
}

.sql-explain {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

.sql-meta {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item { font-size: 13px; color: #909399; }
.meta-tag { margin-left: 4px; }
</style>
