<template>
  <header class="top-nav d-flex align-items-center justify-content-between px-3 shadow-sm">
    <div class="menu-toggle">
      <a href="javascript:void(0)" @click="menuToggle">
        <i class="fa fa-bars fa-lg"></i>
      </a>
    </div>

    <nav class="d-flex align-items-center">
      <b-button variant="light" class="circle-btn mr-2" @click="reloadData">
        <i :class="reloading ? 'fa fa-sync fa-spin' : 'fa fa-sync'"></i>
      </b-button>

      <b-button variant="light" class="circle-btn mr-2" id="popover-hotline">
        <i class="fa fa-phone"></i>
      </b-button>
      <b-popover target="popover-hotline" placement="bottom" triggers="hover focus">
        <template #title>Support</template>
        <ul class="list-unstyled mb-0">
          <li><i class="fa fa-tools"></i> Hỗ trợ kỹ thuật 24/7</li>
          <li><i class="fa fa-phone"></i> Hotline: 111111 ext:2</li>
        </ul>
      </b-popover>

      <b-button variant="light" class="circle-btn mr-2" v-b-toggle.sidebar-right>
        <i class="far fa-bell text-danger"></i>
      </b-button>

      <b-dropdown right>
        <template #button-content>
          <b-avatar :src="app.auth.avatar" v-if="app.auth.avatar !== ''"></b-avatar>
          <b-avatar variant="primary" v-else>{{ app.auth.nameAvatar }}</b-avatar>
          <i class="fas fa-caret-down ml-1"></i>
        </template>
        <b-dropdown-item to="/setting/your-account">
          <i class="fas fa-user"></i> Tài khoản
        </b-dropdown-item>
        <b-dropdown-item to="/setting/your-account">
          <i class="fas fa-life-ring"></i> Hỗ trợ
        </b-dropdown-item>
        <b-dropdown-item href="#" @click.prevent="logout">
          <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </b-dropdown-item>
      </b-dropdown>
    </nav>

    <b-sidebar id="sidebar-right" title="Thông báo" right shadow>
      <ul class="list-unstyled p-2">
        <b-media tag="li" v-for="item in list" :key="item.id" class="mb-2">
          <template #aside>
            <b-avatar size="2rem" :src="item.avatar" v-if="!item.label_name"></b-avatar>
            <b-avatar size="2rem" variant="success" v-else>{{ item.avatar }}</b-avatar>
          </template>
          <h6 class="mb-1">{{ item.name }}</h6>
          <small class="text-muted">{{ item.date }}</small>
          <p class="mb-0">{{ item.title }}</p>
        </b-media>
      </ul>
    </b-sidebar>
  </header>
</template>

<script>
export default {
  name: "Header",
  data() {
    return {
      reloading: false,
      list: [],
      app: {
        auth: {
          avatar: '',
          nameAvatar: 'A'
        }
      }
    };
  },
  created() {
    this.list = [
      { id: 1, name: 'Admin', date: '2025-11-15', title: 'Thông báo 1', avatar: '' },
      { id: 2, name: 'System', date: '2025-11-14', title: 'Thông báo 2', avatar: '' }
    ];
  },
  methods: {
    reloadData() {
      this.reloading = true;
      setTimeout(() => { this.reloading = false }, 2000);
    },
    menuToggle() {
      document.body.classList.toggle('nav-sm');
      document.body.classList.toggle('nav-md');
    },
    logout() {
      const currentPath = window.location.pathname;
      const isAdminPage = currentPath.startsWith('/administrator') || currentPath.startsWith('/auth/login-admin');

      if (isAdminPage) {
        localStorage.removeItem('token-admin');
        window.location.href = '/auth/login-admin';
      } else {
        localStorage.removeItem('token');
        window.location.href = '/auth/login';
      }
    }
  }
};
</script>

<style scoped>
.top-nav { height: 60px; background: #fff; border-bottom: 1px solid #e0e0e0; position: sticky; top: 0; z-index: 1000; }
.menu-toggle a { color: #2c3e50; }
.circle-btn { width: 2.5rem; height: 2.5rem; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; padding: 0; }
.circle-btn i { font-size: 1rem; color: #2c3e50; }
</style>
