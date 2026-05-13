<template>
  <div class="main-layout">
    <div class="mobile-overlay" :class="{ show: sidebarOpen }" @click="sidebarOpen = false" />
    <AppSidebar :open="sidebarOpen" @close="sidebarOpen = false" />
    <div class="main-area">
      <AppHeader @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const sidebarOpen = ref(false)
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
}

.mobile-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 99;
}

.mobile-overlay.show {
  display: block;
}

.main-area {
  flex: 1;
  margin-left: var(--sidebar-w);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0;
  }

  .main-content {
    padding: 16px;
  }
}
</style>
