<template>
  <div class="data-table-card">
    <!-- Header -->
    <div class="card-header" v-if="title || $slots.extra">
      <span class="card-title">{{ title }}</span>
      <div class="card-extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="card-header slot-header" v-else-if="$slots.header">
      <slot name="header" />
    </div>

    <!-- Body -->
    <div class="card-body">
      <slot />
    </div>

    <!-- Footer -->
    <div class="card-footer" v-if="$slots.footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
  }>(),
  {
    title: '',
  },
)
</script>

<style scoped>
.data-table-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeef5;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.slot-header {
  padding: 0;
}

.card-body {
  padding: 0;
}

.card-footer {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
}
</style>
