<template>
  <div class="chat-view">
    <!-- Input Area -->
    <div class="input-section">
      <textarea
        v-model="question"
        class="question-input"
        placeholder="输入数据分析问题，例如：今年各地区的销售额汇总？"
        rows="2"
        :disabled="analysis.loading.value"
        @keyup.ctrl.enter="submit"
      ></textarea>
      <div class="input-actions">
        <select v-model="datasetId" class="dataset-select">
          <option :value="null">（无数据集）</option>
          <option v-for="ds in datasets" :key="ds.id" :value="ds.id">
            {{ ds.name }}
          </option>
        </select>
        <button
          class="btn btn-primary"
          :disabled="analysis.loading.value || !question.trim()"
          @click="submit"
        >
          {{ analysis.loading.value ? '分析中...' : '开始分析' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="analysis.loading.value" class="loading">
      <div class="spinner"></div>
      <p>正在分析... 这可能需要几秒钟</p>
    </div>

    <!-- Error -->
    <div v-if="analysis.error.value" class="error-box">
      {{ analysis.error.value }}
      <button class="btn-sm" @click="analysis.reset()">重试</button>
    </div>

    <!-- Result -->
    <div v-if="analysis.result.value" class="result-section">
      <AnalysisResult :result="analysis.result.value" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
    datasets.value = resp.content.filter(d => d.isEnabled)
  } catch { /* admin may not have datasets yet */ }
})

function submit() {
  if (!question.value.trim()) return
  analysis.analyze(question.value.trim(), datasetId.value)
}
</script>

<style scoped>
.chat-view { max-width: 900px; margin: 0 auto; }

.input-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  margin-bottom: 20px;
}

.question-input {
  width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  font-size: 15px;
  resize: vertical;
  font-family: inherit;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.dataset-select {
  padding: 8px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
}

.btn {
  padding: 10px 28px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1a1a2e; color: #fff;
}

.btn-primary:disabled {
  opacity: 0.5; cursor: default;
}

.btn-primary:hover:not(:disabled) { background: #2a2a4e; }

.btn-sm {
  padding: 4px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  margin-left: 12px;
}

.loading {
  text-align: center;
  padding: 60px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top-color: #1a1a2e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-box {
  background: #fdecea;
  color: #c62828;
  padding: 16px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-section {
  margin-top: 20px;
}
</style>
