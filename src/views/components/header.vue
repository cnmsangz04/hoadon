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

      <b-dropdown right toggle-class="user-toggle">
        <template #button-content>
          <b-avatar :src="app.auth.avatar" v-if="app.auth.avatar" class="user-avatar"></b-avatar>
          <b-avatar variant="primary" v-else class="user-avatar">{{ usernameInitial }}</b-avatar>
        </template>
        <b-dropdown-item to="/setting">
          <i class="fas fa-user"></i> Tài khoản
        </b-dropdown-item>
        <b-dropdown-item to="/help-support">
          <i class="fas fa-life-ring"></i> Hỗ trợ
        </b-dropdown-item>
        <b-dropdown-item v-if="showAdminLink" to="/auth/login-admin">
          <i class="fas fa-shield-alt"></i> Quản trị
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
            <!-- Show image avatar when available; otherwise show initial letter -->
            <b-avatar size="2rem" :src="item.avatar" v-if="item.avatar"></b-avatar>
            <b-avatar size="2rem" variant="success" v-else>{{ nameInitial(item.name) }}</b-avatar>
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
import { parseJwt } from '@/utils/jwt'
export default {
  name: "Header",
  data() {
    return {
      reloading: false,
      list: [],
      app: {
        auth: {
          avatar: '',
          username: '',
          name: ''
        }
      }
    };
  },
  computed: {
    usernameInitial() {
      // Change to use user name initial
      const n = (this.app.auth.name || '').trim()
      return n ? n.charAt(0).toUpperCase() : 'A'
    },
    showAdminLink() {
      const u = (this.$app && this.$app.info && this.$app.info.user) ? this.$app.info.user : null
      if (!u) return false
      const role = u.role
      return role === 0 || role === 1
    }
  },
  created() {
    this.list = [
      { id: 1, name: 'Admin', date: '2025-11-15', title: 'Thông báo 1', avatar: '' },
      { id: 2, name: 'System', date: '2025-11-14', title: 'Thông báo 2', avatar: '' }
    ];

    // Populate auth from JWT if available and preserve companyId
    try {
      const p = window.location?.pathname || ''
      const isAdmin = /administrator|admin/.test(p)
      const primaryKey = isAdmin ? 'token-admin' : 'token'
      const altKey = isAdmin ? 'token' : 'token-admin'
      let token = localStorage.getItem(primaryKey) || localStorage.getItem(altKey)
      const payload = parseJwt(token)
      if (payload) {
        // Basic user visuals
        this.app.auth.username = payload.username || payload.user_name || payload.sub || this.app.auth.username || ''
        this.app.auth.name = payload.name || payload.full_name || payload.displayName || this.app.auth.name || ''
        this.app.auth.avatar = payload.avatar || this.app.auth.avatar || ''

        // Extract company id from various fields
        const cid = (
          payload.companyId ?? payload.company_id ??
          (typeof payload.company === 'object' ? payload.company?.id : payload.company) ??
          payload.cid ?? payload.tenantId ?? payload.tenant_id ?? payload.orgId ?? payload.org_id
        )
        const nCid = cid != null && cid !== '' ? Number(cid) : undefined
        if (nCid != null && Number.isFinite(nCid)) {
          // Initialize or update global app company info
          const cur = this.$app?.info?.company || {}
          this.$app.info.company = { ...(cur || {}), id: nCid }
          // Mirror to localStorage to survive token changes
          try { localStorage.setItem('companyId', String(nCid)) } catch {}
        } else {
          // Fallback: keep previous or localStorage value
          const lsCid = localStorage.getItem('companyId')
          if (lsCid) {
            const n = Number(lsCid)
            if (Number.isFinite(n)) {
              const cur = this.$app?.info?.company || {}
              this.$app.info.company = { ...(cur || {}), id: n }
            }
          }
        }
      }
    } catch {}

    // Use globally fetched info if available (without overwriting existing company id)
    try {
      const info = this.$app?.info || {}
      if (info.user) {
        this.app.auth.username = info.user.username || this.app.auth.username
        this.app.auth.name = info.user.name || this.app.auth.name
        this.app.auth.avatar = info.user.avatar || this.app.auth.avatar
      }
      if (info.company) {
        const cur = this.$app.info.company || {}
        // Only set if id present or not already set
        if (info.company.id != null && !Number.isNaN(Number(info.company.id))) {
          this.$app.info.company = { ...cur, id: Number(info.company.id) }
          try { localStorage.setItem('companyId', String(Number(info.company.id))) } catch {}
        }
      }
    } catch {}
  },
  methods: {
    // Return the uppercase initial from a name; fallback to '?'
    nameInitial(name) {
      const n = (name || '').trim()
      return n ? n.charAt(0).toUpperCase() : '?'
    },
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
        localStorage.removeItem('last-admin-account');
        window.location.href = '/auth/login-admin';
      } else {
        localStorage.removeItem('token');
        localStorage.removeItem('last-account');
        window.location.href = '/auth/login';
      }
    }
  }
};
</script>

<style scoped>
/* Polished top nav styling */
.top-nav {
  height: 64px;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
  border-bottom: 1px solid #e9ecef;
  position: sticky;
  top: 0;
  z-index: 1000;
  backdrop-filter: saturate(180%) blur(6px);
}

.menu-toggle a {
  color: #34495e;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.2s ease, color 0.2s ease;
}
.menu-toggle a:hover { background: #f5f7fa; color: #2c3e50; }

.circle-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #e6e9ed;
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.06s ease;
}
.circle-btn i { font-size: 1rem; color: #2f3b52; }
.circle-btn:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.circle-btn:active { transform: scale(0.98); }

/* User dropdown trigger */
.user-toggle {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: background 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.user-toggle:hover { background: #f6f8fb; border-color: #e6e9ed; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }

.user-avatar {
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border: 2px solid #ffffff;
}

.caret { color: #7b8a8b; }

/* Sidebar tweaks */
#sidebar-right ::v-deep .b-sidebar {
  border-left: 1px solid #e9ecef;
}

/* Popover title style */
::v-deep .popover-header { background: #f9fafb; font-weight: 600; }
</style>