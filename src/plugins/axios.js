import axios from 'axios'
import toastr from 'toastr'
import 'toastr/build/toastr.min.css'
import { toastError, toastWarning, toastSuccess } from '@/utils/toast'

axios.defaults.baseURL = process.env.VUE_APP_API_BASE_URL || '/v1'

function isAdminContext() {
  try {
    const p = window.location?.pathname || ''
    return /administrator|admin/.test(p)
  } catch {
    return false
  }
}

// Các endpoint không bao giờ gửi kèm token (route xác thực công khai)
const PUBLIC_PATHS = ['/auth/login', '/auth/register', '/auth/forgot-password', '/auth/reset-password', '/public/']

axios.interceptors.request.use(config => {
  try {
    // Bỏ tiền tố baseURL ở đầu để so sánh theo đường dẫn tương đối
    const relUrl = (config.url || '').replace(/^\/v1/, '')
    const isPublic = PUBLIC_PATHS.some(p => relUrl.startsWith(p))

    if (!isPublic) {
      const admin = isAdminContext()
      const primaryKey = admin ? 'token-admin' : 'token'
      let token = localStorage.getItem(primaryKey)

      // Dự phòng: nếu thiếu token chính thì thử khóa token còn lại
      if (!token) {
        const altKey = admin ? 'token' : 'token-admin'
        token = localStorage.getItem(altKey)
      }

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }

    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

  } catch {}
  return config
}, err => Promise.reject(err))

// Xử lý phản hồi thành công, có thể hiện toast theo cấu hình request
axios.interceptors.response.use(
  res => {
    try {
      const cfg = res?.config || {}
      const method = (cfg.method || '').toUpperCase()
      // Đọc các khóa thông báo thành công tùy chọn từ config
      const msg = cfg.successMessage || cfg.successText || cfg?.meta?.successMessage
      // Mặc định: chỉ hiện với các method thay đổi dữ liệu khi có thông báo
      if (msg && ['POST','PUT','PATCH','DELETE'].includes(method)) {
        toastSuccess(msg)
      }
    } catch {}
    return res
  },
  err => {
    const status = err?.response?.status
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'

    const cfg = err?.config || {}
    const suppressGlobal = cfg?.meta?.suppressGlobalErrorToast === true

    const message =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err.message ||
      'Lỗi hệ thống'

    if (status === 401) {
      localStorage.removeItem(key)
      if (!suppressGlobal) toastWarning(message || 'Phiên đăng nhập đã hết hạn', 'HTTP_401')
      setTimeout(() => {
        window.location.href = admin ? '/auth/login-admin' : '/auth/login'
      }, 1200)
      return Promise.reject(err)
    }

    if (status === 403) {
      if (!suppressGlobal) toastError(message || 'Bạn không có quyền thao tác', 'HTTP_403')
      setTimeout(() => {
        window.location.href = admin ? '/administrator' : '/'
      }, 1200)
      return Promise.reject(err)
    }

    if (!suppressGlobal) toastError(message, `HTTP_${status || 'ERR'}`)
    return Promise.reject(err)
  }
)

window.axios = axios
export default axios
