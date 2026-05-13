import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAiServiceStatus, getAiProviders, switchText2ImageProvider, switchImage2TextProvider } from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const text2ImageProviders = ref({})
  const image2TextProviders = ref({})
  const activeText2ImageProvider = ref('')
  const activeImage2TextProvider = ref('')
  const loading = ref(false)
  const lastChecked = ref(null)

  async function fetchStatus() {
    if (loading.value) return
    loading.value = true
    try {
      const res = await getAiServiceStatus()
      const data = res.data
      text2ImageProviders.value = data.text2ImageProviders || {}
      image2TextProviders.value = data.image2TextProviders || {}
      activeText2ImageProvider.value = data.activeText2ImageProvider || ''
      activeImage2TextProvider.value = data.activeImage2TextProvider || ''
      lastChecked.value = new Date().toLocaleTimeString()
    } catch (e) {
      console.error('Failed to fetch AI service status:', e)
    } finally {
      loading.value = false
    }
  }

  async function switchT2I(providerName) {
    try {
      await switchText2ImageProvider(providerName)
      activeText2ImageProvider.value = providerName
    } catch (e) {
      console.error('Failed to switch text2image provider:', e)
    }
  }

  async function switchI2T(providerName) {
    try {
      await switchImage2TextProvider(providerName)
      activeImage2TextProvider.value = providerName
    } catch (e) {
      console.error('Failed to switch image2text provider:', e)
    }
  }

  const t2iProviderList = computed(() => {
    return Object.entries(text2ImageProviders.value).map(([name, info]) => ({
      name,
      ...info
    }))
  })

  const i2tProviderList = computed(() => {
    return Object.entries(image2TextProviders.value).map(([name, info]) => ({
      name,
      ...info
    }))
  })

  const activeT2IInfo = computed(() => {
    return text2ImageProviders.value[activeText2ImageProvider.value] || null
  })

  const activeI2TInfo = computed(() => {
    return image2TextProviders.value[activeImage2TextProvider.value] || null
  })

  function isProviderAvailable(name) {
    return text2ImageProviders.value[name]?.available || image2TextProviders.value[name]?.available || false
  }

  return {
    text2ImageProviders,
    image2TextProviders,
    activeText2ImageProvider,
    activeImage2TextProvider,
    loading,
    lastChecked,
    t2iProviderList,
    i2tProviderList,
    activeT2IInfo,
    activeI2TInfo,
    fetchStatus,
    switchT2I,
    switchI2T,
    isProviderAvailable
  }
})
