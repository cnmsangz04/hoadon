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

Vue.prototype.$toastr = toastr

toastr.options = {
  closeButton: true,
  progressBar: true,
  positionClass: 'toast-top-right',
  timeOut: 3000
}

Vue.use(BootstrapVue)
Vue.use(IconsPlugin)

Vue.config.productionTip = false

// Global app store (lightweight) accessible via this.$app
Vue.prototype.$app = {
  info: {
    user: null,
    company: null
  }
}

// Fetch info on app load so all components can use it
import axios from './plugins/axios'
axios.get('/auth/info').then(res => {
  const data = res?.data || {}
  Vue.prototype.$app.info.user = data.user || null
  Vue.prototype.$app.info.company = data.company || null
}).catch(() => {})

new Vue({
  router,
  i18n,
  render: h => h(App)
}).$mount('#app')