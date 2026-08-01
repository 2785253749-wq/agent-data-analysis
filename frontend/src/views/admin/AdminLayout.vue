<template>
  <Layout />
</template>

<script lang="ts">
// Legacy compat view — AdminLayout has moved to src/layouts/AdminLayout.vue.
// Kept as a forwarding wrapper so any existing reference keeps working.
import Layout from '@/layouts/AdminLayout.vue'

export default {
  name: 'AdminLayoutLegacy',
  components: { Layout },
}
</script>
