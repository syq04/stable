import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getProfile } from '@/api/user'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isDesigner = computed(() => userInfo.value?.role === 'DESIGNER' || userInfo.value?.role === 'ADMIN')

  async function login(credentials) {
    const res = await loginApi(credentials)
    const data = res.data
    token.value = data.token
    userInfo.value = {
      id: data.userId,
      username: data.username,
      email: data.email,
      role: data.role,
      avatarUrl: data.avatarUrl
    }
    localStorage.setItem('token', data.token)
    return data
  }

  async function fetchProfile() {
    const res = await getProfile()
    userInfo.value = res.data
    return res.data
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return { token, userInfo, isLoggedIn, isAdmin, isDesigner, login, fetchProfile, logout, clearAuth }
})
