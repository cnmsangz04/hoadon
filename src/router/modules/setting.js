export default {
  path: '/setting',
  component: () => import('../../views/settings/page.vue'),
  meta: { requiresUser: true, title: 'Cấu hình' },
  children: [
    {
      path: '',
      redirect: 'account/list'
    },
    {
      path: 'account/list',
      name: 'setting-account',
      component: () => import('../../views/settings/account/list.vue'),
      meta: { requiresUser: true, title: 'Tài khoản' }
    },
	{
	  path: 'profile/list',
	  name: 'setting-profile',
	  component: () => import('../../views/settings/profile/list.vue'),
	  meta: { requiresUser: true, title: 'Hồ sơ' }
	}
  ]
}
