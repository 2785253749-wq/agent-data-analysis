<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <h3>管理后台</h3>
      <nav>
        <router-link to="/admin/datasets" class="nav-item">
          📊 数据集管理
        </router-link>
        <template v-if="currentDataset">
          <router-link
            :to="`/admin/datasets/${currentDataset.id}`"
            class="nav-item sub"
          >
            ✏️ 编辑：{{ currentDataset.name }}
          </router-link>
          <router-link
            :to="`/admin/datasets/${currentDataset.id}/fields`"
            class="nav-item sub"
          >
            📋 字段管理
          </router-link>
          <router-link
            :to="`/admin/datasets/${currentDataset.id}/metrics`"
            class="nav-item sub"
          >
            📐 指标管理
          </router-link>
        </template>
      </nav>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAdminStore } from '@/stores/admin'

const store = useAdminStore()
const currentDataset = computed(() => store.currentDataset)
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: calc(100vh - 56px);
  margin: -24px;
}

.sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  padding: 20px 0;
  flex-shrink: 0;
}

.sidebar h3 {
  padding: 0 20px 16px;
  font-size: 14px;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.nav-item {
  display: block;
  padding: 10px 20px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
}

.nav-item:hover {
  background: #f0f2f5;
}

.nav-item.router-link-active {
  background: #e8edf5;
  color: #1a1a2e;
  font-weight: 600;
  border-right: 3px solid #1a1a2e;
}

.nav-item.sub {
  padding-left: 36px;
  font-size: 13px;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-x: auto;
}
</style>
