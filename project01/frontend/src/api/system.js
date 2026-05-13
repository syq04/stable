import request from '@/utils/request'

export function getSystemConfigs() {
  return request.get('/system/configs')
}

export function getSystemConfig(key) {
  return request.get(`/system/configs/${key}`)
}

export function updateSystemConfig(key, data) {
  return request.put(`/system/configs/${key}`, data)
}

export function getSystemLogs(params) {
  return request.get('/system/logs', { params })
}

export function exportSystemLogs(data) {
  return request.post('/system/logs/export', null, {
    params: data,
    responseType: 'blob'
  })
}

export function uploadModel(formData) {
  return request.post('/system/models/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

export function getModelList(params) {
  return request.get('/system/models', { params })
}

export function deleteModel(id) {
  return request.delete(`/system/models/${id}`)
}
