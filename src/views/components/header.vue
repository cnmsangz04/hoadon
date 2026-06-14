<template>
  <header class="top-nav d-flex align-items-center justify-content-between px-3 shadow-sm">
    <div class="menu-toggle">
      <a href="javascript:void(0)" @click="menuToggle">
        <i class="fa fa-bars fa-lg"></i>
      </a>
    </div>

    <nav class="d-flex align-items-center">
      <b-button v-if="!isCompanyPending" variant="light" class="circle-btn mr-2" @click="reloadData">
        <i :class="reloading ? 'fa fa-sync fa-spin' : 'fa fa-sync'"></i>
      </b-button>

      <b-button v-if="!isCompanyPending" variant="light" class="circle-btn mr-2" id="popover-hotline">
        <i class="fa fa-phone"></i>
      </b-button>
      <b-popover v-if="!isCompanyPending" target="popover-hotline" placement="bottom" triggers="hover focus">
        <template #title>Support</template>
        <ul class="list-unstyled mb-0">
          <li><i class="fa fa-tools"></i> Hỗ trợ kỹ thuật 24/7</li>
          <li><i class="fa fa-phone"></i> Hotline: 111111 ext:2</li>
        </ul>
      </b-popover>

      <!-- Chuông thông báo theo mẫu -->
      <li v-if="!isCompanyPending" class="nav-item bell-notify pl-0 pr-1">
        <a href="javascript:void(0)" class="info-number circle-icon-top position-relative" v-b-toggle.sidebar-right>
          <i class="far fa-bell text-danger"></i>
          <span v-if="list.length > 0" class="badge badge-danger badge-pill notification-badge">{{ list.length > 9 ? '9+' : list.length }}</span>
        </a>
      </li>

      <b-dropdown right toggle-class="user-toggle" variant="outline-info">
        <template #button-content>
          <b-avatar :src="avatarSrc" v-if="avatarSrc" class="user-avatar"></b-avatar>
          <b-avatar variant="primary" v-else class="user-avatar">{{ usernameInitial }}</b-avatar>
          <span class="ml-2 d-none d-md-inline">{{ app.auth.username || 'User' }}</span>
        </template>
        <b-dropdown-item v-if="!isCompanyPending" to="/setting">
          <i class="fas fa-user"></i> Tài khoản
        </b-dropdown-item>
        <b-dropdown-item v-if="!isCompanyPending" to="/help-support">
          <i class="fas fa-life-ring"></i> Hỗ trợ
        </b-dropdown-item>
        <b-dropdown-item v-if="!isCompanyPending && showAdminLink" to="/auth/login-admin">
          <i class="fas fa-shield-alt"></i> Quản trị
        </b-dropdown-item>
        <b-dropdown-item href="#" @click.prevent="logout">
          <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </b-dropdown-item>
      </b-dropdown>
    </nav>

    <b-sidebar v-if="!isCompanyPending" id="sidebar-right" title="Thông báo" right shadow>
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
      },
      pollingInterval: null,
      lastNotificationCount: 0
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
      // Ưu tiên: $app.info.user.avatar (từ database) > payload JWT > dữ liệu local
      try {
        const dbAvatar = this.$app?.info?.user?.avatar
        if (dbAvatar && typeof dbAvatar === 'string' && dbAvatar.trim() !== '') {
          return dbAvatar
        }
      } catch {}
      // Dự phòng dùng app.auth.avatar local
      return this.app.auth.avatar || ''
    },
    isCompanyPending() {
      const status = this.$app?.info?.company?.status ?? localStorage.getItem('company-status')
      return String(status) === '2'
    }
  },
  created() {
    // Nạp thông tin auth từ JWT nếu có, không dùng companyId trong localStorage
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

        // Chỉ lấy company id từ payload JWT, không dự phòng/lưu bằng localStorage
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

    // Dùng thông tin đã tải toàn cục nếu có, không lưu companyId vào localStorage
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

    // Lấy thông báo gần đây (tối đa 10) từ lịch sử theo điều kiện yêu cầu
    if (!this.isCompanyPending) this.fetchNotifications()

    // Mẫu realtime Echo: khi có tin nhắn thì hiện toast và tải lại thông báo
    try {
      if (typeof io !== 'undefined') {
        const vm = this
        const channel = vm.app?._channel || 'default'
        Echo.channel('NotificationCpanel_' + channel)
          .listen('.MessagePostedCpanel', (data) => {
            vm.app._isRefresh = true
            const msg = data?.message?.message || ''
            if (msg) { window.toastr && toastr.success(msg) }
            if (!vm.isCompanyPending) vm.fetchNotifications()
          })
      }
    } catch {}
  },
  mounted() {
    // Đảm bảo đã gọi /auth/info để có role/company dù JWT thiếu role; cập nhật đúng $app toàn cục
    try {
      axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } })
        .then(res => {
          const info = res.data || {}
          
          // Cập nhật dữ liệu auth local
          if (info?.user && typeof info.user.role === 'number') {
            this.app.auth.role = info.user.role
          }
          
          // Cập nhật đối tượng $app toàn cục (kích hoạt cập nhật logo sidebar)
          if (this.$app) {
            this.$app.info.user = info.user || null
            this.$app.info.company = info.company || null
          }
          if (info.company && info.company.status != null) {
            localStorage.setItem('company-status', String(info.company.status))
          }
          if (this.isCompanyPending) {
            this.list = []
            this.stopPolling()
          } else {
            this.fetchNotifications()
            this.startPolling()
          }
        })
    } catch {}
  },
  beforeDestroy() {
    // Dọn interval polling khi component bị hủy
    this.stopPolling()
  },
  methods: {
    // Trả về chữ cái đầu viết hoa từ tên; mặc định là '?'
    nameInitial(name) {
      const n = (name || '').trim()
      return n ? n.charAt(0).toUpperCase() : '?'
    },
    reloadData() {
      this.reloading = true;
      setTimeout(() => { this.reloading = false }, 2000);
      if (!this.isCompanyPending) this.fetchNotifications()
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
        localStorage.removeItem('company-status');
        window.location.href = '/auth/login';
      }
    },
    async fetchNotifications() {
      if (this.isCompanyPending) {
        this.list = []
        return
      }
      try {
        const params = { limit: 10, show_notify: 1, status: 1 }
        const res = await axios.get('/history/notifications', { params, meta: { suppressGlobalErrorToast: true } })
          .catch(err => {
            if (err?.response?.status === 401) this.stopPolling()
            throw err
          })
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

        const oldList = this.list
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

        // Kiểm tra có thông báo mới không (so sánh số lượng hoặc ID)
        if (oldList.length > 0 && this.list.length > oldList.length) {
          const newCount = this.list.length - oldList.length
          try { 
            window.toastr && toastr.info(`Bạn có ${newCount} thông báo mới`) 
          } catch {}
        } else if (oldList.length > 0 && this.list.length > 0) {
          // Kiểm tra item đầu tiên có mới không (khác ID)
          const oldFirstId = oldList[0]?.id
          const newFirstId = this.list[0]?.id
          if (oldFirstId && newFirstId && oldFirstId !== newFirstId) {
            try { 
              window.toastr && toastr.info(`Bạn có thông báo mới: ${this.list[0].title}`) 
            } catch {}
          }
        }
      } catch (e) {
        // Bỏ qua lỗi âm thầm với request polling
        console.debug('Fetch notifications failed:', e)
        this.list = []
      }
    },
    startPolling() {
      if (this.isCompanyPending) return
      // Dừng interval hiện có trước
      this.stopPolling()
      
      // Poll mỗi 5 giây
      this.pollingInterval = setInterval(() => {
        if (this.isCompanyPending) {
          this.stopPolling()
          return
        }
        this.fetchNotifications()
      }, 5000)
    },
    stopPolling() {
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval)
        this.pollingInterval = null
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
/* Style thanh điều hướng trên đã tinh chỉnh */
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

/* Style mẫu chuông thông báo */
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

/* Huy hiệu thông báo */
.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 2px 5px;
  font-size: 10px;
  font-weight: 600;
  line-height: 14px;
  border-radius: 9px;
  background: #dc3545;
  color: white;
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.4);
  animation: pulse-badge 2s infinite;
}

@keyframes pulse-badge {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* Nút mở dropdown người dùng */
.user-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: background 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.user-toggle:hover { background: #f6f8fb; border-color: #e6e9ed; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.user-toggle span {
  color: #2c3e50;
  font-weight: 500;
  font-size: 0.95rem;
}

.user-avatar {
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border: 2px solid #ffffff;
}

.caret { color: #7b8a8b; }

/* Tinh chỉnh sidebar */
#sidebar-right ::v-deep .b-sidebar {
  border-left: 1px solid #e9ecef;
  background: #ffffff;
  height: 100vh;
  max-height: 100vh !important; /* ghi đè max-height mặc định: 100% */
}
#sidebar-right ::v-deep .b-sidebar-body {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* Khung danh sách thông báo: chiếm phần còn lại và cuộn */
#sidebar-right .list-unstyled {
  margin: 0;
  /* Ước lượng chiều cao tiêu đề (gồm padding) ~56px; chỉnh nếu cần */
  max-height: calc(100vh - 56px) !important;
  overflow-y: auto;
  padding: 6px 8px;
}

/* Bố cục item thông báo */
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

/* Kiểu hiển thị avatar */
#sidebar-right .b-avatar {
  box-shadow: 0 1px 3px rgba(16,24,40,0.08);
  border: 2px solid #ffffff;
}

/* Tiêu đề và thông tin phụ */
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

/* Nội dung text */
#sidebar-right p {
  font-size: 0.92rem;
  color: #374151;
  margin: 0;
}

/* Đường phân cách giữa các item */
#sidebar-right .b-media + .b-media::before {
  content: "";
  position: absolute;
  left: 52px;
  right: 12px;
  top: -6px;
  height: 1px;
  background: #f0f2f5;
}

/* Trạng thái rỗng */
#sidebar-right .text-center.text-muted {
  color: #94a3b8 !important;
  background: #f8fafc;
  border: 1px dashed #e5e7eb;
  border-radius: 12px;
}

/* Style tiêu đề popover */
::v-deep .popover-header { background: #f9fafb; font-weight: 600; }

/* Tinh chỉnh responsive */
@media (max-width: 768px) {
  .top-nav { height: 56px; }
  .circle-btn, .bell-notify .circle-icon-top { width: 36px; height: 36px; }
  .menu-toggle a { padding: 6px 8px; }
  /* Giữ sidebar cao toàn màn hình trên mobile */
  #sidebar-right ::v-deep .b-sidebar { max-height: 100vh !important; }
  #sidebar-right .list-unstyled { max-height: calc(100vh - 56px) !important; }
}
</style>

<style>
/* Ghi đè toàn cục cho sidebar BootstrapVue render trong body */
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
  max-height: calc(100vh - 56px) !important; /* chừa chỗ cho tiêu đề/header */
  overflow-y: auto;
}
@media (max-width: 768px) {
  .b-sidebar#sidebar-right { max-height: 100vh !important; }
  .b-sidebar#sidebar-right .list-unstyled { max-height: calc(100vh - 56px) !important; }
}
</style>

