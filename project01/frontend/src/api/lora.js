import request from '@/utils/request'

/**
 * 创建 LoRA 训练任务
 */
export function createLoraTask(data) {
  return request.post('/lora/tasks', data)
}

/**
 * 获取 LoRA 训练任务列表
 */
export function getLoraTaskList(params) {
  return request.get('/lora/tasks', { params })
}

/**
 * 启动 LoRA 训练任务
 */
export function startLoraTask(id) {
  return request.post(`/lora/tasks/${id}/start`)
}

/**
 * 获取训练任务日志
 */
export function getLoraTaskLogs(id) {
  return request.get(`/lora/tasks/${id}/logs`)
}

/**
 * 下载训练好的模型
 */
export function downloadLoraModel(id) {
  return request.get(`/lora/tasks/${id}/download`, { responseType: 'blob' })
}

/**
 * 删除训练任务
 */
export function deleteLoraTask(id) {
  return request.delete(`/lora/tasks/${id}`)
}

/**
 * 获取 LoRA 模型列表（系统模型）
 */
export function getLoraModels(params) {
  return request.get('/system/models', { params })
}
