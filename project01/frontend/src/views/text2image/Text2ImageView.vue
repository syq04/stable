<template>
  <div class="text2image-page">
    <AiStatusBar />
    <div class="page-grid">
      <div class="panel-left">
        <div class="card">
          <div class="card-header">
            <h3><el-icon><EditPen /></el-icon> 创作面板</h3>
            <div class="mode-tabs">
              <button
                :class="['mode-tab', { active: mode === 'simple' }]"
                @click="mode = 'simple'"
              >
                <el-icon><Sunny /></el-icon> 简单版
              </button>
              <button
                :class="['mode-tab', { active: mode === 'pro' }]"
                @click="mode = 'pro'"
              >
                <el-icon><Setting /></el-icon> 专业版
              </button>
            </div>
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

              <template v-if="mode === 'simple'">
                <el-form-item label="风格" class="compact-label">
                  <div class="style-tags">
                    <div
                      v-for="s in styles"
                      :key="s.id"
                      :class="['style-tag', { selected: form.styleId === s.id }]"
                      @click="form.styleId = form.styleId === s.id ? null : s.id"
                    >
                      {{ s.name }}
                    </div>
                  </div>
                </el-form-item>
                <el-form-item label="比例" class="compact-label">
                  <div class="ratio-options">
                    <div
                      v-for="r in ratioPresets"
                      :key="r.label"
                      :class="['ratio-option', { selected: form.width === r.width && form.height === r.height }]"
                      @click="form.width = r.width; form.height = r.height"
                    >
                      <div :class="['ratio-box', r.shape]"></div>
                      <span>{{ r.label }}</span>
                    </div>
                  </div>
                </el-form-item>
              </template>

              <template v-if="mode === 'pro'">
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
                <el-form-item label="LoRA 模型">
                  <el-select v-model="form.loraModelId" placeholder="选择 LoRA 模型（可选）" clearable style="width:100%">
                    <el-option v-for="lora in loraModels" :key="lora.id" :label="lora.name + ' (v' + lora.version + ')'" :value="lora.id" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="form.loraModelId != null" label="LoRA 权重">
                  <div class="slider-row">
                    <el-slider v-model="form.loraWeight" :min="0" :max="1" :step="0.05" class="slider-flex" />
                    <span style="width:40px;text-align:right">{{ form.loraWeight }}</span>
                  </div>
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
              </template>

              <el-button
                type="primary"
                size="large"
                :loading="generating"
                @click="handleGenerate"
                class="gen-btn"
              >
                <el-icon><MagicStick /></el-icon>
                {{ generating ? '生成中...' : '开始生成' }}
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
            <div v-if="!results.length" class="empty-state">
              <el-icon :size="48" color="var(--text-muted)"><Picture /></el-icon>
              <p>输入提示词开始创作</p>
            </div>
            <div v-else class="result-grid">
              <div v-for="(img, idx) in results" :key="idx" class="result-item">
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
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { generateImage } from '@/api/text2image'
import { getStyleList } from '@/api/style'
import { getLoraModels } from '@/api/lora'
import { ElMessage } from 'element-plus'
import AiStatusBar from '@/components/common/AiStatusBar.vue'

const mode = ref('simple')
const generating = ref(false)
const results = ref([])
const styles = ref([])
const loraModels = ref([])

const ratioPresets = [
  { label: '1:1', width: 512, height: 512, shape: 'square' },
  { label: '4:3', width: 640, height: 480, shape: 'landscape' },
  { label: '3:4', width: 480, height: 640, shape: 'portrait' },
  { label: '16:9', width: 768, height: 432, shape: 'wide' },
  { label: '9:16', width: 432, height: 768, shape: 'tall' }
]

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
  loraModelId: null,
  loraWeight: 0.7
})

onMounted(async () => {
  try {
    const res = await getStyleList({ page: 1, size: 100 })
    styles.value = res.data.records || []
  } catch {}
  try {
    const res = await getLoraModels({ page: 1, size: 100 })
    loraModels.value = res.data.records || []
  } catch {}
})

async function handleGenerate() {
  if (!form.prompt.trim()) {
    ElMessage.warning('请输入提示词')
    return
  }
  generating.value = true
  results.value = []
  try {
    const payload = { ...form }
    if (mode.value === 'simple') {
      delete payload.negativePrompt
      delete payload.samplerName
      payload.steps = 30
      payload.cfgScale = 7
      payload.seed = null
      payload.batchSize = 1
    }
    const res = await generateImage(payload)
    const record = res.data
    if (record) {
      if (record.status === 'SUCCESS') {
        results.value = [{
          id: record.id,
          url: record.imageUrl,
          prompt: record.inputContent,
          status: record.status
        }]
        ElMessage.success('生成完成')
      } else {
        results.value = []
        ElMessage.error(record.outputContent || '生成失败，请检查AI服务配置')
      }
    }
  } catch (e) {
    ElMessage.error('生成请求失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function downloadImage(url) {
  const a = document.createElement('a')
  a.href = url
  a.download = 'nebula-studio-' + Date.now() + '.png'
  a.click()
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

.mode-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-deep);
  border-radius: 8px;
  padding: 3px;
}

.mode-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.mode-tab:hover {
  color: var(--text-secondary);
}

.mode-tab.active {
  background: var(--primary);
  color: #fff;
  font-weight: 500;
}

.card-body {
  padding: 16px 20px;
}

.compact-label :deep(.el-form-item__label) {
  padding-bottom: 4px;
}

.compact-label :deep(.el-form-item__content) {
  line-height: 1;
}

.style-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-tag {
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.style-tag:hover {
  border-color: var(--border-hover);
  color: var(--text-primary);
  background: var(--bg-hover);
}

.style-tag.selected {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.12);
  color: var(--primary-light);
  font-weight: 500;
}

.ratio-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ratio-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
  min-width: 50px;
}

.ratio-option:hover {
  border-color: var(--border-hover);
  background: var(--bg-hover);
}

.ratio-option.selected {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.08);
}

.ratio-option span {
  font-size: 11px;
  color: var(--text-muted);
}

.ratio-option.selected span {
  color: var(--primary-light);
  font-weight: 500;
}

.ratio-box {
  border: 2px solid var(--text-muted);
  border-radius: 3px;
  transition: border-color 0.2s;
}

.ratio-option.selected .ratio-box {
  border-color: var(--primary);
}

.ratio-box.square {
  width: 20px;
  height: 20px;
}

.ratio-box.landscape {
  width: 24px;
  height: 18px;
}

.ratio-box.portrait {
  width: 18px;
  height: 24px;
}

.ratio-box.wide {
  width: 28px;
  height: 16px;
}

.ratio-box.tall {
  width: 16px;
  height: 28px;
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
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.result-item {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.result-item img {
  width: 100%;
  display: block;
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

@media (max-width: 1024px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}
</style>
