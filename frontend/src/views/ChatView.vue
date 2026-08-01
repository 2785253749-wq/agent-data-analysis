<template>
  <AnalysisView />
</template>

<script lang="ts">
// Legacy compat view — ChatView superseded by AnalysisView (Phase 5).
// Kept as a forwarding wrapper so any existing reference keeps working.
import AnalysisView from '@/views/AnalysisView.vue'

export default {
  name: 'ChatView',
  components: { AnalysisView },
}
</script>
