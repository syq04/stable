<template>
  <div class="ai-status-bar">
    <div class="provider-section">
      <div class="section-label">
        <el-icon><PictureFilled /></el-icon>
        <span>文生图</span>
      </div>
      <el-select
        v-model="activeT2I"
        size="small"
        class="provider-select"
        @change="handleSwitchT2I"
      >
        <el-option
          v-for="p in t2iList"
          :key="p.name"
          :label="getProviderLabel(p)"
          :value="p.name"
          :disabled="!p.available"
        >
          <div class="provider-option">
            <span>{{ getProviderDisplayName(p.name) }}</span>
            <el-tag :type="p.available ? 'success' : 'info'" size="small" effect="dark" round>
              {{ p.available ? '可用' : '不可用' }}
            </el-tag>
          </div>
        </el-option>
      </el-select>
    </div>

    <div class="status-divider"></div>

    <div class="provider-section">
      <div class="section-label">
        <el-icon><View /></el-icon>
        <span>图生文</span>
      </div>
      <el-select
        v-model="activeI2T"
        size="small"
        class="provider-select"
        @change="handleSwitchI2T"
      >
        <el-option
          v-for="p in i2tList"
          :key="p.name"
          :label="getProviderLabel(p)"
          :value="p.name"
          :disabled="!p.available"
        >
          <div class="provider-option">
            <span>{{ getProviderDisplayName(p.name) }}</span>
            <el-tag :type="p.available ? 'success' : 'info'" size="small" effect="dark" round>
              {{ p.available ? '可用' : '不可用' }}
            </el-tag>
          </div>
        </el-option>
      </el-select>
    </div>

    <el-button
      class="refresh-btn"
      :icon="Refresh"
      circle
      size="small"
      :loading="aiStore.loading"
      @click="aiStore.fetchStatus()"
    />
  </div>
</template>

<script setup>
import { onMounted, computed, ref, watch } from 'vue'
import { PictureFilled, View, Refresh } from '@element-plus/icons-vue'
import { useAiStore } from '@/stores/ai'

const aiStore = useAiStore()

const activeT2I = ref('')
const activeI2T = ref('')

const t2iList = computed(() => aiStore.t2iProviderList)
const i2tList = computed(() => aiStore.i2tProviderList)

watch(() => aiStore.activeText2ImageProvider, (val) => {
  activeT2I.value = val
}, { immediate: true })

watch(() => aiStore.activeImage2TextProvider, (val) => {
  activeI2T.value = val
}, { immediate: true })

function handleSwitchT2I(val) {
  aiStore.switchT2I(val)
}

function handleSwitchI2T(val) {
  aiStore.switchI2T(val)
}

function getProviderDisplayName(name) {
  const map = {
    'pollinations': 'Pollinations.ai',
    'huggingface': 'HuggingFace',
    'stable-diffusion': 'Stable Diffusion',
    'doubao': '豆包视觉',
    'gemini': 'Google Gemini',
    'openrouter': 'OpenRouter',
    'siliconflow': '硅基流动',
    'zhipu': '智谱AI',
    'comfyui': 'ComfyUI',
    'mock': '模拟模式'
  }
  return map[name] || name
}

function getProviderLabel(p) {
  const name = getProviderDisplayName(p.name)
  return p.available ? name : `${name} (不可用)`
}

onMounted(() => {
  aiStore.fetchStatus()
})
</script>

<style scoped>
.ai-status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  margin-bottom: 20px;
}

.provider-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}

.section-label .el-icon {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
}

.provider-select {
  width: 180px;
}

.provider-select :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

.provider-select :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.provider-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.status-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
}

.refresh-btn {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}
</style>
