import request from '@/utils/request'

export function getStyleList(params) {
  return request.get('/styles', { params })
}

export function createStyle(data) {
  return request.post('/styles', data)
}

export function getStyleDetail(id) {
  return request.get(`/styles/${id}`)
}

export function updateStyle(id, data) {
  return request.put(`/styles/${id}`, data)
}

export function deleteStyle(id) {
  return request.delete(`/styles/${id}`)
}
