import request from '@/utils/request'

export function generateImage(data) {
  return request.post('/text2image/generate', data)
}

export function getText2ImageHistory(params) {
  return request.get('/text2image/history', { params })
}

export function getText2ImageDetail(id) {
  return request.get(`/text2image/${id}`)
}

export function deleteText2ImageRecord(id) {
  return request.delete(`/text2image/${id}`)
}

export function getText2ImageModels() {
  return request.get('/text2image/models')
}

export function getText2ImageProgress(taskId) {
  return request.get(`/text2image/progress/${taskId}`)
}

export function evaluateImage(data) {
  return request.post('/text2image/evaluate', data)
}
