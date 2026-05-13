<template>
  <div class="lora-page">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Cpu /></el-icon> LoRA 训练任务</h3>
        <el-button type="primary" @click="dialogVisible = true">
          <el-icon><Plus /></el-icon> 新建任务
        </el-button>
      </div>
      <div class="card-body">
        <el-table :data="tasks" stripe v-loading="loading" style="width:100%">
          <el-table-column prop="name" label="任务名称" min-width="160" />
          <el-table-column prop="baseModel" label="基础模型" width="140" />
          <el-table-column label="训练图片" width="100">
            <template #default="{ row }">{{ row.imageCount || 0 }} 张</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <el-progress v-if="row.status === 'TRAINING'" :percentage="row.progress || 0" :stroke-width="6" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click="startTask(row)">启动</el-button>
              <el-button v-if="row.status === 'TRAINING'" link type="primary" size="small" @click="viewLogs(row)">日志</el-button>
              <el-button v-if="row.status === 'COMPLETED'" link type="success" size="small" @click="downloadModel(row)">下载</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建训练任务" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="输入任务名称" />
        </el-form-item>
        <el-form-item label="基础模型" prop="baseModel">
          <el-select v-model="form.baseModel" style="width:100%">
            <el-option label="Stable Diffusion XL" value="sdxl" />
            <el-option label="Stable Diffusion 1.5" value="sd15" />
          </el-select>
        </el-form-item>
        <el-form-item label="训练图片" prop="images">
          <el-upload
            :auto-upload="false"
            :file-list="form.imageFiles"
            accept="image/*"
            list-type="picture-card"
            multiple
            :on-change="handleImageChange"
            :on-remove="handleImageRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="触发词" prop="triggerWord">
          <el-input v-model="form.triggerWord" placeholder="如: my_style" />
        </el-form-item>
        <div class="param-grid">
          <el-form-item label="训练步数">
            <el-input-number v-model="form.steps" :min="100" :max="10000" :step="100" />
          </el-form-item>
          <el-form-item label="学习率">
            <el-input-number v-model="form.learningRate" :min="0.00001" :max="0.01" :step="0.0001" :precision="5" />
          </el-form-item>
        </div>
        <el-form-item label="网络维度">
          <el-input-number v-model="form.networkDim" :min="1" :max="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logVisible" title="训练日志" width="700px">
      <div class="log-content">
        <pre>{{ logContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { createLoraTask, getLoraTaskList, startLoraTask, getLoraTaskLogs, downloadLoraModel, deleteLoraTask } from '@/api/lora'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const tasks = ref([])
const dialogVisible = ref(false)
const logVisible = ref(false)
const logContent = ref('')
const formRef = ref(null)

const form = reactive({
  name: '',
  baseModel: 'sdxl',
  imageFiles: [],
  triggerWord: '',
  steps: 1500,
  learningRate: 0.0001,
  networkDim: 16
})

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  baseModel: [{ required: true, message: '请选择基础模型', trigger: 'change' }],
  triggerWord: [{ required: true, message: '请输入触发词', trigger: 'blur' }]
}

function statusType(s) {
  return { PENDING: 'info', TRAINING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { PENDING: '待启动', TRAINING: '训练中', COMPLETED: '已完成', FAILED: '失败' }[s] || s
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLoraTaskList({ page: 1, size: 50 })
    tasks.value = res.data.records || []
  } catch {} finally {
    loading.value = false
  }
}

function handleImageChange(file, fileList) {
  form.imageFiles = fileList
}

function handleImageRemove(file, fileList) {
  form.imageFiles = fileList
}

async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const params = JSON.stringify({
      baseModel: form.baseModel,
      triggerWord: form.triggerWord,
      steps: form.steps,
      learningRate: form.learningRate,
      networkDim: form.networkDim,
      imageCount: form.imageFiles.length
    })
    await createLoraTask({ name: form.name, params, dataPath: '/upload/pending' })
    ElMessage.success('任务创建成功')
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    saving.value = false
  }
}

async function startTask(row) {
  try {
    await startLoraTask(row.id)
    ElMessage.success('任务已启动')
    fetchData()
  } catch {}
}

async function viewLogs(row) {
  try {
    const res = await getLoraTaskLogs(row.id)
    logContent.value = res.data?.logs || '暂无日志'
    logVisible.value = true
  } catch {}
}

async function downloadModel(row) {
  try {
    const res = await downloadLoraModel(row.id)
    const url = URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.name}.safetensors`
    a.click()
    URL.revokeObjectURL(url)
  } catch {}
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除任务「${row.name}」？`, '提示', { type: 'warning' })
  try {
    await deleteLoraTask(row.id)
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

.param-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.log-content {
  background: var(--bg-deep);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.log-content pre {
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: var(--text-secondary);
}
</style>
