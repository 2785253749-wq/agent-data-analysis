<template>
  <div class="dashboard-view">
    <!-- Empty state -->
    <el-empty v-if="store.isEmpty() && !store.loading" description="暂无数据，请先在 AI 数据分析页发起分析" :image-size="100" class="empty-state" />

    <template v-else>
      <!-- Stat cards -->
      <div class="stat-row">
        <div v-for="card in statCards" :key="card.label" class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </div>

      <!-- Trend chart + recent + failures -->
      <div class="dash-grid">
        <DataTableCard title="近 7 天分析趋势">
          <div class="chart-box">
            <v-chart v-if="trendOption" :option="trendOption" :autoresize="true" style="width: 100%; height: 300px" />
          </div>
        </DataTableCard>

        <DataTableCard title="常见失败原因">
          <el-table :data="store.summary?.commonFailures || []" stripe>
            <el-table-column label="失败分类" min-width="150">
              <template #default="{ row }">
                <el-tag type="danger" effect="light" size="small">{{ row.reason }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="次数" width="120">
              <template #default="{ row }">{{ row.count }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无失败记录" :image-size="60" />
            </template>
          </el-table>
        </DataTableCard>
      </div>

      <!-- Recent tasks -->
      <DataTableCard title="最近分析任务">
        <el-table :data="store.summary?.recentTasks || []" stripe>
          <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="datasetName" label="数据集" width="120">
            <template #default="{ row }">{{ row.datasetName || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'danger'" effect="light" size="small">
                {{ row.status === 'COMPLETED' ? '成功' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">{{ fmtDur(row.durationMs) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无分析任务" :image-size="60" />
          </template>
        </el-table>
      </DataTableCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { Folder, DataAnalysis, CircleCheck, TrendCharts } from '@element-plus/icons-vue'
import DataTableCard from '@/components/common/DataTableCard.vue'
import { useDashboardStore } from '@/stores/dashboard'

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent])

const store = useDashboardStore()

const statCards = computed(() => [
  { label: '数据集数量', value: store.summary?.datasetCount ?? 0, icon: Folder, bg: '#ecf5ff', color: '#409eff' },
  { label: '累计分析次数', value: store.summary?.analysisCount ?? 0, icon: DataAnalysis, bg: '#f0f9eb', color: '#67c23a' },
  {
    label: '成功率',
    value: store.summary?.successRate != null ? store.summary.successRate + '%' : '-',
    icon: CircleCheck, bg: '#fdf6ec', color: '#e6a23c',
  },
  {
    label: '近7天分析',
    value: (store.summary?.last7DaysTrend || []).reduce((s, p) => s + p.count, 0),
    icon: TrendCharts, bg: '#fef0f0', color: '#f56c6c',
  },
])

const trendOption = computed(() => {
  const trend = store.summary?.last7DaysTrend || []
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map((p) => p.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', data: trend.map((p) => p.count),
      itemStyle: { color: '#409eff' }, barMaxWidth: 32,
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  }
})

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('zh-CN')
}

function fmtDur(ms: number | null) {
  if (ms == null) return '-'
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
}

onMounted(() => store.fetchSummary())
</script>

<style scoped>
.dashboard-view { display: flex; flex-direction: column; gap: 16px; }

.empty-state { padding: 80px 0; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 900px) { .stat-row { grid-template-columns: repeat(2, 1fr); } }

.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value { font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }

.dash-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) { .dash-grid { grid-template-columns: 1fr; } }

.chart-box { padding: 8px; }
</style>
