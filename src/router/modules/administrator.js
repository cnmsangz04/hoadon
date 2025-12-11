export default {
  path: '/administrator',
  component: () => import('../../views/administrators/page.vue'), // layout admin
  meta: { requiresAdmin: true, title: 'Khu vực admin' },
  children: [
    {
      path: '',
      name: 'home',
      component: () => import('../../views/administrators/index.vue'),
      meta: { requiresAdmin: true, title: 'Trang quản trị' }
    },
	{
	 path: 'bank/list',
	 name: 'admin-bank-list',
	 component: () => import('../../views/administrators/bank/list.vue'),
	 meta: { requiresAdmin: true, title: 'Ngân hàng' }
	},
	{
		 path: 'tax-authorities/list',
		 name: 'admin-bank-list',
		 component: () => import('../../views/administrators/tax-authorities/list.vue'),
		 meta: { requiresAdmin: true, title: 'Cơ quan thuế' }
	}
  ]
}
