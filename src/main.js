import Vue from 'vue';
import BootstrapVue from 'bootstrap-vue';
import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import ListInvoices from './views/customers/invoices/list.vue';

Vue.use(BootstrapVue);

new Vue({
  render: h => h(ListInvoices)
}).$mount('#app');
