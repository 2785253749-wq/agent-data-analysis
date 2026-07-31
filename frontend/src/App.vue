<template>
  <div id="app-container">
    <header class="app-header">
      <h1>AI-Agent 数据分析平台</h1>
      <nav class="nav-links">
        <router-link to="/">对话</router-link>
        <router-link to="/admin/datasets">管理</router-link>
      </nav>
      <span class="version">v0.1.0</span>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// App shell — navigation and layout expanded in later tasks
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
  color: #1a1a2e;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #1a1a2e;
  color: #fff;
}

.app-header h1 {
  font-size: 18px;
  font-weight: 600;
}

.nav-links {
  display: flex;
  gap: 20px;
}

.nav-links a {
  color: #a0a0b0;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  color: #fff;
  border-bottom-color: #5470c6;
}

.app-header .version {
  font-size: 12px;
  color: #a0a0b0;
  font-family: monospace;
}

.app-main {
  min-height: calc(100vh - 56px);
  padding: 24px;
}
</style>
