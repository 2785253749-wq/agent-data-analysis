<template>
  <div class="report-page">
    <!-- Top: form + history -->
    <div class="report-top">
      <DataTableCard title="生成报告">
        <div class="report-form">
          <el-form label-width="80px">
            <el-form-item label="报告标题" required>
              <el-input v-model="title" maxlength="100" placeholder="请输入报告标题" />
            </el-form-item>
            <el-form-item label="数据说明">
              <el-input
                v-model="notes"
                type="textarea"
                :rows="3"
                placeholder="报告补充说明（可选）"
              />
            </el-form-item>
            <el-form-item label="分析来源">
              <el-select v-model="sourceId" placeholder="选择分析结果" style="width: 100%">
                <el-option v-if="latest" :label="latest.question.slice(0, 30) + '（最近一次分析）'" :value="'latest'" />
              </el-select>
            </el-form-item>
            <el-alert
              v-if="!latest && !reports.length"
              type="info"
              :closable="false"
              show-icon
              title="请先在 AI 数据分析页完成一次分析"
            />
            <el-form-item>
              <el-button
                type="primary"
                :disabled="!title.trim() || !source"
                @click="generate"
              >
                生成报告
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </DataTableCard>

      <DataTableCard title="报告历史">
        <el-table :data="reports" stripe>
          <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
          <el-table-column label="创建时间" width="110">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="loadDoc(row as ReportDoc)">查看</el-button>
              <el-button link type="danger" @click="removeDoc(row.id)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>暂无报告</template>
        </el-table>
      </DataTableCard>
    </div>

    <!-- Below: report body -->
    <DataTableCard v-if="currentDoc" title="报告正文">
      <div class="report-body">
        <h2 class="report-title">{{ currentDoc.title }}</h2>
        <p class="report-meta">
          生成时间：{{ formatDateTime(currentDoc.createdAt) }} · 分析问题：{{ currentDoc.question }}
        </p>
        <p v-if="currentDoc.notes" class="report-notes">{{ currentDoc.notes }}</p>
        <el-divider />

        <div class="report-section">
          <h3 class="section-title">SQL</h3>
          <SqlBlock v-if="currentDoc.result.sqlResult?.sql" :sql-result="currentDoc.result.sqlResult" />
          <el-text v-else type="info">无</el-text>
        </div>

        <el-divider />

        <div class="report-section">
          <h3 class="section-title">查询结果</h3>
          <QueryResultTable v-if="currentDoc.result.queryResult" :query-result="currentDoc.result.queryResult" />
          <el-text v-else type="info">无</el-text>
        </div>

        <el-divider />

        <div class="report-section">
          <h3 class="section-title">推荐图表</h3>
          <ChartRenderer v-if="currentDoc.result.chartSpec" :spec="currentDoc.result.chartSpec" />
          <el-text v-else type="info">无</el-text>
        </div>

        <el-divider />

        <div class="report-section">
          <h3 class="section-title">数据解读</h3>
          <InterpretationBlock v-if="currentDoc.result.interpretation" :interpretation="currentDoc.result.interpretation" />
          <el-text v-else type="info">无</el-text>
        </div>
      </div>
    </DataTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTableCard from '@/components/common/DataTableCard.vue'
import SqlBlock from '@/components/analysis/SqlBlock.vue'
import QueryResultTable from '@/components/analysis/QueryResultTable.vue'
import InterpretationBlock from '@/components/analysis/InterpretationBlock.vue'
import ChartRenderer from '@/components/ChartRenderer.vue'
import {
  getReports, saveReport, deleteReport, loadLatestAnalysis, makeId,
} from '@/utils/reportStorage'
import type { ReportDoc } from '@/utils/reportStorage'

const title = ref('')
const notes = ref('')
const sourceId = ref<string>('latest')
const reports = ref<ReportDoc[]>([])
const currentDoc = ref<ReportDoc | null>(null)

const latest = computed(() => loadLatestAnalysis())

const source = computed(() => {
  if (sourceId.value === 'latest' && latest.value) return { question: latest.value.question, result: latest.value }
  return null
})

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('zh-CN')
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

function generate() {
  if (!source.value) return
  const doc: ReportDoc = {
    id: makeId(),
    title: title.value.trim(),
    notes: notes.value.trim(),
    question: source.value.question,
    createdAt: new Date().toISOString(),
    result: source.value.result,
  }
  reports.value = saveReport(doc)
  currentDoc.value = doc
  ElMessage.success('报告已生成')
}

function loadDoc(doc: ReportDoc) {
  currentDoc.value = doc
}

async function removeDoc(id: string) {
  await ElMessageBox.confirm('确定删除这份报告吗？', '删除确认', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  })
  reports.value = deleteReport(id)
  if (currentDoc.value?.id === id) currentDoc.value = null
  ElMessage.success('已删除')
}

onMounted(() => {
  reports.value = getReports()
})
</script>

<style scoped>
.report-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-top {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .report-top { grid-template-columns: 1fr; }
}

.report-form { padding: 8px 16px; }

.report-body { padding: 8px 24px 24px; }

.report-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.report-meta {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.report-notes {
  font-size: 14px;
  color: #606266;
  background: #f8f9fb;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
</style>
