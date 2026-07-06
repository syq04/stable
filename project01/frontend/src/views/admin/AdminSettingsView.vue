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
          <el-form-item label="推理服务地址">
            <el-input v-model="form['ai.local-model.api-url']" placeholder="http://127.0.0.1:5000" />
          </el-form-item>
          <el-form-item label="模型目录">
            <el-input v-model="form['ai.local-model.model-dir']" placeholder="../sd-models" />
          </el-form-item>

          <div class="section-title">千问配置</div>
          <el-form-item label="API Key">
            <el-input v-model="form['ai.qwen.api-key']" :type="showApiKey ? 'text' : 'password'" placeholder="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxx">
              <template #suffix>
                <el-icon class="toggle-pwd" @click="showApiKey = !showApiKey">
                  <View v-if="!showApiKey" /><Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="API 地址">
            <el-input v-model="form['ai.qwen.api-url']" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" />
          </el-form-item>
          <el-form-item label="模型名称">
            <el-input v-model="form['ai.qwen.model']" placeholder="qwen3.5-omni-plus" />
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
import { View, Hide } from '@element-plus/icons-vue'

const aiStore = useAiStore()

const loading = ref(false)
const saving = ref(false)
const showApiKey = ref(false)

const form = reactive({
  'app.name': '文图互转主题设计系统',
  'app.version': '1.0.0',
  'ai.local-model.api-url': 'http://127.0.0.1:5000',
  'ai.local-model.model-dir': '../sd-models',
  'ai.qwen.api-key': '',
  'ai.qwen.api-url': 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
  'ai.qwen.model': 'qwen3.5-omni-plus',
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

.toggle-pwd {
  cursor: pointer;
  color: var(--text-muted);
}

.toggle-pwd:hover {
  color: var(--text-primary);
}
</style>
