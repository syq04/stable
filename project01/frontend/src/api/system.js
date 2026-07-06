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
