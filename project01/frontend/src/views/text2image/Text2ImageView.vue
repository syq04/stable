<template>
  <div class="text2image-page">
    <AiStatusBar :hide-i2-t="true" />
    <div class="page-grid">
      <div class="panel-left">
        <div class="card">
          <div class="card-header">
            <h3><el-icon><EditPen /></el-icon> 创作面板</h3>
          </div>
          <div class="card-body">
            <el-form :model="form" label-position="top">
              <el-form-item label="提示词">
                <el-input
                  v-model="form.prompt"
                  type="textarea"
                  :rows="3"
                  placeholder="描述你想要生成的图像，例如：一只橘猫坐在窗台上，阳光洒落，温暖治愈..."
                />
              </el-form-item>

              <el-form-item label="模型选择">
                <el-select v-model="form.checkpointName" placeholder="选择模型（可选，默认使用第一个）" clearable style="width:100%">
                  <el-option v-for="m in availableModels" :key="m.name" :label="m.name + ' (' + m.sizeMb + ' MB)'" :value="m.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="反向提示词">
                <el-input
                  v-model="form.negativePrompt"
                  type="textarea"
                  :rows="2"
                  placeholder="描述你不想出现的内容..."
                />
              </el-form-item>
              <el-form-item label="风格">
                <el-select v-model="form.styleId" placeholder="选择风格" clearable style="width:100%">
                  <el-option v-for="s in styles" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
              </el-form-item>
              <div class="params-row">
                <el-form-item label="宽度">
                  <el-input-number v-model="form.width" :min="256" :max="2048" :step="64" style="width:100%" />
                </el-form-item>
                <el-form-item label="高度">
                  <el-input-number v-model="form.height" :min="256" :max="2048" :step="64" style="width:100%" />
                </el-form-item>
              </div>
              <el-form-item label="采样步数">
                <div class="slider-row">
                  <el-slider v-model="form.steps" :min="1" :max="150" class="slider-flex" />
                  <el-input-number v-model="form.steps" :min="1" :max="150" size="small" style="width:100px" />
                </div>
              </el-form-item>
              <el-form-item label="CFG Scale">
                <div class="slider-row">
                  <el-slider v-model="form.cfgScale" :min="1" :max="30" :step="0.5" class="slider-flex" />
                  <el-input-number v-model="form.cfgScale" :min="1" :max="30" :step="0.5" size="small" style="width:100px" />
                </div>
              </el-form-item>
              <el-form-item label="采样器">
                <el-select v-model="form.samplerName" placeholder="选择采样器" clearable style="width:100%">
                  <el-option v-for="sa in samplerOptions" :key="sa.value" :label="sa.label" :value="sa.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="种子">
                <div class="seed-row">
                  <el-input v-model.number="form.seed" placeholder="留空则随机" style="flex:1" />
                  <el-button @click="form.seed = Math.floor(Math.random() * 2147483647)" title="随机种子">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="生成数量">
                <el-input-number v-model="form.batchSize" :min="1" :max="4" style="width:100%" />
              </el-form-item>
              <el-form-item>
                <div class="evaluate-toggle">
                  <span class="toggle-label">评估模式</span>
                  <el-switch v-model="evaluateMode" />
                  <span class="toggle-hint">生成后自动评估准确度</span>
                </div>
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="generating"
                @click="handleGenerate"
                class="gen-btn"
              >
                <el-icon><MagicStick /></el-icon>
                {{ generating ? (evaluateMode ? '生成并评估中...' : '生成中...') : (evaluateMode ? '生成并评估' : '开始生成') }}
              </el-button>
            </el-form>
          </div>
        </div>
      </div>

      <div class="panel-right">
        <div class="card">
          <div class="card-header">
            <h3><el-icon><Picture /></el-icon> 生成结果</h3>
          </div>
          <div class="card-body result-area">
            <div v-if="showProgress" class="progress-panel">
              <el-progress :percentage="t2iProgress.percent" :status="t2iProgress.finished ? 'success' : undefined" :stroke-width="10" />
              <div class="progress-stats">
                <div class="progress-row">
                  <span class="progress-label">步骤</span>
                  <span class="progress-value">{{ t2iProgress.step }} / {{ t2iProgress.totalSteps }}</span>
                </div>
                <div class="progress-row">
                  <span class="progress-label">已用</span>
                  <span class="progress-value">{{ formatTime(t2iProgress.elapsed) }}</span>
                </div>
                <div class="progress-row">
                  <span class="progress-label">速度</span>
                  <span class="progress-value">{{ t2iProgress.its.toFixed(2) }} it/s</span>
                </div>
                <div class="progress-row" v-if="!t2iProgress.finished && t2iProgress.its > 0">
                  <span class="progress-label">剩余</span>
                  <span class="progress-value">{{ formatTime((t2iProgress.totalSteps - t2iProgress.step) / t2iProgress.its) }}</span>
                </div>
              </div>
              <p class="progress-status-text">{{ evaluateMode ? '生成中 → AI评估中...' : (t2iProgress.finished ? '✅ 生成完成' : '生成中...') }}</p>
            </div>
            <div v-if="!results.length && !showProgress" class="empty-state">
              <el-icon :size="48" color="var(--text-muted)"><Picture /></el-icon>
              <p>输入提示词开始创作</p>
            </div>
              <div v-else class="result-grid">
              <div v-for="(img, idx) in results" :key="idx" class="result-item">
                <div class="result-image-wrap">
                  <img :src="img.url" :alt="'生成图片 ' + (idx + 1)" />
                  <div class="result-overlay">
                    <span class="result-prompt">{{ img.prompt }}</span>
                    <div class="result-actions">
                      <el-button size="small" circle @click="downloadImage(img.url)" title="下载">
                        <el-icon><Download /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
                <div v-if="img.evaluation" class="eval-panel">
                  <div v-if="img.evaluation.accuracy == null && img.evaluation.evalError" class="eval-error">
                    <el-icon><WarningFilled /></el-icon> {{ img.evaluation.evalError }}
                  </div>
                  <div v-else class="eval-score-block">
                    <span class="eval-score-label">准确度</span>
                    <span class="eval-score-value" :class="getAccuracyClass(img.evaluation.accuracy)">{{ img.evaluation.accuracy ?? '--' }}%</span>
                  </div>
                  <div v-if="img.evaluation.accuracyDetail" class="eval-detail">
                    <span class="eval-detail-label">评分说明</span>
                    <p>{{ img.evaluation.accuracyDetail }}</p>
                  </div>
                  <div v-if="img.evaluation.analysis" class="eval-analysis">
                    <div v-if="img.evaluation.analysis.description" class="eval-field">
                      <span class="eval-field-label">图片描述</span>
                      <p>{{ img.evaluation.analysis.description }}</p>
                    </div>
                    <div v-if="img.evaluation.analysis.tags && img.evaluation.analysis.tags.length" class="eval-field">
                      <span class="eval-field-label">标签</span>
                      <div class="eval-tags">
                        <el-tag v-for="(tag, ti) in img.evaluation.analysis.tags" :key="ti" size="small">{{ tag }}</el-tag>
                      </div>
                    </div>
                    <div v-if="img.evaluation.analysis.style" class="eval-field">
                      <span class="eval-field-label">风格</span>
                      <p>{{ img.evaluation.analysis.style }}</p>
                    </div>
                    <div v-if="img.evaluation.analysis.prompt" class="eval-field">
                      <span class="eval-field-label">英文提示词</span>
                      <p class="eval-prompt-en">{{ img.evaluation.analysis.prompt }}</p>
                    </div>
                  </div>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { generateImage, getText2ImageProgress, evaluateImage } from '@/api/text2image'
import { getStyleList } from '@/api/style'
import { getText2ImageModels } from '@/api/text2image'
import { ElMessage } from 'element-plus'
import AiStatusBar from '@/components/common/AiStatusBar.vue'

const generating = ref(false)
const evaluateMode = ref(false)
const results = ref([])
const styles = ref([])
const availableModels = ref([])
const showProgress = ref(false)
const t2iProgress = ref({
  step: 0, totalSteps: 30, elapsed: 0, its: 0, finished: false, percent: 0
})

let pollTimer = null

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const samplerOptions = [
  { label: 'Euler', value: 'Euler' },
  { label: 'Euler a', value: 'Euler a' },
  { label: 'DPM++ 2M Karras', value: 'DPM++ 2M Karras' },
  { label: 'DPM++ SDE Karras', value: 'DPM++ SDE Karras' },
  { label: 'DDIM', value: 'DDIM' },
  { label: 'UniPC', value: 'UniPC' }
]

const form = reactive({
  prompt: '',
  negativePrompt: '',
  styleId: null,
  width: 512,
  height: 512,
  steps: 30,
  cfgScale: 7,
  seed: null,
  batchSize: 1,
  samplerName: null,
  checkpointName: null,
})

onMounted(async () => {
  try {
    const res = await getStyleList({ page: 1, size: 100 })
    styles.value = res.data.records || []
  } catch {
    console.error('Failed to load styles')
  }
  try {
    const res = await getText2ImageModels()
    availableModels.value = res.data || []
  } catch {
    console.error('Failed to load models')
  }
})

async function handleGenerate() {
  if (!form.prompt.trim()) {
    ElMessage.warning('请输入提示词')
    return
  }
  generating.value = true
  results.value = []
  showProgress.value = true
  t2iProgress.value = { step: 0, totalSteps: form.steps || 30, elapsed: 0, its: 0, finished: false, percent: 0 }
  const taskId = crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2)
  startPolling(taskId)
  const payload = { ...form, taskId }

  if (evaluateMode.value) {
    await handleEvaluate(payload, taskId)
  } else {
    await handlePlainGenerate(payload, taskId)
  }
}

async function handlePlainGenerate(payload, taskId) {
  try {
    const res = await generateImage(payload)
    if (pollTimer) clearInterval(pollTimer)
    const record = res.data
    if (record) {
      if (record.status === 'SUCCESS') {
        t2iProgress.value.finished = true
        t2iProgress.value.percent = 100
        results.value = [{
          id: record.id,
          url: record.imageUrl,
          prompt: record.inputContent,
          status: record.status
        }]
        setTimeout(() => { showProgress.value = false }, 1200)
        ElMessage.success('生成完成')
      } else {
        showProgress.value = false
        ElMessage.error(record.outputContent || '生成失败，请检查AI服务配置')
      }
    }
  } catch (e) {
    if (pollTimer) clearInterval(pollTimer)
    showProgress.value = false
    ElMessage.error(e?.message || '生成请求失败，请稍后重试')
    console.error('Text2Image generate error:', e)
  } finally {
    generating.value = false
  }
}

async function handleEvaluate(payload, taskId) {
  try {
    const res = await evaluateImage(payload)
    if (pollTimer) clearInterval(pollTimer)
    t2iProgress.value.finished = true
    t2iProgress.value.percent = 100
    const data = res.data
    if (data && data.imageUrl) {
      setTimeout(() => { showProgress.value = false }, 800)
      results.value = [{
        id: data.recordId,
        url: data.imageUrl,
        prompt: data.originalPrompt,
        status: data.status,
        evaluation: {
          accuracy: data.accuracy,
          accuracyDetail: data.accuracyDetail,
          analysis: data.analysis || data.analysisRaw || null,
          evalError: data.evalError || null
        }
      }]
      if (data.status === 'EVAL_FAILED') {
        ElMessage.warning(data.evalError || '图片已生成，但AI评估失败')
      } else {
        const acc = data.accuracy
        ElMessage.success(acc != null ? `评估完成 — 准确度: ${acc}%` : '评估完成')
      }
    } else {
      showProgress.value = false
      ElMessage.error('评估失败，未获取到图片')
    }
  } catch (e) {
    if (pollTimer) clearInterval(pollTimer)
    showProgress.value = false
    ElMessage.error(e?.message || '评估请求失败，请稍后重试')
    console.error('Text2Image evaluate error:', e)
  } finally {
    generating.value = false
  }
}

function startPolling(taskId) {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    try {
      const res = await getText2ImageProgress(taskId)
      const data = res.data || {}
      t2iProgress.value = {
        step: data.step || 0,
        totalSteps: data.totalSteps || 30,
        elapsed: data.elapsed || 0,
        its: data.its || 0,
        finished: data.finished || false,
        percent: Math.round(((data.step || 0) / (data.totalSteps || 30)) * 100)
      }
    } catch {}
  }, 300)
}

function formatTime(seconds) {
  if (!seconds || seconds <= 0) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return (m > 0 ? m + ':' : '') + String(s).padStart(2, '0') + 's'
}

function downloadImage(url) {
  const a = document.createElement('a')
  a.href = url
  a.download = 'nebula-studio-' + Date.now() + '.png'
  a.click()
}

function getAccuracyClass(accuracy) {
  if (accuracy == null) return ''
  if (accuracy >= 80) return 'acc-high'
  if (accuracy >= 60) return 'acc-mid'
  return 'acc-low'
}

function getAccuracyColor(accuracy) {
  if (accuracy == null) return '#909399'
  if (accuracy >= 80) return '#67c23a'
  if (accuracy >= 60) return '#e6a23c'
  return '#f56c6c'
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
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  padding: 16px 20px;
}

.params-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.slider-flex {
  flex: 1;
}

.seed-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.gen-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary), #7c3aed);
  border: none;
  margin-top: 4px;
}

.progress-panel {
  padding: 24px 16px;
}

.progress-stats {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.progress-label {
  color: var(--text-muted);
}

.progress-value {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.progress-status-text {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
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

.result-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-item {
  display: flex;
  gap: 14px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.result-image-wrap {
  position: relative;
  flex-shrink: 0;
  max-width: 400px;
}

.result-image-wrap img {
  width: 100%;
  max-height: 360px;
  display: block;
  object-fit: contain;
  background: var(--bg-page);
}

.result-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.result-item:hover .result-overlay {
  opacity: 1;
}

.result-prompt {
  font-size: 11px;
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.result-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.evaluate-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toggle-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.toggle-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.eval-panel {
  flex: 1;
  padding: 12px;
  background: var(--bg-page);
  min-width: 200px;
  max-height: 360px;
  overflow-y: auto;
}

.eval-error {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #e6a23c;
  padding: 6px 0;
  margin-bottom: 4px;
}

.eval-error .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.eval-score-block {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.eval-score-label {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.eval-score-value {
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.eval-score-value.acc-high { color: #22c55e; }
.eval-score-value.acc-mid { color: #eab308; }
.eval-score-value.acc-low { color: #ef4444; }

.eval-detail {
  margin-bottom: 10px;
}

.eval-detail-label {
  font-size: 11px;
  color: var(--text-muted);
  display: block;
  margin-bottom: 3px;
}

.eval-detail p {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0;
}

.eval-analysis {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.eval-field-label {
  font-size: 11px;
  color: var(--text-muted);
  display: block;
  margin-bottom: 2px;
}

.eval-field p {
  font-size: 12px;
  color: var(--text-primary);
  line-height: 1.45;
  margin: 0;
}

.eval-prompt-en {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: var(--accent);
}

.eval-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

@media (max-width: 1024px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}
</style>
