<template>
  <div class="history-page">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Clock /></el-icon> 图生文历史</h3>
        <el-input v-model="search" placeholder="搜索..." prefix-icon="Search" clearable style="width:240px" />
      </div>
      <div class="card-body">
        <el-table :data="records" stripe v-loading="loading" style="width:100%">
          <el-table-column label="图片" width="90">
            <template #default="{ row }">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="thumb" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="outputContent" label="分析结果" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="分析详情" width="600px">
      <div v-if="currentRecord" class="detail-content">
        <el-image v-if="currentRecord.imageUrl" :src="currentRecord.imageUrl" fit="contain" class="detail-img" />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="输入内容" :span="2">{{ currentRecord.inputContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分析结果" :span="2">{{ currentRecord.outputContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(currentRecord.status) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRecord.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getImage2TextHistory, deleteImage2TextRecord } from '@/api/image2text'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const records = ref([])
const search = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentRecord = ref(null)

function statusType(s) {
  return { PENDING: 'info', PROCESSING: 'warning', SUCCESS: 'success', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info'
}

function statusLabel(s) {
  return { PENDING: '等待中', PROCESSING: '分析中', SUCCESS: '已完成', COMPLETED: '已完成', FAILED: '失败' }[s] || s
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getImage2TextHistory({ page: page.value, size: size.value, keyword: search.value })
    records.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {} finally {
    loading.value = false
  }
}

function viewDetail(row) {
  currentRecord.value = row
  detailVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
  try {
    await deleteImage2TextRecord(row.id)
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

.thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.detail-img {
  width: 100%;
  max-height: 400px;
  margin-bottom: 16px;
  border-radius: var(--radius-sm);
}
</style>
