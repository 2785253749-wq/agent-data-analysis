<template>
  <div class="analysis-view">
    <!-- Input card -->
    <el-card shadow="never" class="input-card">
      <template #header>
        <span class="card-title">开始分析</span>
      </template>
      <el-input
        v-model="question"
        type="textarea"
        :rows="3"
        :disabled="analysis.loading.value"
        placeholder="输入数据分析问题，例如：今年各地区的销售额汇总？"
        class="question-input"
        @keyup.ctrl.enter="submit"
      />
      <div class="input-actions">
        <el-select
          v-model="datasetId"
          clearable
          placeholder="选择数据集"
          style="width: 240px"
        >
          <el-option v-for="ds in datasets" :key="ds.id" :label="ds.name" :value="ds.id" />
        </el-select>
        <el-button
          type="primary"
          :loading="analysis.loading.value"
          :disabled="!question.trim()"
          @click="submit"
        >
          {{ analysis.loading.value ? '分析中...' : '开始分析' }}
        </el-button>
      </div>
    </el-card>

    <!-- Loading -->
    <el-card v-if="analysis.loading.value" shadow="never" class="loading-card">
      <el-skeleton :rows="3" animated />
      <p class="loading-text">正在分析... 这可能需要几秒钟</p>
    </el-card>

    <!-- Error -->
    <el-alert
      v-if="analysis.error.value"
      type="error"
      :closable="false"
      show-icon
      class="error-alert"
    >
      <template #title>
        {{ analysis.error.value }}
        <el-button size="small" @click="analysis.reset()">重试</el-button>
      </template>
    </el-alert>

    <!-- Result -->
    <div v-if="analysis.result.value" class="result-section">
      <AnalysisResult :result="analysis.result.value" />
    </div>

    <!-- Empty state: centered, no large blank area -->
    <el-card
      v-else-if="!analysis.loading.value && !analysis.error.value"
      shadow="never"
      class="empty-card"
    >
      <el-empty description="输入问题并点击「开始分析」，结果将展示在这里">
        <template #image>
          <el-icon :size="72" color="#c0c4cc"><DataAnalysis /></el-icon>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import { useAnalysis } from '@/composables/useAnalysis'
import { getDatasetList } from '@/api/datasets'
import type { DatasetResponse } from '@/api/datasets'
import AnalysisResult from '@/components/AnalysisResult.vue'

const analysis = useAnalysis()
const question = ref('')
const datasetId = ref<number | null>(null)
const datasets = ref<DatasetResponse[]>([])

onMounted(async () => {
  try {
    const resp = await getDatasetList(0, 100)
    datasets.value = resp.content.filter((d) => d.isEnabled)
  } catch {
    /* admin may not have datasets yet */
  }
})

function submit() {
  if (!question.value.trim()) return
  analysis.analyze(question.value.trim(), datasetId.value)
}
</script>

<style scoped>
.analysis-view { display: flex; flex-direction: column; gap: 16px; }

.input-card {
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.card-title { font-weight: 600; }

.question-input { margin-bottom: 16px; }

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.loading-card { text-align: center; }

.loading-text { margin-top: 12px; color: #909399; }

.error-alert { margin-bottom: 16px; }

.empty-card {
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.empty-card :deep(.el-empty) {
  padding: 48px 0;
}
</style>
