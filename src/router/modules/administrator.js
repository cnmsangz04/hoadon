export default {
	path: '/administrator',
	component: () => import('../../views/administrators/page.vue'), // layout admin
	meta: { requiresAdmin: true, title: 'Khu vực admin' },
	children: [
		{
			path: '',
			name: 'admin',
			component: () => import('../../views/administrators/index.vue'),
			meta: { requiresAdmin: true, title: 'Danh sách công ty' }
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
			path: 'tax-authority/list',
			name: 'admin-tax-authority-list',
			component: () => import('../../views/administrators/tax-authority/list.vue'),
			meta: { requiresAdmin: true, title: 'Cơ quan thuế' }
		},
		{
			path: 'tax-rate/list',
			name: 'admin-tax-rate-list',
			component: () => import('../../views/administrators/tax-rate/list.vue'),
			meta: { requiresAdmin: true, title: 'Thuế suất' }
		},
		{
			path: 'access-control',
			name: 'admin-access-control',
			component: () => import('../../views/administrators/access-control/index.vue'),
			meta: { requiresAdmin: true, title: 'Quản lý phân quyền' },
			children: [
				{
					path: 'permissions/list',
					name: 'admin-permissions-list',
					component: () => import('../../views/administrators/access-control/permissions/list.vue'),
					meta: { requiresAdmin: true, title: 'Quyền' }
				},
				{
					path: 'permission-categories/list',
					name: 'admin-permission-categories-list',
					component: () => import('../../views/administrators/access-control/permission-categories/list.vue'),
					meta: { requiresAdmin: true, title: 'Nhóm quyền' }
				}
			]
		}
	]
}