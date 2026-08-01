<template>
  <div class="filter-toolbar">
    <div class="toolbar-left">
      <span v-if="title" class="toolbar-title">{{ title }}</span>
      <slot name="left-extra" />
    </div>

    <div class="toolbar-right">
      <el-input
        v-if="showSearch"
        :model-value="modelValue"
        :placeholder="searchPlaceholder"
        :prefix-icon="Search"
        clearable
        class="search-input"
        @update:model-value="$emit('update:modelValue', $event)"
        @keyup.enter="$emit('search', (modelValue || '').trim())"
        @clear="$emit('search', '')"
      />
      <div class="toolbar-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  searchPlaceholder?: string
  modelValue?: string
  showSearch?: boolean
}>()

defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'search', value: string): void
}>()
</script>

<style scoped>
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 220px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}
</style>
