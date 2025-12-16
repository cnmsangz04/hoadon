import axios from 'axios'
import toastr from 'toastr'
import 'toastr/build/toastr.min.css'

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
	
    const message =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err.message ||
      'Lỗi hệ thống'

    // 401 – chưa đăng nhập / hết hạn
    if (status === 401) {
      toastr.warning(message || 'Phiên đăng nhập đã hết hạn')

      setTimeout(() => {
        try { localStorage.removeItem(key) } catch (_) {}
        window.location.href = admin ? '/auth/login-admin' : '/auth/login'
      }, 1200)

      return Promise.reject(err)
    }

    // 403 – không có quyền
    if (status === 403) {
      toastr.error(message || 'Bạn không có quyền thao tác')
      return Promise.reject(err)
    }

    // lỗi khác
    toastr.error(message)
    return Promise.reject(err)
  }
)

window.axios = axios
export default axios
