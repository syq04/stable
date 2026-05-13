<template>
  <div class="admin-settings">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Setting /></el-icon> 系统配置</h3>
      </div>
      <div class="card-body" v-loading="loading">
        <el-form :model="form" label-width="160px" @submit.prevent="handleSave">
          <div class="section-title">基础配置</div>
          <el-form-item label="系统名称">
            <el-input v-model="form['app.name']" />
          </el-form-item>
          <el-form-item label="系统版本">
            <el-input v-model="form['app.version']" />
          </el-form-item>

          <div class="section-title">AI 服务配置</div>
          <el-form-item label="SD API 地址">
            <el-input v-model="form['ai.sd.api.url']" />
          </el-form-item>
          <el-form-item label="豆包 API 地址">
            <el-input v-model="form['ai.doubao.api.url']" />
          </el-form-item>
          <el-form-item label="OpenRouter API Key">
            <el-input v-model="form['ai.openrouter.api-key']" type="password" show-password placeholder="免费视觉模型，配置后可启用图生文" />
          </el-form-item>
          <el-form-item label="Gemini API Key">
            <el-input v-model="form['ai.gemini.api-key']" type="password" show-password placeholder="Google Gemini 视觉模型" />
          </el-form-item>
          <el-form-item label="HuggingFace API Key">
            <el-input v-model="form['ai.huggingface.api-key']" type="password" show-password placeholder="HuggingFace 模型服务" />
          </el-form-item>
          <el-form-item label="硅基流动 API Key">
            <el-input v-model="form['ai.siliconflow.api-key']" type="password" show-password placeholder="国内免费文生图，推荐" />
          </el-form-item>
          <el-form-item label="智谱AI API Key">
            <el-input v-model="form['ai.zhipu.api-key']" type="password" show-password placeholder="国内免费文生图+图生文，推荐" />
          </el-form-item>
          <el-form-item label="ComfyUI API 地址">
            <el-input v-model="form['ai.comfyui.api-url']" placeholder="http://127.0.0.1:8188" />
          </el-form-item>

          <div class="section-title">存储配置</div>
          <el-form-item label="MinIO 地址">
            <el-input v-model="form['storage.minio.url']" />
          </el-form-item>
          <el-form-item label="MinIO 存储桶">
            <el-input v-model="form['storage.minio.bucket']" />
          </el-form-item>

          <div class="section-title">安全配置</div>
          <el-form-item label="JWT 过期时间(小时)">
            <el-input-number v-model="form['security.jwt.expire-hours']" :min="1" :max="720" />
          </el-form-item>

          <div class="section-title">训练配置</div>
          <el-form-item label="默认训练轮数">
            <el-input-number v-model="form['training.default.epochs']" :min="1" :max="100" />
          </el-form-item>
          <el-form-item label="默认批次大小">
            <el-input-number v-model="form['training.default.batch-size']" :min="1" :max="64" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" native-type="submit">保存配置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSystemConfigs, updateSystemConfig } from '@/api/system'
import { useAiStore } from '@/stores/ai'
import { ElMessage } from 'element-plus'

const aiStore = useAiStore()

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  'app.name': '文图互转主题设计系统',
  'app.version': '1.0.0',
  'ai.sd.api.url': 'http://localhost:7860',
  'ai.doubao.api.url': 'https://api.doubao.com',
  'ai.openrouter.api-key': '',
  'ai.gemini.api-key': '',
  'ai.huggingface.api-key': '',
  'ai.siliconflow.api-key': '',
  'ai.zhipu.api-key': '',
  'ai.comfyui.api-url': 'http://127.0.0.1:8188',
  'storage.minio.url': 'http://localhost:9000',
  'storage.minio.bucket': 'ai-content',
  'security.jwt.expire-hours': '24',
  'training.default.epochs': '10',
  'training.default.batch-size': '8'
})

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    const records = res.data.records || []
    records.forEach(r => {
      if (r.configKey in form) {
        form[r.configKey] = r.configValue
      }
    })
  } catch {} finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  let savedCount = 0
  let failedKeys = []
  try {
    const entries = Object.entries(form)
    for (const [key, value] of entries) {
      try {
        await updateSystemConfig(key, { configValue: String(value) })
        savedCount++
      } catch (e) {
        failedKeys.push(key)
        console.error(`保存配置失败 [${key}]:`, e)
      }
    }
    if (failedKeys.length === 0) {
      ElMessage.success(`配置保存成功（共 ${savedCount} 项）`)
    } else {
      ElMessage.warning(`保存完成，${savedCount} 项成功，${failedKeys.length} 项失败：${failedKeys.join(', ')}`)
    }
    aiStore.fetchStatus()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>

<style scoped>
.card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow: hidden;
  max-width: 800px;
}

.card-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header h3 {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 .el-icon {
  color: var(--accent);
}

.card-body {
  padding: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-light);
  margin: 20px 0 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.section-title:first-child {
  margin-top: 0;
}
</style>
