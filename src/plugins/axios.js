import axios from 'axios'
import toastr from 'toastr'
import 'toastr/build/toastr.min.css'

axios.defaults.baseURL = process.env.VUE_APP_API_BASE_URL || '/v1'

function isAdminContext() {
  try {
    const p = window.location?.pathname || ''
    return /administrator|admin/.test(p)
  } catch {
    return false
  }
}

axios.interceptors.request.use(config => {
  try {
    const admin = isAdminContext()
    const primaryKey = admin ? 'token-admin' : 'token'
    let token = localStorage.getItem(primaryKey)

    // Fallback: if primary token missing, try the alternate token key
    if (!token) {
      const altKey = admin ? 'token' : 'token-admin'
      token = localStorage.getItem(altKey)
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

  } catch {}
  return config
}, err => Promise.reject(err))

// Success handler with optional success toast via request config
axios.interceptors.response.use(
  res => {
    try {
      const cfg = res?.config || {}
      const method = (cfg.method || '').toUpperCase()
      // Read optional success message keys from config
      const msg = cfg.successMessage || cfg.successText || cfg?.meta?.successMessage
      // Default: only show for mutating methods when message provided
      if (msg && ['POST','PUT','PATCH','DELETE'].includes(method)) {
        toastr.success(msg)
      }
    } catch {}
    return res
  },
  err => {
    const status = err?.response?.status
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'

    const message =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err.message ||
      'Lỗi hệ thống'

    if (status === 401) {
      toastr.warning(message || 'Phiên đăng nhập đã hết hạn')
      setTimeout(() => {
        localStorage.removeItem(key)
        window.location.href = admin ? '/auth/login-admin' : '/auth/login'
      }, 1200)
      return Promise.reject(err)
    }

    if (status === 403) {
      toastr.error(message || 'Bạn không có quyền thao tác')
      return Promise.reject(err)
    }

    toastr.error(message)
    return Promise.reject(err)
  }
)

window.axios = axios
export default axios