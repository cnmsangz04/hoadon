// src/router/modules/category.js
export default {
  path: '/categories',
  component: () => import('@/views/customers/page.vue'),
  meta: { requiresUser: true, title: 'Danh mục' },
  children: [
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
}
