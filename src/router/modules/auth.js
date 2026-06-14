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
    },
    {
      path: 'forgot-password',
      name: 'auth-forgot-password',
      component: () => import('../../views/auth/forgot_password.vue'),
      meta: { guestUser: true }
    },
    {
      path: 'register',
      name: 'auth-register',
      component: () => import('../../views/auth/register.vue'),
      meta: { guestUser: true }
    },
    {
      path: 'reset-password/:token',
      name: 'auth-reset-password',
      component: () => import('../../views/auth/reset_password.vue'),
      meta: { guestUser: true }
    }
  ]
}
