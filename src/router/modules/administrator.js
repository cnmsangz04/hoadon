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
    }
  ]
}
