import Vue from 'vue'
import VueRouter from 'vue-router'

import authRoute from "./modules/auth";
import administratorRoute from './modules/administrator';
import CustomerIndex from '@/views/customers/index.vue'
import AdministratorsIndex from '@/views/administrators/index.vue'

Vue.use(VueRouter)

const router = new VueRouter({
    mode: 'history',
    routes: [
        {
            path: '/',
            component: () => import('@/views/customers/page.vue'),
            meta: { requiresUser: true },
            children: [{ path: '', name: '/', component: CustomerIndex }]
        },
        authRoute, administratorRoute,
        { path: '*', redirect: '/auth/login' }
    ]
})


router.beforeEach((to, from, next) => {

  const tokenUser = localStorage.getItem('token')
  const tokenAdmin = localStorage.getItem('token-admin')

  const needUser = to.matched.some(r => r.meta.requiresUser)
  const needAdmin = to.matched.some(r => r.meta.requiresAdmin)
  const guestUser = to.matched.some(r => r.meta.guestUser)
  const guestAdmin = to.matched.some(r => r.meta.guestAdmin)

  if (needUser && !tokenUser) return next('/auth/login')
  if (tokenUser && guestUser) return  next('/')
  if (needAdmin && !tokenAdmin) return next('/auth/login-admin')
  if (tokenAdmin && guestAdmin) return next('/administrator')

	console.log(1);
  next()
})


export default router
