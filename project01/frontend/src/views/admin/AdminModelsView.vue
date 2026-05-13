<template>
  <div class="admin-models">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Box /></el-icon> 模型管理</h3>
        <el-button type="primary" @click="uploadVisible = true">
          <el-icon><Upload /></el-icon> 上传模型
        </el-button>
      </div>
      <div class="card-body">
        <el-table :data="models" stripe v-loading="loading" style="width:100%">
          <el-table-column prop="name" label="模型名称" min-width="160" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type || '基础模型' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ row.sizeMb ? row.sizeMb + ' MB' : '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '启用' : '未启用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchData"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="uploadVisible" title="上传模型" width="480px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="模型名称">
          <el-input v-model="uploadForm.name" placeholder="模型名称" />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="uploadForm.type" style="width:100%">
            <el-option label="基础模型" value="base" />
            <el-option label="LoRA" value="lora" />
            <el-option label="VAE" value="vae" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="handleModelChange"
            accept=".safetensors,.ckpt,.pt,.bin"
          >
            <el-button>选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getModelList, uploadModel, deleteModel } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const uploading = ref(false)
const models = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const uploadVisible = ref(false)
const modelFile = ref(null)

const uploadForm = reactive({
  name: '',
  type: 'base'
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getModelList({ page: page.value, size: size.value })
    models.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

function handleModelChange(file) {
  modelFile.value = file.raw
}

async function handleUpload() {
  if (!modelFile.value) {
    ElMessage.warning('请选择模型文件')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('name', uploadForm.name)
    formData.append('file', modelFile.value)
    await uploadModel(formData)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchData()
  } catch {} finally {
    uploading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除模型「${row.name}」？`, '提示', { type: 'warning' })
  try {
    await deleteModel(row.id)
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

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
