export default {
  path: "/administrator",
  component: () => import("../../views/administrators/page.vue"), // layout admin
  meta: { requiresAdmin: true, title: "Khu vực admin" },
  children: [
    {
      path: "",
      name: "admin",
      component: () => import("../../views/administrators/index.vue"),
      meta: { requiresAdmin: true, title: "Danh sách công ty" },
    },
    {
      path: "company/list",
      name: "admin-company-list",
      component: () => import("../../views/administrators/company/list.vue"),
      meta: { requiresAdmin: true, title: "Danh sách công ty" },
    },
    {
      path: "company-registration/list",
      name: "admin-company-registration-list",
      component: () => import("../../views/administrators/company-registration/list.vue"),
      meta: { requiresAdmin: true, title: "Duyệt đăng ký" },
    },
    {
      path: "buy-invoice/list",
      name: "admin-buy-invoice-list",
      component: () =>
        import("../../views/administrators/buy-invoice/list.vue"),
      meta: { requiresAdmin: true, title: "Mua hóa đơn" },
    },
    {
      path: "bank/list",
      name: "admin-bank-list",
      component: () => import("../../views/administrators/bank/list.vue"),
      meta: { requiresAdmin: true, title: "Ngân hàng" },
    },
    {
      path: "tax-authority/list",
      name: "admin-tax-authority-list",
      component: () =>
        import("../../views/administrators/tax-authority/list.vue"),
      meta: { requiresAdmin: true, title: "Cơ quan thuế" },
    },
    {
      path: "vat-rate/list",
      name: "admin-vat-rate-list",
      component: () => import("../../views/administrators/vat-rate/list.vue"),
      meta: { requiresAdmin: true, title: "Thuế suất" },
    },
    {
      path: "email-template/list",
      name: "admin-email-template-list",
      component: () =>
        import("../../views/administrators/email-template/list.vue"),
      meta: { requiresAdmin: true, title: "Mail Template" },
    },
    {
      path: "email-template/create",
      name: "admin-email-template-create",
      component: () =>
        import("../../views/administrators/email-template/create.vue"),
      meta: { requiresAdmin: true, title: "Add Template" },
    },
    {
      path: "email/mail-history",
      name: "admin-email-mail-history",
      component: () =>
        import("../../views/administrators/email/mail-history.vue"),
      meta: { requiresAdmin: true, title: "Lịch sử gửi mail" },
    },
    {
      path: "form-invoice/list",
      name: "admin-form-invoice-list",
      component: () =>
        import("../../views/administrators/form-invoice/list.vue"),
      meta: { requiresAdmin: true, title: "Mẫu hóa đơn" },
    },
    {
      path: "form-invoice/create",
      name: "admin-form-invoice-create",
      component: () =>
        import("../../views/administrators/form-invoice/create.vue"),
      meta: { requiresAdmin: true, title: "Tạo mẫu hóa đơn" },
    },
    {
      path: "form-invoice/:id/edit",
      name: "admin-form-invoice-edit",
      component: () =>
        import("../../views/administrators/form-invoice/create.vue"),
      meta: { requiresAdmin: true, title: "Cập nhật mẫu hóa đơn" },
    },
    {
      path: "access-control",
      name: "admin-access-control",
      component: () =>
        import("../../views/administrators/access-control/index.vue"),
      meta: { requiresAdmin: true, title: "Quản lý phân quyền" },
      children: [
        {
          path: "permissions/list",
          name: "admin-permissions-list",
          component: () =>
            import(
              "../../views/administrators/access-control/permissions/list.vue"
            ),
          meta: { requiresAdmin: true, title: "Quyền" },
        },
        {
          path: "permission-categories/list",
          name: "admin-permission-categories-list",
          component: () =>
            import(
              "../../views/administrators/access-control/permission-categories/list.vue"
            ),
          meta: { requiresAdmin: true, title: "Nhóm quyền" },
        },
      ],
    },
  ],
};
