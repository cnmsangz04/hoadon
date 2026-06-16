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
	},
	{
	  path: 'sessions/list',
	  name: 'setting-sessions',
	  component: () => import('../../views/settings/sessions/list.vue'),
	  meta: { requiresUser: true, title: 'Phiên đăng nhập' }
	},
	{
	  path: 'member/list',
	  name: 'setting-member',
	  component: () => import('../../views/settings/member/list.vue'),
	  meta: { requiresUser: true, title: 'Thành viên', rolePolicy: 'role<2' }
	},
	{
	  path: 'login-history/list',
	  name: 'setting-login-history',
	  component: () => import('../../views/settings/login-history/list.vue'),
	  meta: { requiresUser: true, title: 'Lịch sử đăng nhập', rolePolicy: 'role<2' }
	},
	{
	  path: 'security/ip',
	  name: 'setting-security-ip',
	  component: () => import('../../views/settings/security/ip.vue'),
	  meta: { requiresUser: true, title: 'Bảo mật bằng IP', rolePolicy: 'role<2' }
	}
  ]
}
