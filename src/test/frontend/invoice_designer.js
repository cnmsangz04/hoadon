import Vue from 'vue'
import BootstrapVue from 'bootstrap-vue'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import Designer from '@/views/customers/form-invoice/designer.vue'

Vue.use(BootstrapVue)
Vue.prototype.$route = { path: '/form-invoice/designer', query: { formId: '1' } }
Vue.prototype.$router = { back() {}, push() {} }

new Vue({
  render: (h) => h(Designer),
  mounted() {
    window.invoiceDesigner = this.$children[0]
  }
}).$mount('#app')
