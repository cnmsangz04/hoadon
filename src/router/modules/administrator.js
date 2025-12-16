export default {
	path: '/administrator',
	component: () => import('../../views/administrators/page.vue'), // layout admin
	meta: { requiresAdmin: true, title: 'Khu vực admin' },
	children: [
		{
			path: '',
			name: 'admin',
			component: () => import('../../views/administrators/index.vue'),
			meta: { requiresAdmin: true, title: 'Trang quản trị' }
		},
		{
			path: 'company/list',
			name: 'admin-company-list',
			component: () => import('../../views/administrators/company/list.vue'),
			meta: { requiresAdmin: true, title: 'Danh sách công ty' }
		},
		{
			path: 'buy-invoice/list',
			name: 'admin-buy-invoice-list',
			component: () => import('../../views/administrators/buy-invoice/list.vue'),
			meta: { requiresAdmin: true, title: 'Mua hóa đơn' }
		},
		{
			path: 'bank/list',
			name: 'admin-bank-list',
			component: () => import('../../views/administrators/bank/list.vue'),
			meta: { requiresAdmin: true, title: 'Ngân hàng' }
		},
		{
			path: 'tax-authorities/list',
			name: 'admin-tax-authorities-list',
			component: () => import('../../views/administrators/tax-authorities/list.vue'),
			meta: { requiresAdmin: true, title: 'Cơ quan thuế' }
		}
	]
}
