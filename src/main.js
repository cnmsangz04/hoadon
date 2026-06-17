import Vue from 'vue'
import App from './App.vue'
import router from './router'
import './plugins/axios'

import { BootstrapVue, IconsPlugin } from 'bootstrap-vue'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

import '@fortawesome/fontawesome-free/css/all.css';
import '@fortawesome/fontawesome-free/js/all.js';

import toastr from 'toastr'
import 'toastr/toastr.scss'

import 'vue-advanced-cropper/dist/style.css'

import i18n from './i18n'

import $ from 'jquery'
import 'jquery-confirm/dist/jquery-confirm.min.css'
import 'jquery-confirm/dist/jquery-confirm.min.js'

// Đăng ký vue-select dùng toàn cục
import 'vue-select/dist/vue-select.css'
import VSelect from 'vue-select'
Vue.component('v-select', VSelect)

window.$ = window.jQuery = $

Vue.prototype.$toastr = toastr

toastr.options = {
  closeButton: true,
  progressBar: true,
  positionClass: 'toast-top-right',
  timeOut: 3000,
  preventDuplicates: true
}

Vue.use(BootstrapVue)
Vue.use(IconsPlugin)

function bvToastContentToText(content) {
  if (Array.isArray(content)) {
    return content.map(item => bvToastContentToText(item)).filter(Boolean).join(' ')
  }
  if (content && typeof content === 'object') {
    if (content.text) return content.text
    if (content.children) return bvToastContentToText(content.children)
    return content.message || ''
  }
  return String(content || '')
}

function bvToastVariantToType(variant) {
  switch (variant) {
    case 'success': return 'success'
    case 'warning': return 'warning'
    case 'info': return 'info'
    default: return 'error'
  }
}

function bvToastTitle(type, title) {
  if (title) return title
  switch (type) {
    case 'success': return 'Thành công'
    case 'warning': return 'Cảnh báo'
    case 'info': return 'Thông báo'
    default: return 'Lỗi'
  }
}

// Chỉ dùng toastr cho thông báo. Các chỗ cũ còn gọi $bvToast.toast sẽ được chuyển qua toastr.
Vue.mixin({
  beforeCreate() {
    this._bv__toast = {
      toast(content, options = {}) {
        const message = bvToastContentToText(content)
        if (!message) return
        const type = bvToastVariantToType(options.variant)
        const title = bvToastTitle(type, options.title)
        if (toastr && typeof toastr[type] === 'function') {
          toastr[type](message, title)
        }
      },
      show() {},
      hide() {}
    }
  }
})

Vue.config.productionTip = false

// Store toàn cục gọn nhẹ, truy cập qua this.$app
Vue.prototype.$app = Vue.observable({
  info: {
    user: null,
    company: null
  }
})

// Tải thông tin khi mở app để các component cùng dùng (chỉ khi token hợp lệ và chưa hết hạn)
import axios from './plugins/axios'
;(function () {
  const token = localStorage.getItem('token') || localStorage.getItem('token-admin')
  if (!token) return
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload || (payload.exp && payload.exp < Math.floor(Date.now() / 1000))) return
  } catch { return }
  axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } }).then(res => {
    const data = res?.data || {}
    Vue.prototype.$app.info.user = data.user || null
    Vue.prototype.$app.info.company = data.company || null
    if (data.company && data.company.status != null) {
      localStorage.setItem('company-status', String(data.company.status))
    } else {
      localStorage.removeItem('company-status')
    }
  }).catch(() => {})
})()

new Vue({
  router,
  i18n,
  render: h => h(App)
}).$mount('#app')
