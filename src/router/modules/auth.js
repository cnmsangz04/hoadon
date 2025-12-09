export default {
  path: '/auth',
  component: () => import('../../views/auth/page.vue'),
  children: [
    {
      path: 'login',
      name: 'login',
      component: () => import('../../views/auth/login.vue'),
      meta: { guestUser: true }
    },
    {
      path: 'login-admin',
      name: 'login_admin',
      component: () => import('../../views/auth/login_admin.vue'),
      meta: { guestAdmin: true }
    }
  ]
}
