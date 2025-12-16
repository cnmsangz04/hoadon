import axios from 'axios'
import toastr from 'toastr'
import 'toastr/toastr.scss'

// Base URL
axios.defaults.baseURL = process.env.VUE_APP_API_BASE_URL || '/v1'
axios.defaults.headers.post['Content-Type'] = 'application/json'

// Helper xác định ngữ cảnh admin
function isAdminContext() {
  try {
    const p = window.location?.pathname || ''
    return /administrator|admin/.test(p)
  } catch (e) {
    return false
  }
}

// Request interceptor: gắn token
axios.interceptors.request.use(config => {
  try {
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'
    const token = localStorage.getItem(key)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  } catch (e) {}
  return config
}, err => Promise.reject(err))

// Response interceptor: xử lý lỗi
axios.interceptors.response.use(
  res => res,
  err => {
    const status = err?.response?.status
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'

    if (status === 401 || status === 403) {
      try { localStorage.removeItem(key) } catch (_) {}
      const target = admin ? '/auth/login-admin' : '/auth/login'
      if (typeof window !== 'undefined') window.location.href = target
      return Promise.reject(err)
    }

    // Hiển thị toastr lỗi tự động
    const msg = err?.response?.data?.message || err.message || 'Lỗi API'
    toastr.error(msg)
    return Promise.reject(err)
  }
)

window.axios = axios
export default axios
