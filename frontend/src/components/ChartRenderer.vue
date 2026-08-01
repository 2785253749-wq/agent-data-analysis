<template>
  <div class="chart-container">
    <div v-if="spec.type === 'table'" class="table-view">
      <h3 v-if="spec.title">{{ spec.title }}</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th v-for="col in spec.labels" :key="col">{{ col }}</th>
            <th v-for="ds in spec.datasets" :key="ds.label">{{ ds.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(label, idx) in spec.labels" :key="idx">
            <td>{{ label }}</td>
            <td v-for="ds in spec.datasets" :key="ds.label">
              {{ ds.data[idx] }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else :class="['chart-view', { 'pie-limited': spec.type === 'pie' }]">
      <h3 v-if="spec.title">{{ spec.title }}</h3>
      <v-chart
        v-if="chartOption"
        :option="chartOption"
        :autoresize="true"
        :style="{ width: '100%', height: chartHeight + 'px' }"
      />
      <div v-else class="empty">无法渲染图表：数据格式不正确</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  BarChart, LineChart, PieChart, ScatterChart,
} from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
} from 'echarts/components'
import type { ChartSpecDTO } from '@/api/datasets'

// Register ECharts modules
use([CanvasRenderer, BarChart, LineChart, PieChart, ScatterChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps<{
  spec: ChartSpecDTO
}>()

// Responsive height based on data volume; pie stays compact.
const chartHeight = computed(() => {
  const { type, labels } = props.spec
  const n = labels?.length || 0
  if (type === 'pie') return 360
  if (type === 'horizontal_bar') return Math.max(320, n * 30 + 100)
  return Math.max(360, n * 22 + 120) // bar / line / scatter
})

const chartOption = computed(() => {
  const { type, labels, datasets } = props.spec

  if (type === 'table') return null

  const base: any = {
    title: { text: props.spec.title, left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: datasets.map(d => d.label) },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
  }

  switch (type) {
    case 'line':
      base.xAxis = { type: 'category', data: labels }
      base.yAxis = { type: 'value' }
      base.series = datasets.map(d => ({
        name: d.label,
        type: 'line',
        data: d.data,
        smooth: true,
        itemStyle: d.color ? { color: d.color } : undefined,
      }))
      break

    case 'pie':
      delete base.tooltip
      delete base.grid
      delete base.xAxis
      delete base.yAxis
      base.tooltip = { trigger: 'item' }
      base.series = datasets.map(d => ({
        name: d.label,
        type: 'pie',
        radius: '62%',
        center: ['50%', '46%'],
        data: labels.map((l, i) => ({ name: l, value: d.data[i] })),
        itemStyle: d.color ? { color: d.color } : undefined,
      }))
      break

    case 'scatter':
      base.xAxis = { type: 'value' }
      base.yAxis = { type: 'value' }
      base.series = datasets.map((d, di) => ({
        name: d.label,
        type: 'scatter',
        data: d.data.map((val, i) => (di === 0 ? [val, datasets[1]?.data[i] ?? 0] : null)).filter(Boolean),
      }))
      break

    case 'horizontal_bar':
      base.xAxis = { type: 'value' }
      base.yAxis = { type: 'category', data: labels }
      base.series = datasets.map(d => ({
        name: d.label,
        type: 'bar',
        data: d.data,
        itemStyle: d.color ? { color: d.color } : undefined,
      }))
      break

    case 'bar':
    default:
      base.xAxis = { type: 'category', data: labels }
      base.yAxis = { type: 'value' }
      base.series = datasets.map(d => ({
        name: d.label,
        type: 'bar',
        data: d.data,
        itemStyle: d.color ? { color: d.color } : undefined,
      }))
      break
  }

  return base
})
</script>

<style scoped>
.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06);
}

.chart-view {
  width: 100%;
}

.pie-limited {
  max-width: 560px;
  margin: 0 auto;
}

.chart-container h3 {
  font-size: 16px;
  margin-bottom: 16px;
  text-align: center;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th {
  background: #f5f7fa;
  padding: 8px 12px;
  font-size: 13px;
  color: #666;
}
.data-table td {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}

.empty {
  text-align: center;
  padding: 60px;
  color: #888;
}
</style>
