import axios from 'axios'

// Thay đổi baseURL theo backend của bạn
axios.defaults.baseURL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'

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
  if (err.response && err.response.status === 401) {
    const admin = isAdminContext()
    const key = admin ? 'token-admin' : 'token'
    localStorage.removeItem(key)
    // chuyển hướng tới trang đăng nhập tương ứng
    window.location.href = admin ? '/auth/login-admin' : '/auth/login'
  }
  return Promise.reject(err)
})

window.axios = axios

export default axios