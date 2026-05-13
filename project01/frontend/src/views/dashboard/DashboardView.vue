<template>
  <div class="dashboard-page">
    <div class="welcome">
      <h2>欢迎回来，{{ userStore.userInfo?.username }}</h2>
      <p>开始你的创意之旅</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <el-icon :size="20" :style="{ color: stat.color }"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h3>快速操作</h3>
      <div class="action-grid">
        <router-link to="/text2image" class="action-card">
          <el-icon :size="28"><Picture /></el-icon>
          <span>文生图</span>
        </router-link>
        <router-link to="/image2text" class="action-card">
          <el-icon :size="28"><EditPen /></el-icon>
          <span>图生文</span>
        </router-link>
        <router-link to="/styles" class="action-card" v-if="userStore.isDesigner">
          <el-icon :size="28"><Brush /></el-icon>
          <span>风格管理</span>
        </router-link>
        <router-link to="/lora" class="action-card" v-if="userStore.isDesigner">
          <el-icon :size="28"><Cpu /></el-icon>
          <span>LoRA训练</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const stats = ref([
  { label: '文生图次数', value: 0, icon: 'Picture', color: '#818cf8', bg: 'rgba(99,102,241,0.12)' },
  { label: '图生文次数', value: 0, icon: 'EditPen', color: '#22d3ee', bg: 'rgba(34,211,238,0.1)' },
  { label: '自定义风格', value: 0, icon: 'Brush', color: '#34d399', bg: 'rgba(52,211,153,0.1)' },
  { label: '训练任务', value: 0, icon: 'Cpu', color: '#fbbf24', bg: 'rgba(251,191,36,0.1)' }
])
</script>

<style scoped>
.welcome {
  margin-bottom: 28px;
}

.welcome h2 {
  font-family: 'Sora', sans-serif;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.welcome p {
  color: var(--text-muted);
  font-size: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 28px;
}

.stat-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.25s;
}

.stat-card:hover {
  border-color: var(--border-hover);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-family: 'Sora', sans-serif;
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.quick-actions h3 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}

.action-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s;
  cursor: pointer;
}

.action-card:hover {
  color: var(--primary-light);
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
