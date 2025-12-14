import axios from 'axios'

// Thay đổi baseURL theo backend của bạn
// Sử dụng '/v1' mặc định để tận dụng proxy của devServer và tránh CORS khi phát triển
axios.defaults.baseURL = process.env.VUE_APP_API_BASE_URL || '/v1'

// Helper: xác định ngữ cảnh admin dựa trên URL hiện tại
function isAdminContext() {
  try {
    const p = window.location?.pathname || ''
    // nếu URL chứa "administrator" hoặc "admin" thì coi là ngữ cảnh admin
    return /administrator|admin/.test(p)
  } catch (e) {
    return false
  }
}

// Gắn token tự động từ localStorage
axios.interceptors.request.use(config => {
  try {
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'
    const token = localStorage.getItem(key)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  } catch (e) {
    // ignore
  }
  return config
}, err => Promise.reject(err))

axios.interceptors.response.use(res => res, err => {
  const status = err?.response?.status
  if (status === 401 || status === 403) {
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'
    try {
      localStorage.removeItem(key)
    } catch (_) {}
    // Nếu là 401: chưa đăng nhập hoặc token hết hạn -> chuyển về login
    // Nếu là 403: không có quyền hoặc token sai phạm -> cũng quay về login admin/user tương ứng
    const target = admin ? '/auth/login-admin' : '/auth/login'
    if (typeof window !== 'undefined') {
      window.location.href = target
    }
  }
  return Promise.reject(err)
})

window.axios = axios

export default axios