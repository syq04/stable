import request from '@/utils/request'

export function analyzeImage(formData) {
  return request.post('/image2text/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export function getImage2TextHistory(params) {
  return request.get('/image2text/history', { params })
}

export function getImage2TextDetail(id) {
  return request.get(`/image2text/${id}`)
}

export function updateImage2TextResult(id, data) {
  return request.put(`/image2text/${id}`, data)
}

export function deleteImage2TextRecord(id) {
  return request.delete(`/image2text/${id}`)
}
