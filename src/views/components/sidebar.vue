<template>
  <aside class="sidebar" aria-label="Main sidebar">
    <div class="sidebar-inner">
      <!-- Thương hiệu / biểu trưng -->
      <div class="brand">
        <router-link class="brand-link" to="/">
          <img class="logo" :src="logoSrc" alt="logo" />
        </router-link>
      </div>

      <div v-if="isCompanyPending" class="pending-company">
        Tài khoản công ty đang chờ kích hoạt. Vui lòng mua gói hóa đơn để kích hoạt.
      </div>

      <div v-else class="search">
        <input v-model="search" type="search" placeholder="Tìm kiếm..." aria-label="Tìm kiếm menu" />
        <i class="fas fa-search"></i>
      </div>

      <!-- Trình đơn -->
      <nav class="menu" role="navigation">
        <ul>
          <li v-for="(item, index) in filteredItems" :key="index"
              :class="{ 'has-children': !!item.children, open: isOpen === index, active: isActive(item) }">

            <!-- Mục có menu con -->
            <template v-if="item.children">
              <a href="#" class="menu-item" @click.prevent="toggle(index)" :aria-expanded="isOpen === index">
                <i :class="item.icon"></i>
                <span class="label">{{ item.title }}</span>
                <i class="chev fas fa-chevron-down" :class="{ rotated: isOpen === index }" aria-hidden="true"></i>
              </a>

              <transition name="slide">
                <ul class="sub" v-show="isOpen === index">
                  <li v-for="(c, ci) in item.children" :key="ci" :class="{ active: isActive(c) }">
                    <router-link :to="c.to">
                      <i v-if="c.icon" :class="c.icon"></i>
                      <span>{{ c.title }}</span>
                    </router-link>
                  </li>
                </ul>
              </transition>
            </template>

            <!-- Link đơn -->
            <template v-else>
              <router-link class="menu-item" :to="item.to">
                <i :class="item.icon"></i>
                <span class="label">{{ item.title }}</span>
              </router-link>
            </template>

          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">v1.0</div>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'Sidebar',
  data() {
    return {
      isOpen: null,
      search: '',
      menuItems: [
        { title: 'Trang chủ', icon: 'fas fa-home', to: '/' },
        { title: 'Gói hóa đơn', icon: 'fas fa-box-open', to: '/invoice-packages', pendingAllowed: true },
        {
          title: 'Đăng ký phát hành',
          icon: 'fas fa-file-signature',
          children: [
            { title: 'Tờ khai hóa đơn điện tử', icon: 'fas fa-file-alt', to: '/register/invoice/list' },
          ],
        },
        { title: 'Mẫu hóa đơn', icon: 'fas fa-file-contract', to: '/form-invoice/list' },
        {
          title: 'Hóa đơn',
          icon: 'fas fa-file-invoice-dollar',
          children: [
            { title: 'Hóa đơn GTGT', icon: 'fas fa-receipt', to: '/invoice/vat-invoice/list' },
          ],
        },
        {
          title: 'Import dữ liệu',
          icon: 'fas fa-file-import',
          children: [
            { title: 'Import hóa đơn', icon: 'fas fa-file-excel', to: '/imports/invoice' },
          ],
        },
        {
          title: 'Xử lý hóa đơn',
          icon: 'fas fa-tasks',
          children: [
            { title: 'Hóa đơn thay thế', icon: 'fas fa-exchange-alt', to: '/invoice/replace' },
            { title: 'Hóa đơn điều chỉnh', icon: 'fas fa-sliders-h', to: '/invoice/adjust' },
          ],
        },
        { title: 'Báo cáo', icon: 'fas fa-chart-bar', to: '/reports/invoice/list' },
        {
          title: 'Danh mục',
          icon: 'fas fa-th-list',
          children: [
            { title: 'Sản phẩm', icon: 'fas fa-box', to: '/categories/product/list' },
            { title: 'Khách hàng', icon: 'fas fa-users', to: '/categories/customer/list' },
          ],
        },
        {
          title: 'Email gửi hóa đơn',
          icon: 'fas fa-envelope',
          children: [
            { title: 'Máy chủ gửi mail', icon: 'fas fa-server', to: '/email/mail-server' },
            { title: 'Lịch sử gửi mail', icon: 'fas fa-history', to: '/email/mail-history' },
          ],
        },
        { title: 'Cài đặt', icon: 'fas fa-cog', to: '/setting' },
      ],
    };
  },
  computed: {
    filteredItems() {
      const source = this.isCompanyPending
        ? this.menuItems.filter(item => item.pendingAllowed)
        : this.menuItems;
      const q = (this.search || '').trim().toLowerCase();
      if (!q) return source;
      return source
        .map((item) => {
          if (!item.children) {
            return item.title.toLowerCase().indexOf(q) !== -1 ? item : null;
          }
          const matchedChildren = item.children.filter((c) => c.title.toLowerCase().indexOf(q) !== -1);
          if (item.title.toLowerCase().indexOf(q) !== -1) return item;
          return matchedChildren.length ? { ...item, children: matchedChildren } : null;
        })
        .filter(Boolean);
    },
    logoSrc() {
      try {
        const logo = this.$app?.info?.company?.logo
        if (logo && typeof logo === 'string' && logo.trim() !== '') return logo
      } catch {}
      return require('@/assets/images/logo/logo-hoadon.png')
    },
    isCompanyPending() {
      const status = this.$app?.info?.company?.status ?? localStorage.getItem('company-status')
      return String(status) === '2'
    }
  },
  watch: {
    // Theo dõi thay đổi thông tin công ty để cập nhật logo
    '$app.info.company.logo': {
      handler() {
        this.$forceUpdate()
      },
      deep: true
    }
  },
  methods: {
    toggle(index) {
      this.isOpen = this.isOpen === index ? null : index;
    },
		
    isActive(item) {
      if (!item) return false;

      // Item không có con
      if (item.to) return this.$route.path === item.to;

      // Item có children => active nếu child match chính xác
      if (item.children) {
        return item.children.some(c => this.$route.path === c.to);
      }
      return false;
    }
  },
  mounted() {
    // Mở menu cha nếu route con đang active
	this.menuItems.forEach((item, idx) => {
	   if (item.children && this.isActive(item)) {
	      this.isOpen = idx;
	   }
	 });
  },
};
</script>

<style scoped>
/* Nền tảng sidebar */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #0f1724 0%, #0b2238 100%);
  color: #e6eef8;
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(2,6,23,0.6);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;
}
.sidebar-inner {
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.brand { text-align: center; margin-bottom: 12px; }
.brand-link { display: inline-block; }
.logo { max-width: 160px; height: auto; display: block; margin: 0 auto; }
.profile { padding: 8px 6px; border-radius: 8px; background: rgba(255,255,255,0.02); margin-bottom: 12px; }
.profile-info .title { font-weight: 700; color: #fff; }
.profile-info .subtitle { font-size: 12px; color: #9fb3cf; }
.search { position: relative; margin-bottom: 12px; }
.search input { width: 100%; padding: 8px 34px 8px 12px; border-radius: 6px; border: none; background: rgba(255,255,255,0.03); color: #cfe6ff; }
.search input::placeholder { color: #7896b3; }
.search .fa-search { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); color: #7896b3; }
.pending-company { padding: 10px 12px; margin-bottom: 12px; border-radius: 6px; background: rgba(255,193,7,0.12); color: #ffd778; font-size: 13px; line-height: 1.4; }
.menu { overflow: auto; flex: 1 1 auto; }
.menu ul { list-style: none; padding: 0; margin: 0; }
.menu li { margin-bottom: 4px; }
.menu-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; color: #dbeefd; text-decoration: none; border-radius: 6px; }
.menu-item i { width: 18px; text-align: center; color: #a9d0ff; }
.menu-item .label { flex: 1; }
.menu a.menu-item:hover { background: rgba(255,255,255,0.03); color: #fff; }
.menu li.active > .menu-item, .menu li > .menu-item.router-link-active { background: linear-gradient(90deg, rgba(255,255,255,0.06), rgba(255,255,255,0.03)); box-shadow: inset 0 0 0 1px rgba(255,255,255,0.02); }
.has-children .chev { transition: transform 0.25s ease; color: #9fb3cf; }
.has-children .chev.rotated { transform: rotate(-180deg); }
.sub { list-style: none; padding-left: 8px; margin: 6px 0 10px 0; }
.sub li { margin: 4px 0; }
.sub li a { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 6px; color: #cfe6ff; text-decoration: none; background: transparent; }
.sub li a i { width: 16px; text-align: center; color: #8fb3d9; font-size: 0.9em; }
.sub li a:hover { background: rgba(255,255,255,0.02); color: #fff; }
.sub li a:hover i { color: #a9d0ff; }
.sub li.active > a, .sub li > a.router-link-active { background: rgba(255,255,255,0.03); color: #fff; }
.sub li.active > a i, .sub li > a.router-link-active i { color: #a9d0ff; }
.sidebar-footer { text-align: center; padding: 8px 0; color: #7f9ebf; font-size: 12px; }
/* Hiệu ứng nhẹ cho submenu */
.slide-enter-active, .slide-leave-active { transition: all 240ms ease; }
.slide-enter, .slide-leave-to { opacity: 0; transform: translateY(-6px); height: 0; }
/* Responsive: thu gọn chiều rộng trên màn hình nhỏ */
@media (max-width: 768px) {
  .sidebar { width: 100%; height: auto; position: relative; }
}
</style>
