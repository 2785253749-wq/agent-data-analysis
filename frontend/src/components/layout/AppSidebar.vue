<template>
  <aside class="app-sidebar" :style="{ width: width + 'px' }">
    <el-menu :default-active="activeMenu" router class="sidebar-menu">
      <el-menu-item
        v-for="item in menus"
        :key="item.path"
        :index="item.path"
        :disabled="item.disabled"
      >
        <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'

export interface MenuItem {
  path: string
  title: string
  icon?: Component
  disabled?: boolean
}

const props = defineProps<{
  menus: MenuItem[]
  width?: number
}>()

const route = useRoute()

// Prefix-match so /datasets/3/fields highlights "数据集管理"
const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/datasets')) return '/datasets'
  if (p.startsWith('/reports')) return '/reports'
  if (p.startsWith('/analysis')) return '/analysis'
  return p
})
</script>

<style scoped>
.app-sidebar {
  background: #fff;
  border-right: 1px solid #e6e6e6;
  flex-shrink: 0;
  padding-top: 8px;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
}

/* Active item: light blue bg + blue text (NOT solid dark blue) */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #409eff;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #f0f2f5;
}
</style>
