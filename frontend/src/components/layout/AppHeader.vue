<template>
  <header class="app-header">
    <!-- Left: logo + title -->
    <div class="header-left">
      <el-icon class="logo-icon" :size="20" color="#409eff">
        <Cpu />
      </el-icon>
      <span class="logo-title">AI Agent 数据分析平台</span>
    </div>

    <!-- Center: breadcrumb -->
    <div class="header-center">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="c in breadcrumbs" :key="c">
          {{ c }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- Right: avatar + name + dropdown -->
    <div class="header-right">
      <el-dropdown trigger="click" @command="onCommand">
        <span class="user-info">
          <el-avatar :size="28" :src="avatarUrl" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <span class="user-name">{{ username }}</span>
          <el-icon class="arrow-down"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" disabled>个人中心</el-dropdown-item>
            <el-dropdown-item command="settings" disabled>系统设置</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Cpu, User, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps<{
  username?: string
}>()

const emit = defineEmits<{
  (e: 'logout'): void
}>()

const route = useRoute()
const username = computed(() => props.username || 'admin')
const avatarUrl = '' // no real avatar; el-avatar falls back to icon

const breadcrumbs = computed(() =>
  route.matched.filter((r) => r.meta?.title).map((r) => r.meta.title as string)
)

function onCommand(cmd: string) {
  if (cmd === 'logout') emit('logout')
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 52px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  z-index: 10;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.logo-icon {
  background: #ecf5ff;
  border-radius: 5px;
  padding: 2px;
}

.logo-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.header-center {
  flex: 1;
  min-width: 0;
}

.header-right {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: #409eff;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.arrow-down {
  font-size: 12px;
  color: #909399;
}
</style>
