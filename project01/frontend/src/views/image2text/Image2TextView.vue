<template>
  <div class="image2text-page">
    <AiStatusBar :hide-t2-i="true" />
    <div class="page-grid">
      <div class="panel-left">
        <div class="card">
          <div class="card-header">
            <h3><el-icon><Upload /></el-icon> 上传图片</h3>
          </div>
          <div class="card-body">
            <el-upload
              class="upload-area"
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="handleFileChange"
            >
              <div v-if="previewUrl" class="preview-wrap">
                <img :src="previewUrl" class="preview-img" />
              </div>
              <div v-else class="upload-placeholder">
                <el-icon :size="40" color="var(--text-muted)"><UploadFilled /></el-icon>
                <p>拖拽图片到此处或点击上传</p>
                <span>支持 JPG / PNG / WEBP，最大 10MB</span>
              </div>
            </el-upload>

            <el-form label-position="top" style="margin-top:20px">
              <el-form-item v-if="showProgress" label="分析进度">
                <el-progress :percentage="progress" :status="progress === 100 ? 'success' : undefined" :stroke-width="8" />
                <p class="progress-text">{{ progressText }}</p>
              </el-form-item>
              <el-button type="primary" size="large" :loading="analyzing" @click="handleAnalyze" class="analyze-btn" :disabled="!file">
                <el-icon><MagicStick /></el-icon> 开始分析
              </el-button>
            </el-form>
          </div>
        </div>
      </div>

      <div class="panel-right">
        <div class="card">
          <div class="card-header">
            <h3><el-icon><Document /></el-icon> 分析结果</h3>
          </div>
          <div class="card-body result-area">
            <div v-if="!result" class="empty-state">
              <el-icon :size="48" color="var(--text-muted)"><EditPen /></el-icon>
              <p>上传图片开始分析</p>
            </div>
            <div v-else class="result-content">
              <div class="result-section">
                <h4>图像描述</h4>
                <p>{{ result.description }}</p>
              </div>
              <div v-if="result.tags?.length" class="result-section">
                <h4>标签</h4>
                <div class="tag-list">
                  <el-tag v-for="tag in result.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                </div>
              </div>
              <div v-if="result.style" class="result-section">
                <h4>风格分析</h4>
                <p>{{ result.style }}</p>
              </div>
              <div v-if="result.prompt" class="result-section">
                <h4>生成提示词</h4>
                <div class="prompt-box">
                  <code>{{ result.prompt }}</code>
                  <el-button size="small" text @click="copyPrompt">
                    <el-icon><CopyDocument /></el-icon> 复制
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { analyzeImage } from '@/api/image2text'
import { ElMessage } from 'element-plus'
import AiStatusBar from '@/components/common/AiStatusBar.vue'

const file = ref(null)
const previewUrl = ref('')
const analyzing = ref(false)
const result = ref(null)
const progress = ref(0)
const progressText = ref('')
const showProgress = ref(false)

let progressTimer = null

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

function handleFileChange(uploadFile) {
  file.value = uploadFile.raw
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
  result.value = null
}

async function handleAnalyze() {
  if (!file.value) return
  analyzing.value = true
  result.value = null
  showProgress.value = true
  progress.value = 0
  progressText.value = '上传图片...'
  startProgress()
  try {
    const formData = new FormData()
    formData.append('image', file.value)
    const res = await analyzeImage(formData)
    const record = res.data
    if (record) {
      if (record.status === 'SUCCESS') {
        let parsed = {}
        try { parsed = JSON.parse(record.outputContent || '{}') } catch { console.error('Failed to parse outputContent:', record.outputContent) }
        result.value = {
          description: parsed.description || record.outputContent || '',
          tags: parsed.tags || [],
          style: parsed.style || '',
          prompt: parsed.prompt || ''
        }
        progress.value = 100
        progressText.value = '分析完成'
        setTimeout(() => { showProgress.value = false }, 1000)
        ElMessage.success('分析完成')
      } else {
        showProgress.value = false
        ElMessage.error(record.outputContent || '分析失败，请检查AI服务配置')
      }
    }
  } catch (e) {
    showProgress.value = false
    ElMessage.error(e?.message || '分析请求失败，请稍后重试')
    console.error('Image2Text analyze error:', e)
  } finally {
    analyzing.value = false
    if (progressTimer) clearInterval(progressTimer)
  }
}

function startProgress() {
  if (progressTimer) clearInterval(progressTimer)
  progressTimer = setInterval(() => {
    if (progress.value < 30) {
      progress.value += 3
    } else if (progress.value < 90) {
      progress.value += 1
      progressText.value = 'AI 分析中（预估）...'
    } else if (progress.value < 99) {
      progress.value += 0.5
      progressText.value = '处理结果...'
    }
  }, 300)
}

function copyPrompt() {
  if (result.value?.prompt) {
    navigator.clipboard.writeText(result.value.prompt)
    ElMessage.success('已复制到剪贴板')
  }
}
</script>

<style scoped>
.page-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
  align-items: start;
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow: hidden;
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
  padding: 20px;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  background: var(--bg-elevated);
  border: 2px dashed var(--border-color);
  border-radius: var(--radius);
  padding: 20px;
  transition: border-color 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--primary);
}

.upload-placeholder {
  text-align: center;
  padding: 20px 0;
}

.upload-placeholder p {
  margin-top: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.upload-placeholder span {
  font-size: 12px;
  color: var(--text-muted);
}

.preview-wrap {
  display: flex;
  justify-content: center;
}

.preview-img {
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--radius-sm);
}

.analyze-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--accent), #06b6d4);
  border: none;
  color: #0b0f19;
}

.progress-text {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.result-area {
  min-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  gap: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

.result-section {
  margin-bottom: 20px;
}

.result-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.result-section p {
  font-size: 14px;
  line-height: 1.7;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.prompt-box {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.prompt-box code {
  font-size: 13px;
  line-height: 1.6;
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 1024px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}
</style>
