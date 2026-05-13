<template>
  <aside class="sidebar" :class="{ open }">
    <div class="sidebar-brand">
      <div class="logo"><el-icon :size="18"><MagicStick /></el-icon></div>
      <h2>文图互转设计</h2>
    </div>

    <nav class="sidebar-nav">
      <div class="nav-section">
        <div class="nav-section-title">概览</div>
        <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }" @click="$emit('close')">
          <el-icon><Grid /></el-icon> 工作台
        </router-link>
      </div>

      <div class="nav-section">
        <div class="nav-section-title">创作</div>
        <router-link to="/text2image" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Picture /></el-icon> 文生图
        </router-link>
        <router-link to="/image2text" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><EditPen /></el-icon> 图生文
        </router-link>
      </div>

      <div v-if="userStore.isDesigner" class="nav-section">
        <div class="nav-section-title">管理</div>
        <router-link to="/styles" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Brush /></el-icon> 风格管理
        </router-link>
        <router-link to="/lora" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Cpu /></el-icon> LoRA训练
        </router-link>
      </div>

      <div v-if="userStore.isAdmin" class="nav-section">
        <div class="nav-section-title">系统</div>
        <router-link to="/admin" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><DataAnalysis /></el-icon> 系统概览
        </router-link>
        <router-link to="/admin/users" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><UserFilled /></el-icon> 用户管理
        </router-link>
        <router-link to="/admin/settings" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Setting /></el-icon> 系统配置
        </router-link>
        <router-link to="/admin/logs" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Document /></el-icon> 日志管理
        </router-link>
        <router-link to="/admin/models" class="nav-item" active-class="active" @click="$emit('close')">
          <el-icon><Box /></el-icon> 模型管理
        </router-link>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div class="sidebar-user">
        <div class="avatar">{{ userStore.userInfo?.username?.[0] || '?' }}</div>
        <div class="info">
          <div class="name">{{ userStore.userInfo?.username || '未登录' }}</div>
          <div class="role">{{ roleLabel }}</div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

defineProps({ open: Boolean })
defineEmits(['close'])

const userStore = useUserStore()

const roleLabel = computed(() => {
  const map = { ADMIN: '系统管理员', DESIGNER: '设计师', USER: '普通用户' }
  return map[userStore.userInfo?.role] || '未知'
})
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-w);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background: linear-gradient(180deg, #0e1322, #111729);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  z-index: 100;
  transition: transform 0.3s;
}

.sidebar-brand {
  height: var(--header-h);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.sidebar-brand .logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.sidebar-brand h2 {
  font-family: 'Sora', sans-serif;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
}

.nav-section {
  margin-bottom: 8px;
}

.nav-section-title {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--text-muted);
  padding: 8px 12px 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
  position: relative;
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-elevated);
}

.nav-item.active {
  color: var(--primary-light);
  background: rgba(99, 102, 241, 0.1);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--primary);
  border-radius: 0 3px 3px 0;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-user:hover {
  background: var(--bg-elevated);
}

.sidebar-user .avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary), #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  font-weight: 600;
}

.sidebar-user .info {
  flex: 1;
  min-width: 0;
}

.sidebar-user .name {
  font-size: 13px;
  font-weight: 500;
}

.sidebar-user .role {
  font-size: 11px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.open {
    transform: translateX(0);
  }
}
</style>
