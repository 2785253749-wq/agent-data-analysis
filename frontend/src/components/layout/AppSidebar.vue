<template>
  <aside class="app-sidebar" :style="{ width: width + 'px' }">
    <el-menu
      :default-active="activeMenu"
      :collapse="false"
      class="sidebar-menu"
      @select="onSelect"
    >
      <template v-for="item in menus" :key="item.key">
        <el-tooltip
          v-if="item.disabled"
          :content="item.tip || '该功能开发中'"
          placement="right"
          :show-after="200"
        >
          <el-menu-item :index="item.key" :disabled="true">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-tooltip>

        <el-menu-item
          v-else
          :index="item.key"
          :disabled="false"
          :class="{ 'logout-item': item.action === 'logout' }"
        >
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { MenuItem } from '@/utils/sidebarMenu'
import { resolveMenuClick, activeMenuKey } from '@/utils/sidebarMenu'

const props = defineProps<{
  menus: MenuItem[]
  width?: number
  /** currently selected dataset id (null if none) */
  datasetId?: number | null
}>()

const emit = defineEmits<{
  (e: 'logout'): void
}>()

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => activeMenuKey(route.path))

function onSelect(key: string) {
  const item = props.menus.find((m) => m.key === key)
  if (!item) return

  const action = resolveMenuClick(item, { datasetId: props.datasetId })

  if (action.type === 'navigate') {
    router.push(action.path)
  } else if (action.type === 'needs-dataset') {
    ElMessage.warning('请先选择数据集')
    router.push('/datasets')
  } else if (action.type === 'logout') {
    emit('logout')
  }
  // 'ignore' → do nothing (disabled handled by el-menu, but kept safe)
}
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
  height: 42px;
  line-height: 42px;
  font-size: 14px;
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

/* Disabled (dev-in-progress) items: gray text + icon, no hover bg */
.sidebar-menu :deep(.el-menu-item.is-disabled) {
  color: #c0c4cc;
  cursor: not-allowed;
}

.sidebar-menu :deep(.el-menu-item.is-disabled .el-icon) {
  color: #c0c4cc;
}

.sidebar-menu :deep(.el-menu-item.is-disabled:hover) {
  background-color: transparent;
}

/* Logout item: separated visually at the bottom */
.sidebar-menu :deep(.logout-item) {
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
}
</style>
