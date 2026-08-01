<template>
  <div class="conversation-view">
    <div class="conv-layout">
      <!-- Left: session list -->
      <ConversationSidebar class="conv-sidebar" />

      <!-- Right: current conversation -->
      <div class="conv-main">
        <template v-if="store.current">
          <div class="conv-header">
            <h3>{{ store.current.title }}</h3>
            <span class="conv-meta">
              数据集：{{ datasetName(store.current.datasetId) }} · {{ store.current.taskCount }} 轮 · 状态：{{ statusLabel(store.current.status) }}
            </span>
          </div>

          <!-- Turns (from linked analysis tasks) -->
          <div class="turns-list">
            <template v-if="store.current.turns.length">
              <div v-for="t in store.current.turns" :key="t.taskId" class="turn">
                <div class="turn-q">
                  <el-icon><User /></el-icon>
                  <span>{{ t.question }}</span>
                </div>
                <div class="turn-status">
                  <el-tag size="small" :type="statusType(t.status)" effect="light">
                    {{ statusLabel(t.status) }}
                  </el-tag>
                  <span class="turn-time">{{ fmtTime(t.createdAt) }}</span>
                </div>
              </div>
            </template>
            <el-empty v-else description="尚无分析轮次，输入问题开始对话" :image-size="60" />
          </div>

          <!-- Latest result -->
          <div v-if="store.currentResult" class="result-block">
            <AnalysisResult :result="store.currentResult" @followup="onFollowup" />
          </div>

          <!-- Input -->
          <div class="input-bar">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              :disabled="store.sending"
              placeholder="输入追问问题（Ctrl+Enter 发送）"
              @keyup.ctrl.enter="send"
            />
            <div class="input-actions">
              <el-button type="primary" :loading="store.sending" :disabled="!question.trim()" @click="send">
                发送
              </el-button>
            </div>
          </div>
        </template>

        <el-empty v-else description="选择左侧会话，或新建一个会话开始分析" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User } from '@element-plus/icons-vue'
import ConversationSidebar from '@/components/conversation/ConversationSidebar.vue'
import AnalysisResult from '@/components/AnalysisResult.vue'
import { useConversationStore } from '@/stores/conversation'
import { getDatasetList } from '@/api/datasets'
import type { DatasetResponse } from '@/api/datasets'
import type { AnalysisResponse } from '@/api/datasets'

const store = useConversationStore()
const question = ref('')
const datasets = ref<DatasetResponse[]>([])

async function send() {
  if (!question.value.trim()) return
  await store.send(question.value.trim())
  question.value = ''
}

/** Recommended follow-up clicked → bring into the input box. */
function onFollowup(q: string) {
  question.value = q
}

function datasetName(id: number | null) {
  if (!id) return '-'
  return datasets.value.find((d) => d.id === id)?.name ?? `#${id}`
}

function statusType(s: string): 'success' | 'danger' | 'warning' | 'info' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function statusLabel(s: string) {
  const m: Record<string, string> = { COMPLETED: '已成功', FAILED: '失败', RUNNING: '进行中', ACTIVE: '进行中', ARCHIVED: '已归档' }
  return m[s] || s
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(async () => {
  try {
    const resp = await getDatasetList(0, 100)
    datasets.value = resp.content
  } catch {
    datasets.value = []
  }
  await store.fetchList()
})
</script>

<style scoped>
.conversation-view {
  display: flex;
  height: calc(100vh - 84px);
}

.conv-layout { display: flex; flex: 1; min-height: 0; }

.conv-sidebar { width: 240px; flex-shrink: 0; }

.conv-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  min-width: 0;
  gap: 12px;
}

.conv-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.conv-meta { font-size: 13px; color: #909399; }

.turns-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  min-height: 120px;
}

.turn {
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.turn-q {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 14px;
  color: #303133;
}

.turn-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.turn-time { font-size: 12px; color: #c0c4cc; }

.result-block { flex-shrink: 0; }

.input-bar { flex-shrink: 0; }

.input-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>
