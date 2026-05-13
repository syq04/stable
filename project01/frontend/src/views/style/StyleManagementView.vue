<template>
  <div class="style-page">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Brush /></el-icon> 风格管理</h3>
        <el-button type="primary" @click="openDialog(null)">
          <el-icon><Plus /></el-icon> 新建风格
        </el-button>
      </div>
      <div class="card-body">
        <div v-loading="loading" class="style-grid">
          <div v-for="item in styles" :key="item.id" class="style-card">
            <div class="style-preview" :style="{ background: !item.previewUrl ? (item.previewGradient || 'var(--bg-elevated)') : 'transparent' }">
              <el-image v-if="item.previewUrl" :src="item.previewUrl" fit="cover" class="preview-img" />
            </div>
            <div class="style-info">
              <div class="style-name">{{ item.name }}</div>
              <div class="style-desc">{{ item.description || '暂无描述' }}</div>
              <div class="style-meta">
                <el-tag size="small" effect="plain">{{ item.category || '通用' }}</el-tag>
                <span class="date">{{ item.updatedAt?.slice(0, 10) }}</span>
              </div>
            </div>
            <div class="style-actions">
              <el-button link type="primary" size="small" @click="openDialog(item)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(item)">删除</el-button>
            </div>
          </div>
          <div v-if="!loading && !styles.length" class="empty">
            <el-empty description="暂无风格" />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑风格' : '新建风格'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="风格名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width:100%">
            <el-option label="通用" value="general" />
            <el-option label="写实" value="realistic" />
            <el-option label="动漫" value="anime" />
            <el-option label="艺术" value="artistic" />
            <el-option label="摄影" value="photography" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="风格描述" />
        </el-form-item>
        <el-form-item label="预览图片">
          <div class="upload-wrapper">
            <el-upload
              class="preview-uploader"
              action="#"
              :show-file-list="false"
              accept="image/*"
              :before-upload="handlePreviewUpload"
            >
              <img v-if="form.previewUrl" :src="form.previewUrl" class="preview-img" />
              <el-icon v-else class="uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">
              <span v-if="form.previewUrl">已上传，点击更换</span>
              <span v-else>点击上传风格预览图</span>
            </div>
            <el-button v-if="form.previewUrl" size="small" type="danger" text @click.stop="form.previewUrl = ''">移除图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="提示词前缀">
          <el-input v-model="form.promptPrefix" type="textarea" :rows="2" placeholder="生成时自动添加的提示词前缀" />
        </el-form-item>
        <el-form-item label="提示词后缀">
          <el-input v-model="form.promptSuffix" type="textarea" :rows="2" placeholder="生成时自动添加的提示词后缀" />
        </el-form-item>
        <el-form-item label="默认参数">
          <div class="param-grid">
            <el-form-item label="步数">
              <el-input-number v-model="form.defaultSteps" :min="1" :max="150" />
            </el-form-item>
            <el-form-item label="CFG">
              <el-input-number v-model="form.defaultCfgScale" :min="1" :max="30" :step="0.5" />
            </el-form-item>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getStyleList, createStyle, updateStyle, deleteStyle } from '@/api/style'
import { uploadImage } from '@/api/upload'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const styles = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  category: 'general',
  description: '',
  previewUrl: '',
  promptPrefix: '',
  promptSuffix: '',
  defaultSteps: 30,
  defaultCfgScale: 7
})

const rules = {
  name: [{ required: true, message: '请输入风格名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getStyleList({ page: 1, size: 100 })
    styles.value = res.data.records || []
  } catch {} finally {
    loading.value = false
  }
}

function openDialog(item) {
  if (item) {
    editingId.value = item.id
    let parsed = {}
    try { parsed = JSON.parse(item.config || '{}') } catch {}
    Object.assign(form, {
      name: item.name,
      category: parsed.category || 'general',
      description: item.description || '',
      previewUrl: item.previewUrl || '',
      promptPrefix: parsed.promptPrefix || '',
      promptSuffix: parsed.promptSuffix || '',
      defaultSteps: parsed.defaultSteps || 30,
      defaultCfgScale: parsed.defaultCfgScale || 7
    })
  } else {
    editingId.value = null
    Object.assign(form, {
      name: '', category: 'general', description: '', previewUrl: '',
      promptPrefix: '', promptSuffix: '', defaultSteps: 30, defaultCfgScale: 7
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const config = JSON.stringify({
      category: form.category,
      promptPrefix: form.promptPrefix,
      promptSuffix: form.promptSuffix,
      defaultSteps: form.defaultSteps,
      defaultCfgScale: form.defaultCfgScale
    })
    const data = {
      name: form.name,
      description: form.description,
      previewUrl: form.previewUrl || null,
      config
    }
    if (editingId.value) {
      await updateStyle(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createStyle(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function handlePreviewUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  try {
    const res = await uploadImage(file)
    form.previewUrl = res.data
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || '未知错误'))
  }
  return false // 阻止自动上传
}

async function handleDelete(item) {
  await ElMessageBox.confirm(`确定删除风格「${item.name}」？`, '提示', { type: 'warning' })
  try {
    await deleteStyle(item.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped>
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
  padding: 20px;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.style-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow: hidden;
  transition: all 0.25s;
}

.style-card:hover {
  border-color: var(--border-hover);
  transform: translateY(-2px);
}

.style-preview {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-img {
  width: 100%;
  height: 100%;
}

.style-info {
  padding: 14px;
}

.style-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.style-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  line-height: 1.5;
}

.style-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.style-meta .date {
  font-size: 11px;
  color: var(--text-muted);
}

.style-actions {
  padding: 0 14px 14px;
  display: flex;
  gap: 8px;
}

.param-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.upload-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preview-uploader {
  width: 120px;
  height: 90px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}

.preview-uploader:hover {
  border-color: var(--accent);
}

.preview-uploader .preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-uploader .uploader-icon {
  font-size: 28px;
  color: var(--text-muted);
}

.upload-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.empty {
  grid-column: 1 / -1;
}
</style>
