<template>
  <div class="admin-layout">
    <AppHeader @logout="onLogout" />
    <div class="layout-body">
      <AppSidebar :menus="menus" :width="155" />
      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import type { MenuItem } from '@/components/layout/AppSidebar.vue'
import { useAdminStore } from '@/stores/admin'
import {
  DataAnalysis, FolderOpened, Grid, TrendCharts, Document, Setting,
} from '@element-plus/icons-vue'

const router = useRouter()
const store = useAdminStore()

const currentDataset = computed(() => store.currentDataset)

// Full menu as in the reference image. Not-yet-implemented features are disabled.
const menus = computed<MenuItem[]>(() => [
  { path: '/', title: 'AI 数据分析', icon: DataAnalysis },
  { path: '/datasets', title: '数据集管理', icon: FolderOpened },
  {
    path: currentDataset.value
      ? `/datasets/${currentDataset.value.id}/fields`
      : '/datasets',
    title: '字段语义管理',
    icon: Grid,
    disabled: !currentDataset.value,
  },
  {
    path: currentDataset.value
      ? `/datasets/${currentDataset.value.id}/metrics`
      : '/datasets',
    title: '指标口径管理',
    icon: TrendCharts,
    disabled: !currentDataset.value,
  },
  { path: '/reports', title: '分析报告', icon: Document },
  // Reserved / not-yet-implemented menu items (kept visible as 开发中)
  { path: '/system', title: '系统设置', icon: Setting, disabled: true },
])

function onLogout() {
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.layout-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.layout-main {
  flex: 1;
  background: #f5f7fa;
  padding: 16px;
  overflow: auto;
}
</style>
