<template>
  <div class="sidebar">
    <div class="sidebar-inner">

      <!-- Biểu trưng -->
      <div class="brand">
        <router-link class="brand-link" to="/">
          <img class="logo" :src="logoSrc" alt="logo" />
        </router-link>
      </div>

      <!-- Trình đơn -->
      <nav class="menu">
        <ul>
          <li v-for="(item, index) in menu" :key="index" :class="{
            'has-children': !!item.children,
            open: openIndex === index,
            active: isActive(item),
          }">

            <!-- Trình đơn có con -->
            <template v-if="item.children">
              <a href="#" class="menu-item" @click.prevent="toggle(index)">
                <i :class="item.icon"></i>
                <span class="label">{{ item.title }}</span>
                <i class="chev fas fa-chevron-down" :class="{ rotated: openIndex === index }"></i>
              </a>

              <transition name="slide">
                <ul class="sub" v-show="openIndex === index">
                  <li v-for="(c, ci) in item.children" :key="ci" :class="{ active: isActive(c) }">
                    <router-link :to="c.to">
                      <i v-if="c.icon" :class="c.icon"></i>
                      {{ c.title }}
                    </router-link>
                  </li>
                </ul>
              </transition>

            </template>

            <!-- Trình đơn không con -->
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
  </div>
</template>


<script>
export default {
  name: "sidebar_administrator",

  data() {
    return {
      openIndex: null,
      menu: [
        { title: 'Trang chủ', icon: 'fas fa-home', to: '/' },
        { title: 'Công ty', icon: 'fas fa-building', to: '/administrator/company/list' },
        { title: 'Mua hóa đơn', icon: 'fas fa-file-invoice-dollar', to: '/administrator/buy-invoice/list' },
        { title: 'Duyệt đăng ký', icon: 'fas fa-user-check', to: '/administrator/company-registration/list' },
        { title: 'Mẫu hóa đơn', icon: 'fas fa-file-contract', to: '/administrator/form-invoice/list' },
        { title: 'Ngân hàng', icon: 'fas fa-piggy-bank', to: '/administrator/bank/list' },
        { title: 'Cơ quan thuế', icon: 'fas fa-landmark', to: '/administrator/tax-authority/list' },
        { title: 'Thuế suất', icon: 'fas fa-percent', to: '/administrator/vat-rate/list' },
        { title: 'Email template', icon: 'fas fa-envelope', to: '/administrator/email-template/list' },
        { title: 'Lịch sử gửi mail', icon: 'fas fa-history', to: '/administrator/email/mail-history' },
        {
          title: 'Phân quyền',
          icon: 'fas fa-user-shield',
          children: [
            { title: 'Quyền', to: '/administrator/access-control/permissions/list', icon: 'fas fa-key' },
            { title: 'Nhóm quyền', to: '/administrator/access-control/permission-categories/list', icon: 'fas fa-layer-group' },
          ],
        },
      ]
    };
  },

  computed: {
    logoSrc() {
      try {
        const logo = this.$app?.info?.company?.logo
        if (logo && typeof logo === 'string' && logo.trim() !== '') {
          return logo
        }
      } catch { }
      return require('@/assets/images/logo/logo-hoadon.png')
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

  mounted() {
    // Mở nhóm menu cha nếu đang ở route con
    this.menu.forEach((item, idx) => {
      if (item.children && this.isActive(item)) this.openIndex = idx;
    });
  },

  methods: {
    toggle(index) {
      this.openIndex = this.openIndex === index ? null : index;
    },

    isActive(item) {
      if (!item) return false;

      // Active nếu route = item.to (không trùng prefix nữa)
      if (item.to) return this.$route.path === item.to;

      // Nếu là cha => active nếu 1 trong child match URL
      if (item.children) {
        return item.children.some(c => this.$route.path === c.to);
      }

      return false;
    }
  }
};
</script>


<style scoped>
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #0f1724 0%, #0b2238 100%);
  color: #e6eef8;
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(2, 6, 23, 0.6);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;
}

.sidebar-inner {
  padding: 18px 14px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.brand {
  text-align: center;
  margin-bottom: 12px;
}

.logo {
  max-width: 160px;
  margin: auto;
  display: block;
}

.menu {
  flex: 1;
  overflow: auto;
}

.menu ul {
  padding: 0;
  margin: 0;
  list-style: none;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: #dbeefd;
  border-radius: 6px;
  text-decoration: none;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.03);
  color: #fff;
}

.menu-item i {
  width: 18px;
  text-align: center;
}

/* Hiển thị icon của mục con nếu có */
.sub li a i {
  width: 16px;
  text-align: center;
  margin-right: 8px;
}

.menu li.active>.menu-item {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.has-children .chev {
  margin-left: auto;
  transition: 0.25s;
}

.rotated {
  transform: rotate(-180deg);
}

.sub {
  padding-left: 10px;
  margin-top: 6px;
}

.sub li a {
  padding: 7px 12px;
  display: block;
  border-radius: 5px;
  color: #cfe6ff;
}

.sub li.active>a {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.sidebar-footer {
  text-align: center;
  font-size: 12px;
  padding: 10px 0;
  color: #8fa7c5;
}

.slide-enter-active,
.slide-leave-active {
  transition: 200ms;
}

.slide-enter,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
  height: 0;
}
</style>
