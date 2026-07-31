<template>
  <div class="login-placeholder">
    <div class="card">
      <h2>登录</h2>
      <p class="hint">认证系统将在后续迭代中实现（JWT + Spring Security）</p>
      <div class="status-row">
        <span class="label">后端状态：</span>
        <span :class="['badge', healthStatus === 'UP' ? 'up' : 'down']">
          {{ healthStatus || '检测中...' }}
        </span>
      </div>
      <button class="btn" @click="checkHealth">检测后端连接</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHealth } from '@/api/client'

const healthStatus = ref<string>('')

async function checkHealth() {
  try {
    const resp = await getHealth()
    healthStatus.value = resp.status
  } catch {
    healthStatus.value = 'DOWN'
  }
}

onMounted(() => {
  checkHealth()
})
</script>

<style scoped>
.login-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  padding: 40px;
  width: 400px;
  text-align: center;
}

.card h2 {
  margin-bottom: 12px;
  font-size: 24px;
}

.hint {
  color: #888;
  font-size: 13px;
  margin-bottom: 24px;
}

.status-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.badge {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.badge.up {
  background: #e6f7e6;
  color: #2e7d32;
}

.badge.down {
  background: #fdecea;
  color: #c62828;
}

.btn {
  padding: 8px 24px;
  border: 1px solid #1a1a2e;
  border-radius: 6px;
  background: #1a1a2e;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn:hover {
  background: #2a2a4e;
}
</style>
