import Vue from 'vue'
import VueRouter from 'vue-router'

import authRoute from "./modules/auth";
import settingRoute from './modules/setting';
import administratorRoute from './modules/administrator';
import CustomerIndex from '@/views/customers/index.vue'
import SettingsIndex from '@/views/settings/index.vue'
import AdministratorsIndex from '@/views/administrators/index.vue'
import { parseJwt } from '@/utils/jwt'

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
        authRoute, settingRoute, administratorRoute,
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
  const rolePolicy = to.matched.find(r => r.meta && r.meta.rolePolicy)?.meta?.rolePolicy

  if (needUser && !tokenUser) return next('/auth/login')
  if (tokenUser && guestUser) return  next('/')

  // Stricter admin access: require valid admin token with role 0 (Root) OR role 1 (System Admin with adminType==1)
  if (needAdmin) {
    if (!tokenAdmin) return next('/auth/login-admin')
    const payload = parseJwt(tokenAdmin)
    // Check token expiry if available
    const nowSec = Math.floor(Date.now() / 1000)
    if (payload && typeof payload.exp === 'number' && payload.exp <= nowSec) {
      try { localStorage.removeItem('token-admin') } catch (e) {}
      return next('/auth/login-admin')
    }
    const roleNum = payload && typeof payload.role !== 'undefined' ? Number(payload.role) : NaN
    const adminType = payload && typeof payload.adminType !== 'undefined' ? Number(payload.adminType) : undefined
    const isRoot = !Number.isNaN(roleNum) && roleNum === 0
    const isSystemAdmin = !Number.isNaN(roleNum) && roleNum === 1 && adminType === 1
    if (!isRoot && !isSystemAdmin) {
      // Invalid token or Admin company/user -> purge and redirect to admin login
      try { localStorage.removeItem('token-admin') } catch (e) {}
      return next('/auth/login-admin')
    }
  }

  // Enforce optional rolePolicy for user routes (e.g., member management requires role < 2)
  if (rolePolicy) {
    const token = tokenUser || tokenAdmin
    const payload = parseJwt(token)
    const roleNum = payload && typeof payload.role !== 'undefined' ? Number(payload.role) : NaN
    if (rolePolicy === 'role<2') {
      if (Number.isNaN(roleNum) || roleNum >= 2) {
        return next('/')
      }
    }
  }

  if (tokenAdmin && guestAdmin) return next('/administrator')

  next()
})


export default router