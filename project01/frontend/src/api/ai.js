import request from '@/utils/request'

export function getAiServiceStatus() {
  return request.get('/ai/status')
}

export function getAiProviders() {
  return request.get('/ai/providers')
}

export function switchText2ImageProvider(providerName) {
  return request.put(`/ai/switch/text2image/${providerName}`)
}

export function switchImage2TextProvider(providerName) {
  return request.put(`/ai/switch/image2text/${providerName}`)
}
