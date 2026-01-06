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

      <!-- Bell notification per sample -->
      <li class="nav-item bell-notify pl-0 pr-1">
        <a href="javascript:void(0)" class="info-number circle-icon-top" v-b-toggle.sidebar-right>
          <i class="far fa-bell text-danger"></i>
        </a>
      </li>

      <b-dropdown right toggle-class="user-toggle">
        <template #button-content>
          <b-avatar :src="avatarSrc" v-if="avatarSrc" class="user-avatar"></b-avatar>
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
            <b-avatar size="2rem" :src="item.avatar" v-if="item.avatar"></b-avatar>
            <b-avatar size="2rem" variant="success" v-else>{{ nameInitial(item.username || 'Hệ thống') }}</b-avatar>
          </template>
          <h6 class="mb-1">{{ item.username || 'Hệ thống' }}</h6>
          <small class="text-muted">{{ relativeTime(item.created_at) }}</small>
          <p class="mb-0">{{ item.title }}</p>
        </b-media>
        <li v-if="list.length === 0" class="text-center text-muted py-2">Không có thông báo</li>
      </ul>
    </b-sidebar>
  </header>
</template>

<script>
import { parseJwt } from '@/utils/jwt'
import axios from '@/plugins/axios'
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
          name: '',
          role: null
        }
      }
    };
  },
  computed: {
    usernameInitial() {
      const n = (this.app.auth.name || '').trim()
      return n ? n.charAt(0).toUpperCase() : 'A'
    },
    showAdminLink() {
      const role = this.app.auth.role
      return role === 0 || role === 1
    },
    avatarSrc() {
      // Priority: $app.info.user.avatar (from database) > JWT payload > local data
      try {
        const dbAvatar = this.$app?.info?.user?.avatar
        if (dbAvatar && typeof dbAvatar === 'string' && dbAvatar.trim() !== '') {
          return dbAvatar
        }
      } catch {}
      // Fallback to local app.auth.avatar
      return this.app.auth.avatar || ''
    }
  },
  created() {
    // Populate auth from JWT if available without using localStorage companyId
    try {
      const p = window.location?.pathname || ''
      const isAdmin = /administrator|admin/.test(p)
      const primaryKey = isAdmin ? 'token-admin' : 'token'
      const altKey = isAdmin ? 'token' : 'token-admin'
      let token = localStorage.getItem(primaryKey) || localStorage.getItem(altKey)
      const payload = parseJwt(token)
      if (payload) {
        this.app.auth.username = payload.username || payload.user_name || payload.sub || this.app.auth.username || ''
        this.app.auth.name = payload.name || payload.full_name || payload.displayName || this.app.auth.name || ''
        this.app.auth.avatar = payload.avatar || this.app.auth.avatar || ''
        const pr = payload.role ?? payload.roles ?? payload.authority ?? payload.auth_role
        if (typeof pr === 'number') {
          this.app.auth.role = pr
        } else if (Array.isArray(pr)) {
          this.app.auth.role = pr.includes('ADMIN') ? 1 : (pr.includes('SUPER_ADMIN') ? 0 : null)
        } else if (typeof pr === 'string') {
          const s = pr.toUpperCase()
          this.app.auth.role = s.includes('SUPER') ? 0 : (s.includes('ADMIN') ? 1 : null)
        }

        // Extract company id only from JWT payload, no localStorage fallback/persist
        const cid = (
          payload.companyId ?? payload.company_id ??
          (typeof payload.company === 'object' ? payload.company?.id : payload.company) ??
          payload.cid ?? payload.tenantId ?? payload.tenant_id ?? payload.orgId ?? payload.org_id
        )
        const nCid = cid != null && cid !== '' ? Number(cid) : undefined
        if (nCid != null && Number.isFinite(nCid)) {
          const cur = this.$app?.info?.company || {}
          this.$app.info.company = { ...(cur || {}), id: nCid }
        }
      }
    } catch {}

    // Use globally fetched info if available; do not persist companyId to localStorage
    try {
      const info = this.$app?.info || {}
      if (info.user) {
        this.app.auth.username = info.user.username || this.app.auth.username
        this.app.auth.name = info.user.name || this.app.auth.name
        this.app.auth.avatar = info.user.avatar || this.app.auth.avatar
        if (typeof info.user.role === 'number') this.app.auth.role = info.user.role
      }
      if (info.company) {
        const cur = this.$app.info.company || {}
        if (info.company.id != null && !Number.isNaN(Number(info.company.id))) {
          this.$app.info.company = { ...cur, id: Number(info.company.id) }
        }
      }
    } catch {}

    // Fetch recent notifications (limit 10) from history with required conditions
    this.fetchNotifications()

    // Echo realtime sample: on message, show toast and refresh notifications
    try {
      if (typeof io !== 'undefined') {
        const vm = this
        const channel = vm.app?._channel || 'default'
        Echo.channel('NotificationCpanel_' + channel)
          .listen('.MessagePostedCpanel', (data) => {
            vm.app._isRefresh = true
            const msg = data?.message?.message || ''
            if (msg) { window.toastr && toastr.success(msg) }
            vm.fetchNotifications()
          })
      }
    } catch {}
  },
  mounted() {
    // Ensure /auth/info is fetched so role/company is set even if JWT lacks role; don’t write localStorage companyId
    try {
      axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } })
        .then(res => {
          const info = res.data || {}
          if (info?.user && typeof info.user.role === 'number') {
            this.app.auth.role = info.user.role
          }
          if (info?.company?.id != null) {
            const cur = this.$app?.info?.company || {}
            this.$app.info.company = { ...cur, id: Number(info.company.id) }
          }
        })
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
      this.fetchNotifications()
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
    },
    async fetchNotifications() {
      try {
        const params = { limit: 10, show_notify: 1, status: 1 }
        const res = await axios.get('/history/notifications', { params })
        const payload = res && res.data

        const arr = Array.isArray(payload)
          ? payload
          : Array.isArray(payload?.data)
          ? payload.data
          : Array.isArray(payload?.items)
          ? payload.items
          : Array.isArray(payload?.content)
          ? payload.content
          : Array.isArray(payload?.list)
          ? payload.list
          : Array.isArray(payload?.rows)
          ? payload.rows
          : []

        this.list = arr.map(r => {
          const created = r.createdAt ?? r.created_at ?? r.time ?? r.timestamp
          const title = r.title ?? r.message ?? r.description ?? ''
          const uname = r.username ?? r.userName ?? r.user_name ?? r.user ?? ''
          const uid = r.userId ?? r.user_id
          const hasUser = uid != null && String(uid).trim() !== '' && Number(uid) > 0
          return {
            id: r.id ?? r._id ?? r.uuid ?? Math.random().toString(36).slice(2),
            title: String(title).trim(),
            description: r.description ?? r.detail ?? '',
            username: hasUser ? (String(uname || '').trim() || 'Hệ thống') : 'Hệ thống',
            created_at: created,
            avatar: r.avatar ?? null,
          }
        })

        // Quick visual confirmation
        try { window.toastr && toastr.info(`Thông báo: ${this.list.length}`) } catch {}
      } catch (e) {
        this.list = []
      }
    },
    relativeTime(d) {
      if (!d) return ''
      try {
        const now = Date.now()
        const t = new Date(d).getTime()
        const diff = Math.max(0, Math.floor((now - t) / 1000))
        const years = Math.floor(diff / (365 * 24 * 3600))
        if (years > 0) return `${years} năm trước`
        const months = Math.floor(diff / (30 * 24 * 3600))
        if (months > 0) return `${months} tháng trước`
        const days = Math.floor(diff / (24 * 3600))
        if (days > 0) return `${days} ngày trước`
        const hours = Math.floor(diff / 3600)
        if (hours > 0) return `${hours} giờ trước`
        const minutes = Math.floor(diff / 60)
        if (minutes > 0) return `${minutes} phút trước`
        return 'Vừa xong'
      } catch { return '' }
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

nav.d-flex.align-items-center {
  gap: 8px;
}

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

/* Bell notify sample styles */
.bell-notify { list-style: none; }
.bell-notify .circle-icon-top {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #e6e9ed;
  border-radius: 50%;
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.06s ease;
}
.bell-notify .circle-icon-top:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }

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
  background: #ffffff;
  height: 100vh;
  max-height: 100vh !important; /* override default max-height: 100% */
}
#sidebar-right ::v-deep .b-sidebar-body {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* Notification list wrapper: occupy remaining space and scroll */
#sidebar-right .list-unstyled {
  margin: 0;
  /* Approximate title height (including padding) ~56px; adjust if needed */
  max-height: calc(100vh - 56px) !important;
  overflow-y: auto;
  padding: 6px 8px;
}

/* Notification item layout */
#sidebar-right .b-media {
  position: relative;
  padding: 12px 12px;
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid transparent;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}
#sidebar-right .b-media:hover {
  background: #f8fafc;
  border-color: #edf2f7;
  box-shadow: 0 1px 2px rgba(16,24,40,0.05);
}

/* Avatar stylings */
#sidebar-right .b-avatar {
  box-shadow: 0 1px 3px rgba(16,24,40,0.08);
  border: 2px solid #ffffff;
}

/* Title and meta */
#sidebar-right h6 {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}
#sidebar-right small {
  display: inline-block;
  color: #6b7280;
  margin-bottom: 6px;
}

/* Content text */
#sidebar-right p {
  font-size: 0.92rem;
  color: #374151;
  margin: 0;
}

/* Divider between items */
#sidebar-right .b-media + .b-media::before {
  content: "";
  position: absolute;
  left: 52px;
  right: 12px;
  top: -6px;
  height: 1px;
  background: #f0f2f5;
}

/* Empty state */
#sidebar-right .text-center.text-muted {
  color: #94a3b8 !important;
  background: #f8fafc;
  border: 1px dashed #e5e7eb;
  border-radius: 12px;
}

/* Popover title style */
::v-deep .popover-header { background: #f9fafb; font-weight: 600; }

/* Responsive adjustments */
@media (max-width: 768px) {
  .top-nav { height: 56px; }
  .circle-btn, .bell-notify .circle-icon-top { width: 36px; height: 36px; }
  .menu-toggle a { padding: 6px 8px; }
  /* Keep sidebar full height on mobile */
  #sidebar-right ::v-deep .b-sidebar { max-height: 100vh !important; }
  #sidebar-right .list-unstyled { max-height: calc(100vh - 56px) !important; }
}
</style>

<style>
/* Global overrides for BootstrapVue sidebar rendered in body */
.b-sidebar#sidebar-right {
  height: 100vh;
  max-height: 100vh !important;
}
.b-sidebar#sidebar-right .b-sidebar-body {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
}
.b-sidebar#sidebar-right .list-unstyled {
  margin: 0;
  max-height: calc(100vh - 56px) !important; /* leave room for title/header */
  overflow-y: auto;
}
@media (max-width: 768px) {
  .b-sidebar#sidebar-right { max-height: 100vh !important; }
  .b-sidebar#sidebar-right .list-unstyled { max-height: calc(100vh - 56px) !important; }
}
</style>