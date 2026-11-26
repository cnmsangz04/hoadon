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
        {{ $t('menu.pending') }}
      </div>

      <div v-else class="search">
        <input v-model="search" type="search" :placeholder="$t('common.search')" :aria-label="$t('menu.searchLabel')" />
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
                <span class="label">{{ itemTitle(item) }}</span>
                <i class="chev fas fa-chevron-down" :class="{ rotated: isOpen === index }" aria-hidden="true"></i>
              </a>

              <transition name="slide">
                <ul class="sub" v-show="isOpen === index">
                  <li v-for="(c, ci) in item.children" :key="ci" :class="{ active: isActive(c) }">
                    <router-link :to="c.to">
                      <i v-if="c.icon" :class="c.icon"></i>
                      <span>{{ itemTitle(c) }}</span>
                    </router-link>
                  </li>
                </ul>
              </transition>
            </template>

            <!-- Link đơn -->
            <template v-else>
              <router-link class="menu-item" :to="item.to">
                <i :class="item.icon"></i>
                <span class="label">{{ itemTitle(item) }}</span>
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
import { appResourceUrl } from '@/utils/appUrl'

export default {
  name: 'Sidebar',
  data() {
    return {
      isOpen: null,
      search: '',
      menuItems: [
        { titleKey: 'home', icon: 'fas fa-home', to: '/' },
        { titleKey: 'invoicePackages', icon: 'fas fa-box-open', to: '/invoice-packages', pendingAllowed: true },
        {
          titleKey: 'registration',
          icon: 'fas fa-file-signature',
          children: [
            { titleKey: 'electronicInvoiceDeclaration', icon: 'fas fa-file-alt', to: '/register/invoice/list' },
          ],
        },
        { titleKey: 'invoiceTemplates', icon: 'fas fa-file-contract', to: '/form-invoice/list' },
        {
          titleKey: 'invoices',
          icon: 'fas fa-file-invoice-dollar',
          children: [
            { titleKey: 'vatInvoices', icon: 'fas fa-receipt', to: '/invoice/vat-invoice/list' },
            { titleKey: 'salesInvoices', icon: 'fas fa-file-invoice', to: '/invoice/sales-invoice/list' },
          ],
        },
        {
          titleKey: 'stockIssues',
          icon: 'fas fa-warehouse',
          children: [
            { titleKey: 'internalTransport', icon: 'fas fa-truck-loading', to: '/stock-issue/internal-transport/list' },
            { titleKey: 'consignment', icon: 'fas fa-dolly', to: '/stock-issue/consignment/list' },
          ],
        },
        {
          titleKey: 'dataImport',
          icon: 'fas fa-file-import',
          children: [
            { titleKey: 'importInvoices', icon: 'fas fa-file-excel', to: '/imports/invoice', menuKey: 'invoice-import' },
            { titleKey: 'importStockIssues', icon: 'fas fa-file-excel', to: '/imports/stock-issue', menuKey: 'stock-import' },
            { titleKey: 'importCustomers', icon: 'fas fa-users', to: '/imports/customer' },
            { titleKey: 'importProducts', icon: 'fas fa-box', to: '/imports/product' },
          ],
        },
        {
          titleKey: 'invoiceProcessing',
          icon: 'fas fa-tasks',
          children: [
            { titleKey: 'invoiceErrors', icon: 'fas fa-exclamation-triangle', to: '/error-message/invoice/list', menuKey: 'invoice-error-message' },
            { titleKey: 'replacementInvoices', icon: 'fas fa-exchange-alt', to: '/invoice/replace' },
            { titleKey: 'adjustedInvoices', icon: 'fas fa-sliders-h', to: '/invoice/adjust' },
          ],
        },
        {
          titleKey: 'stockIssueProcessing',
          icon: 'fas fa-tools',
          children: [
            { titleKey: 'stockIssueErrors', icon: 'fas fa-exclamation-triangle', to: '/error-message/stock-issue/list', menuKey: 'stock-error-message' },
            { titleKey: 'replacementStockIssues', icon: 'fas fa-exchange-alt', to: '/stock-issue/replace' },
            { titleKey: 'adjustedStockIssues', icon: 'fas fa-sliders-h', to: '/stock-issue/adjust' },
          ],
        },
        {
          titleKey: 'reports',
          icon: 'fas fa-chart-bar',
          children: [
            { titleKey: 'vatInvoiceReport', icon: 'fas fa-chart-bar', to: '/reports/invoice/list' },
            { titleKey: 'salesReport', icon: 'fas fa-chart-line', to: '/reports/sales-invoice/list' },
            { titleKey: 'internalTransportReport', icon: 'fas fa-chart-line', to: '/reports/stock-issue/internal-transport/list' },
            { titleKey: 'consignmentReport', icon: 'fas fa-chart-line', to: '/reports/stock-issue/consignment/list' },
          ],
        },
        {
          titleKey: 'categories',
          icon: 'fas fa-th-list',
          children: [
            { titleKey: 'customers', icon: 'fas fa-users', to: '/categories/customer/list' },
            { titleKey: 'products', icon: 'fas fa-box', to: '/categories/product/list' },
          ],
        },
        {
          titleKey: 'invoiceEmail',
          icon: 'fas fa-envelope',
          children: [
            { titleKey: 'mailServer', icon: 'fas fa-server', to: '/email/mail-server' },
            { titleKey: 'mailHistory', icon: 'fas fa-history', to: '/email/mail-history' },
          ],
        },
        { titleKey: 'settings', icon: 'fas fa-cog', to: '/setting' },
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
            return this.itemTitle(item).toLowerCase().indexOf(q) !== -1 ? item : null;
          }
          const matchedChildren = item.children.filter((c) => this.itemTitle(c).toLowerCase().indexOf(q) !== -1);
          if (this.itemTitle(item).toLowerCase().indexOf(q) !== -1) return item;
          return matchedChildren.length ? { ...item, children: matchedChildren } : null;
        })
        .filter(Boolean);
    },
    logoSrc() {
      try {
        const logo = this.$app?.info?.company?.logo
        if (logo && typeof logo === 'string' && logo.trim() !== '') return appResourceUrl(logo)
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
    itemTitle(item) {
      return this.$t(`menu.customer.${item.titleKey}`)
    },
    toggle(index) {
      this.isOpen = this.isOpen === index ? null : index;
    },
		
    isActive(item) {
      if (!item) return false;

      // Mục không có menu con
      if (item.to) {
        if (item.menuKey === 'invoice-import') {
          return this.$route.path === '/imports/invoice'
        }
        if (item.menuKey === 'stock-import') {
          return this.$route.path === '/imports/stock-issue'
        }
        if (item.menuKey === 'invoice-error-message') {
          return this.$route.path === '/error-message/invoice/list' || this.$route.path === '/error-message/invoice/create' || this.$route.path.indexOf('/error-message/invoice/') === 0
        }
        if (item.menuKey === 'stock-error-message') {
          return this.$route.path === '/error-message/stock-issue/list' || this.$route.path === '/error-message/stock-issue/create' || this.$route.path.indexOf('/error-message/stock-issue/') === 0
        }
        return this.$route.path === item.to.split('?')[0]
      }

      // Menu cha được chọn khi đường dẫn trùng với menu con
      if (item.children) {
        return item.children.some(c => this.isActive(c));
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
  flex: 0 0 260px;
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
/* Thu gọn chiều rộng trên màn hình nhỏ */
@media (max-width: 768px) {
  .sidebar { width: 100%; flex-basis: auto; height: auto; position: relative; }
}
</style>
