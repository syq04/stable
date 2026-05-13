import request from '@/utils/request'

/**
 * 上传图片
 * @param {File} file - 图片文件
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
