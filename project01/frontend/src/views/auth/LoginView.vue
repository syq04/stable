<template>
  <div class="login-page">
    <div class="login-bg" />
    <div class="login-card">
      <div class="brand">
        <div class="logo"><el-icon :size="22"><MagicStick /></el-icon></div>
        <h1>文图互转设计</h1>
        <p>Nebula Studio</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱地址" size="large" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" class="login-btn">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="demo-accounts">
        <div class="demo-title">演示账号</div>
        <div class="account-list">
          <div class="account-item" @click="fillAccount('admin@nebula.com', 'admin123')">
            <el-tag type="danger" size="small">管理员</el-tag>
            <span class="account-email">admin@nebula.com</span>
            <span class="account-pwd">admin123</span>
          </div>
          <div class="account-item" @click="fillAccount('designer@nebula.com', 'designer123')">
            <el-tag type="warning" size="small">设计师</el-tag>
            <span class="account-email">designer@nebula.com</span>
            <span class="account-pwd">designer123</span>
          </div>
          <div class="account-item" @click="fillAccount('user@nebula.com', 'user123')">
            <el-tag type="info" size="small">普通用户</el-tag>
            <span class="account-email">user@nebula.com</span>
            <span class="account-pwd">user123</span>
          </div>
        </div>
      </div>
      <div class="footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  email: 'admin@nebula.com',
  password: 'admin123'
})

function fillAccount(email, password) {
  form.email = email
  form.password = password
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0b0f19 0%, #1a1040 50%, #0b0f19 100%);
}

.login-bg::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 50%, rgba(99, 102, 241, 0.08) 0%, transparent 50%),
              radial-gradient(ellipse at 70% 50%, rgba(34, 211, 238, 0.06) 0%, transparent 50%);
  animation: bgFloat 20s ease-in-out infinite;
}

@keyframes bgFloat {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-2%, 2%); }
}

.login-card {
  position: relative;
  width: 420px;
  max-width: 94vw;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 40px 36px;
  z-index: 1;
}

.brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand .logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
}

.brand h1 {
  font-family: 'Sora', sans-serif;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}

.brand p {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary), #7c3aed);
  border: none;
}

.footer {
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 20px;
}

.footer a {
  color: var(--primary-light);
  font-weight: 500;
}

.demo-accounts {
  margin-top: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
}

.demo-title {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  font-weight: 500;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.account-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 13px;
}

.account-item:hover {
  background: var(--bg-surface);
}

.account-email {
  color: var(--text-primary);
  font-weight: 500;
}

.account-pwd {
  margin-left: auto;
  color: var(--text-muted);
  font-family: monospace;
  font-size: 12px;
}
</style>
