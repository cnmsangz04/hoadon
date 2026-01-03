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
			children: [
				{ path: '', name: '/', component: CustomerIndex },
				{
					path: '/register/invoice/list',
					name: 'registers-invoice',
					component: () => import('@/views/customers/registers/invoice/list.vue'),
					meta: {
						requiresUser: true,
						title: 'Danh sách tờ khai hóa đơn điện tử'
					}
				},
				{
					path: '/register/invoice/create',
					name: 'CustomerRegisterInvoiceCreate',
					component: () => import('@/views/customers/registers/invoice/create.vue'),
					meta: {
						requiresUser: true,
						title: 'Tạo tờ khai hóa đơn điện tử'
					}
				},
				{
					path: '/register/invoice/:id/edit',
					name: 'CustomerRegisterInvoiceEdit',
					component: () => import('@/views/customers/registers/invoice/create.vue'),
					meta: {
						requiresUser: true,
						title: 'Cập nhật tờ khai hóa đơn điện tử'
					}
				},
				{
					path: '/form-invoice/list',
					name: 'CustomerFormInvoiceList',
					component: () => import('@/views/customers/form-invoice/list.vue'),
					meta: { requiresUser: true, title: 'Danh sách mẫu hóa đơn' }
				},
				{
					path: '/form-invoice/template',
					name: 'CustomerFormInvoiceTemplate',
					component: () => import('@/views/customers/form-invoice/template.vue'),
					meta: { requiresUser: true, title: 'Chọn mẫu hóa đơn' }
				},
				{
					path: '/form-invoice/create',
					name: 'CustomerFormInvoiceCreate',
					component: () => import('@/views/customers/form-invoice/create.vue'),
					meta: { requiresUser: true, title: 'Thêm mẫu hóa đơn' }
				},
				{
					path: '/form-invoice/:id/edit',
					name: 'CustomerFormInvoiceEdit',
					component: () => import('@/views/customers/form-invoice/create.vue'),
					meta: { requiresUser: true, title: 'Cập nhật mẫu hóa đơn' }
				},
				{
					path: '/form-invoice/:id/view',
					name: 'CustomerFormInvoiceView',
					component: () => import('@/views/customers/form-invoice/create.vue'),
					meta: { requiresUser: true, title: 'Xem mẫu hóa đơn' }
				},
				{
					path: 'product/list',
					name: 'category-product-list',
					component: () => import('@/views/customers/categories/product/list.vue'),
					meta: { requiresUser: true, title: 'Danh mục sản phẩm' }
				},
				{
					path: 'customer/list',
					name: 'category-customer-list',
					component: () => import('@/views/customers/categories/customer/list.vue'),
					meta: { requiresUser: true, title: 'Danh mục khách hàng' }
				}
			]
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
	if (tokenUser && guestUser) return next('/')

	// Helper: validate admin token (role 0 Root or role 1 Admin)
	function validateAdminToken(rawToken) {
		if (!rawToken) return { valid: false, reason: 'missing' }
		let payload
		try { payload = parseJwt(rawToken) } catch (e) { return { valid: false, reason: 'parse' } }
		const nowSec = Math.floor(Date.now() / 1000)
		if (payload && typeof payload.exp === 'number' && payload.exp <= nowSec) return { valid: false, reason: 'expired' }
		const roleNum = payload && typeof payload.role !== 'undefined' ? Number(payload.role) : NaN
		const isRoot = !Number.isNaN(roleNum) && roleNum === 0
		const isAdmin = !Number.isNaN(roleNum) && roleNum === 1
		return { valid: isRoot || isAdmin, isRoot, isAdmin }
	}

	// Admin access: require valid admin token with role 0 (Root) OR role 1 (Admin)
	if (needAdmin) {
		const check = validateAdminToken(tokenAdmin)
		if (!check.valid) {
			try { localStorage.removeItem('token-admin') } catch (e) { }
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

	// Redirect logged-in admins away from admin login only when token is valid
	if (guestAdmin) {
		const check = validateAdminToken(tokenAdmin)
		if (check.valid) return next('/administrator')
		// If invalid token present, purge and stay on login-admin
		if (tokenAdmin && !check.valid) { try { localStorage.removeItem('token-admin') } catch (e) { } }
	}

	next()
})


export default router