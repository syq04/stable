<template>
  <div class="admin-dashboard">
    <div class="stats-row">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <el-icon :size="22" :style="{ color: stat.color }"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-header"><h3>AI 服务状态</h3></div>
        <div class="card-body">
          <div class="provider-group">
            <div class="provider-group-title">文生图提供商</div>
            <div v-for="(info, name) in aiStatus.text2ImageProviders" :key="'t2i-'+name" class="ai-service-item">
              <div class="ai-service-info">
                <el-icon :size="18" :color="info.available ? '#67c23a' : '#909399'"><PictureFilled /></el-icon>
                <div>
                  <div class="ai-service-name">
                    {{ getProviderDisplayName(name) }}
                    <el-tag v-if="info.active" type="primary" size="small" effect="dark" round style="margin-left:6px">当前</el-tag>
                  </div>
                  <div class="ai-service-msg">{{ info.message || '未知状态' }}</div>
                </div>
              </div>
              <el-tag :type="info.available ? 'success' : 'info'" size="small" effect="dark" round>
                {{ info.available ? '可用' : '不可用' }}
              </el-tag>
            </div>
          </div>
          <div class="provider-group" style="margin-top:16px">
            <div class="provider-group-title">图生文提供商</div>
            <div v-for="(info, name) in aiStatus.image2TextProviders" :key="'i2t-'+name" class="ai-service-item">
              <div class="ai-service-info">
                <el-icon :size="18" :color="info.available ? '#67c23a' : '#909399'"><View /></el-icon>
                <div>
                  <div class="ai-service-name">
                    {{ getProviderDisplayName(name) }}
                    <el-tag v-if="info.active" type="primary" size="small" effect="dark" round style="margin-left:6px">当前</el-tag>
                  </div>
                  <div class="ai-service-msg">{{ info.message || '未知状态' }}</div>
                </div>
              </div>
              <el-tag :type="info.available ? 'success' : 'info'" size="small" effect="dark" round>
                {{ info.available ? '可用' : '不可用' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3>系统资源</h3></div>
        <div class="card-body">
          <div class="resource-item">
            <div class="resource-label">CPU 使用率</div>
            <el-progress :percentage="systemInfo.cpu || 0" :stroke-width="8" />
          </div>
          <div class="resource-item">
            <div class="resource-label">内存使用率</div>
            <el-progress :percentage="systemInfo.memory || 0" :stroke-width="8" color="#818cf8" />
          </div>
          <div class="resource-item">
            <div class="resource-label">GPU 使用率</div>
            <el-progress :percentage="systemInfo.gpu || 0" :stroke-width="8" color="#22d3ee" />
          </div>
          <div class="resource-item">
            <div class="resource-label">磁盘使用率</div>
            <el-progress :percentage="systemInfo.disk || 0" :stroke-width="8" color="#34d399" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3>最近活动</h3></div>
        <div class="card-body">
          <div v-if="!activities.length" class="empty-text">暂无活动记录</div>
          <div v-for="act in activities" :key="act.id" class="activity-item">
            <div class="activity-dot" />
            <div class="activity-content">
              <div class="activity-text">{{ act.description }}</div>
              <div class="activity-time">{{ act.createdAt }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import { getAiServiceStatus } from '@/api/ai'
import { PictureFilled, View } from '@element-plus/icons-vue'

const stats = ref([
  { label: '用户总数', key: 'totalUsers', value: 0, icon: 'UserFilled', color: '#818cf8', bg: 'rgba(99,102,241,0.12)' },
  { label: '今日生成', key: 'todayImages', value: 0, icon: 'Picture', color: '#22d3ee', bg: 'rgba(34,211,238,0.1)' },
  { label: '活跃任务', key: 'runningTasks', value: 0, icon: 'Cpu', color: '#fbbf24', bg: 'rgba(251,191,36,0.1)' },
  { label: '风格数量', key: 'totalStyles', value: 0, icon: 'Box', color: '#34d399', bg: 'rgba(52,211,153,0.1)' }
])

const systemInfo = reactive({ cpu: 0, memory: 0, gpu: 0, disk: 0 })
const activities = ref([])
const aiStatus = reactive({ text2ImageProviders: {}, image2TextProviders: {} })

function getProviderDisplayName(name) {
  const map = {
    'pollinations': 'Pollinations.ai',
    'huggingface': 'HuggingFace',
    'stable-diffusion': 'Stable Diffusion',
    'doubao': '豆包视觉',
    'gemini': 'Google Gemini',
    'openrouter': 'OpenRouter',
    'siliconflow': '硅基流动',
    'zhipu': '智谱AI',
    'mock': '模拟模式'
  }
  return map[name] || name
}

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    const data = res.data
    stats.value.forEach(stat => {
      if (data[stat.key] !== undefined) {
        stat.value = data[stat.key]
      }
    })
  } catch {}
  try {
    const aiRes = await getAiServiceStatus()
    aiStatus.text2ImageProviders = aiRes.data.text2ImageProviders || {}
    aiStatus.image2TextProviders = aiRes.data.image2TextProviders || {}
  } catch {}
})
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-family: 'Sora', sans-serif;
  font-size: 26px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.provider-group-title {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ai-service-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
}

.ai-service-item:last-child {
  border-bottom: none;
}

.ai-service-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-service-name {
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.ai-service-msg {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
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
}

.card-body {
  padding: 20px;
}

.resource-item {
  margin-bottom: 18px;
}

.resource-item:last-child {
  margin-bottom: 0;
}

.resource-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.activity-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  margin-top: 6px;
  flex-shrink: 0;
}

.activity-text {
  font-size: 13px;
  line-height: 1.5;
}

.activity-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.empty-text {
  text-align: center;
  color: var(--text-muted);
  padding: 20px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr 1fr; }
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
