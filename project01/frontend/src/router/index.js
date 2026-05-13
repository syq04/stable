import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'password',
        name: 'Password',
        component: () => import('@/views/auth/PasswordView.vue'),
        meta: { title: '修改密码' }
      },
      {
        path: 'text2image',
        name: 'Text2Image',
        component: () => import('@/views/text2image/Text2ImageView.vue'),
        meta: { title: '文生图' }
      },
      {
        path: 'text2image/history',
        name: 'Text2ImageHistory',
        component: () => import('@/views/text2image/Text2ImageHistoryView.vue'),
        meta: { title: '文生图历史' }
      },
      {
        path: 'image2text',
        name: 'Image2Text',
        component: () => import('@/views/image2text/Image2TextView.vue'),
        meta: { title: '图生文' }
      },
      {
        path: 'image2text/history',
        name: 'Image2TextHistory',
        component: () => import('@/views/image2text/Image2TextHistoryView.vue'),
        meta: { title: '图生文历史' }
      },
      {
        path: 'styles',
        name: 'StyleManagement',
        component: () => import('@/views/style/StyleManagementView.vue'),
        meta: { title: '风格管理', roles: ['DESIGNER', 'ADMIN'] }
      },
      {
        path: 'lora',
        name: 'LoraTraining',
        component: () => import('@/views/lora/LoraTrainingView.vue'),
        meta: { title: 'LoRA训练', roles: ['DESIGNER', 'ADMIN'] }
      },
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '系统概览', roles: ['ADMIN'] }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/AdminSettingsView.vue'),
        meta: { title: '系统配置', roles: ['ADMIN'] }
      },
      {
        path: 'admin/logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/AdminLogsView.vue'),
        meta: { title: '日志管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/models',
        name: 'AdminModels',
        component: () => import('@/views/admin/AdminModelsView.vue'),
        meta: { title: '模型管理', roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || '文图互转主题设计系统'} - Nebula Studio`

  if (to.meta.requiresAuth !== false) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    if (!userStore.userInfo) {
      try {
        await userStore.fetchProfile()
      } catch {
        userStore.clearAuth()
        next({ name: 'Login' })
        return
      }
    }
    const requiredRoles = to.meta.roles
    if (requiredRoles && !requiredRoles.includes(userStore.userInfo.role)) {
      next({ name: 'Dashboard' })
      return
    }
  }

  if (to.meta.requiresAuth === false && to.name !== 'Login' && to.name !== 'Register') {
    next()
    return
  }

  next()
})

export default router
