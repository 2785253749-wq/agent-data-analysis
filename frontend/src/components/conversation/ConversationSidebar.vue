<template>
  <div class="conv-sidebar">
    <div class="conv-head">
      <span class="conv-title">多轮会话</span>
      <el-button type="primary" size="small" :icon="Plus" @click="onCreate">新建</el-button>
    </div>

    <div class="conv-list">
      <div
        v-for="c in store.conversations"
        :key="c.id"
        :class="['conv-item', { active: c.id === store.activeId }]"
        @click="store.open(c.id)"
      >
        <span class="conv-name">{{ c.title }}</span>
        <span class="conv-count">{{ c.taskCount }}轮</span>
        <el-dropdown trigger="click" class="conv-actions" @command="(cmd: string) => onCommand(cmd, c.id)">
          <el-icon><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item command="archive" divided>归档</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div v-if="!store.conversations.length" class="conv-empty">
        暂无会话，点击「新建」开始
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled } from '@element-plus/icons-vue'
import { useConversationStore } from '@/stores/conversation'

const store = useConversationStore()
const newTitle = ref('')

async function onCreate() {
  const { value } = await ElMessageBox.prompt('请输入会话标题', '新建会话', {
    confirmButtonText: '创建', cancelButtonText: '取消',
    inputPlaceholder: '例如：Q2销售分析',
  })
  if (value?.trim()) {
    await store.create(value.trim(), null)
    ElMessage.success('会话已创建')
  }
}

async function onCommand(cmd: string, id: number) {
  if (cmd === 'rename') {
    const { value } = await ElMessageBox.prompt('请输入新标题', '重命名会话', {
      confirmButtonText: '保存', cancelButtonText: '取消',
    })
    if (value?.trim()) {
      await store.rename(id, value.trim())
      ElMessage.success('已重命名')
    }
  } else if (cmd === 'archive') {
    await ElMessageBox.confirm('归档后该会话不可继续提问，确定归档？', '归档确认', {
      type: 'warning', confirmButtonText: '归档', cancelButtonText: '取消',
    })
    await store.archive(id)
    ElMessage.success('已归档')
  }
}
</script>

<style scoped>
.conv-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-right: 1px solid #ebeef5;
  background: #fff;
}

.conv-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.conv-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.conv-item:hover { background: #f0f2f5; }

.conv-item.active { background: #ecf5ff; }

.conv-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-count {
  font-size: 11px;
  color: #909399;
}

.conv-actions { cursor: pointer; color: #909399; }

.conv-empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
