<template>
  <div class="admin-logs">
    <div class="card">
      <div class="card-header">
        <h3><el-icon><Document /></el-icon> 日志管理</h3>
        <div class="header-actions">
          <el-select v-model="operationType" placeholder="操作类型" clearable style="width:140px" @change="fetchData">
            <el-option label="CREATE" value="CREATE" />
            <el-option label="UPDATE" value="UPDATE" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="LOGIN" value="LOGIN" />
          </el-select>
          <el-select v-model="targetType" placeholder="目标类型" clearable style="width:140px" @change="fetchData">
            <el-option label="用户" value="USER" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="风格" value="STYLE" />
            <el-option label="配置" value="CONFIG" />
          </el-select>
          <el-button @click="handleExport">导出日志</el-button>
        </div>
      </div>
      <div class="card-body">
        <el-table :data="logs" stripe v-loading="loading" style="width:100%">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column prop="operationType" label="操作类型" width="110">
            <template #default="{ row }">
              <el-tag :type="opType(row.operationType)" size="small">{{ row.operationType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetType" label="目标类型" width="100" />
          <el-table-column prop="operatorId" label="操作人ID" width="100" />
          <el-table-column prop="ipAddress" label="IP地址" width="140" />
          <el-table-column label="详情" min-width="200">
            <template #default="{ row }">
              <span class="log-detail">{{ row.afterData || row.beforeData || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @change="fetchData"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSystemLogs, exportSystemLogs } from '@/api/system'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const operationType = ref('')
const targetType = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

function opType(op) {
  return { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', LOGIN: 'info' }[op] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, operationType: operationType.value, targetType: targetType.value }
    const res = await getSystemLogs(params)
    if (res && res.data) {
      logs.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      logs.value = []
      total.value = 0
    }
  } catch (e) {
    logs.value = []
    total.value = 0
    console.error('获取日志失败:', e)
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  try {
    const data = { operationType: operationType.value || undefined, targetType: targetType.value || undefined }
    const res = await exportSystemLogs(data)
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit-logs-${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败: ' + (e.message || '未知错误'))
  }
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
  flex-wrap: wrap;
  gap: 10px;
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

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.card-body {
  padding: 20px;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.log-detail {
  font-size: 12px;
  color: var(--text-secondary);
  word-break: break-all;
}
</style>
