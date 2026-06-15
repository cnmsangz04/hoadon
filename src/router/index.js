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
					path: '/invoice/vat-invoice/list',
					name: 'CustomerVatInvoiceList',
					component: () => import('@/views/customers/invoices/vat-invoice/list.vue'),
					meta: { requiresUser: true, title: 'Danh sách hóa đơn GTGT' }
				},
				{
					path: '/invoice-packages',
					name: 'CustomerInvoicePackages',
					component: () => import('@/views/customers/invoice-packages/index.vue'),
					meta: { requiresUser: true, title: 'Gói hóa đơn' }
				},
				{
					path: '/invoice/create',
					name: 'CustomerVatInvoiceCreate',
					component: () => import('@/views/customers/invoices/vat-invoice/create.vue'),
					meta: { requiresUser: true, title: 'Lập hóa đơn GTGT' }
				},
				{
					path: '/invoice/replace',
					name: 'CustomerVatInvoiceReplace',
					component: () => import('@/views/customers/invoices/vat-invoice/process.vue'),
					meta: { requiresUser: true, title: 'Hóa đơn thay thế', processingMode: 'replace' }
				},
				{
					path: '/invoice/adjust',
					name: 'CustomerVatInvoiceAdjust',
					component: () => import('@/views/customers/invoices/vat-invoice/process.vue'),
					meta: { requiresUser: true, title: 'Hóa đơn điều chỉnh', processingMode: 'adjust' }
				},
				{
					path: '/invoice/:id/edit',
					name: 'CustomerVatInvoiceEdit',
					component: () => import('@/views/customers/invoices/vat-invoice/create.vue'),
					meta: { requiresUser: true, title: 'Cập nhật hóa đơn GTGT' }
				},
				{
					path: '/imports/invoice',
					name: 'CustomerInvoiceImport',
					component: () => import('@/views/customers/imports/invoice/index.vue'),
					meta: { requiresUser: true, title: 'Import hóa đơn' }
				},
				{
					path: '/categories/product/list',
					name: 'category-product-list',
					component: () => import('@/views/customers/categories/product/list.vue'),
					meta: { requiresUser: true, title: 'Danh mục sản phẩm' }
				},
				{
					path: '/categories/customer/list',
					name: 'category-customer-list',
					component: () => import('@/views/customers/categories/customer/list.vue'),
					meta: { requiresUser: true, title: 'Danh mục khách hàng' }
				},
				{
					path: '/reports/invoice/list',
					name: 'CustomerReportInvoiceList',
					component: () => import('@/views/customers/reports/invoice/list.vue'),
					meta: { requiresUser: true, title: 'Báo cáo hóa đơn' }
				},
				{
					path: '/email/mail-server',
					name: 'EmailMailServer',
					component: () => import('@/views/customers/email/mail-server.vue'),
					meta: { requiresUser: true, title: 'Máy chủ gửi mail' }
				},
				{
					path: '/email/mail-history',
					name: 'EmailMailHistory',
					component: () => import('@/views/customers/email/mail-history.vue'),
					meta: { requiresUser: true, title: 'Lịch sử gửi mail' }
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

	// Hàm hỗ trợ kiểm tra token admin (role 0 Root hoặc role 1 Admin)
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

	// Quyền admin: yêu cầu token admin hợp lệ với role 0 (Root) hoặc role 1 (Admin)
	if (needAdmin) {
		const check = validateAdminToken(tokenAdmin)
		if (!check.valid) {
			try { localStorage.removeItem('token-admin') } catch (e) { }
			return next('/auth/login-admin')
		}
	}

	// Áp dụng rolePolicy tùy chọn cho route user (ví dụ quản lý thành viên cần role < 2)
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

	// Chuyển admin đã đăng nhập khỏi trang login admin chỉ khi token hợp lệ
	if (guestAdmin) {
		const check = validateAdminToken(tokenAdmin)
		if (check.valid) return next('/administrator')
		// Nếu có token không hợp lệ thì xóa và ở lại login-admin
		if (tokenAdmin && !check.valid) { try { localStorage.removeItem('token-admin') } catch (e) { } }
	}

	next()
})


export default router
