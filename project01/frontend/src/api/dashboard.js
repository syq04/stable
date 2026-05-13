import request from '@/utils/request'

export function getDashboardStats() {
  return request.get('/system/dashboard/stats')
}

export function getActiveStyles() {
  return request.get('/styles/active')
}
