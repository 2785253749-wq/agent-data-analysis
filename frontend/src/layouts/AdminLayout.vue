<template>
  <div class="admin-layout">
    <AppHeader @logout="onLogout" />
    <div class="layout-body">
      <AppSidebar
        :menus="menus"
        :width="155"
        :dataset-id="currentDataset?.id ?? null"
        @logout="onLogout"
      />
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
import { useAdminStore } from '@/stores/admin'
import { clearSessionStorage } from '@/utils/sidebarMenu'
import type { MenuItem } from '@/utils/sidebarMenu'
import {
  DataAnalysis, FolderOpened, Grid, TrendCharts, Document,
  HomeFilled, User, Collection, Cpu, DocumentCopy, ChatDotRound,
  SetUp, Monitor, List, Clock, Bell, Setting, SwitchButton,
} from '@element-plus/icons-vue'

const router = useRouter()
const store = useAdminStore()

const currentDataset = computed(() => store.currentDataset)

const DEV_TIP = '该功能开发中'

/**
 * Full sidebar menu.
 * Implemented items navigate; dev-in-progress items stay visible but disabled.
 */
const menus = computed<MenuItem[]>(() => [
  // ---- Implemented ----
  { key: '/', path: '/', title: 'AI 数据分析', icon: DataAnalysis },
  { key: '/datasets', path: '/datasets', title: '数据集管理', icon: FolderOpened },
  {
    key: '/datasets/fields',
    title: '字段语义管理',
    icon: Grid,
    needsDataset: true,
    datasetSuffix: 'fields',
  },
  {
    key: '/datasets/metrics',
    title: '指标口径管理',
    icon: TrendCharts,
    needsDataset: true,
    datasetSuffix: 'metrics',
  },
  { key: '/reports', path: '/reports', title: '分析报告', icon: Document },

  // ---- Dev-in-progress (visible, disabled, hover tip) ----
  { key: '/dev/home', title: '系统首页', icon: HomeFilled, disabled: true, tip: DEV_TIP },
  { key: '/dev/user', title: '用户管理', icon: User, disabled: true, tip: DEV_TIP },
  { key: '/dev/tables', title: '数据表管理', icon: Collection, disabled: true, tip: DEV_TIP },
  { key: '/dev/model', title: 'AI 模型配置', icon: Cpu, disabled: true, tip: DEV_TIP },
  { key: '/dev/prompt', title: 'Prompt 模板管理', icon: DocumentCopy, disabled: true, tip: DEV_TIP },
  { key: '/dev/session', title: '多轮分析会话', icon: ChatDotRound, disabled: true, tip: DEV_TIP },
  { key: '/dev/plan', title: 'Agent 分析计划', icon: SetUp, disabled: true, tip: DEV_TIP },
  { key: '/dev/trace', title: 'Agent 执行追踪', icon: Monitor, disabled: true, tip: DEV_TIP },
  { key: '/dev/logs', title: '操作日志', icon: List, disabled: true, tip: DEV_TIP },
  { key: '/dev/history', title: '分析历史', icon: Clock, disabled: true, tip: DEV_TIP },
  { key: '/dev/notify', title: '消息通知', icon: Bell, disabled: true, tip: DEV_TIP },
  { key: '/dev/settings', title: '系统设置', icon: Setting, disabled: true, tip: DEV_TIP },

  // ---- Logout ----
  { key: 'logout', title: '退出系统', icon: SwitchButton, action: 'logout' },
])

function onLogout() {
  clearSessionStorage()
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
