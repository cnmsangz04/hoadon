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
        <a href="javascript:void(0)" class="info-number circle-icon-top position-relative" v-b-toggle.sidebar-right @click="markNotificationsRead">
          <i :class="unreadNotificationCount > 0 ? 'far fa-bell text-danger' : 'far fa-bell text-muted'"></i>
          <span v-if="unreadNotificationCount > 0" class="badge badge-danger badge-pill notification-badge">
            {{ unreadNotificationCount > 9 ? '9+' : unreadNotificationCount }}
          </span>
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
        <b-dropdown-item v-if="!isCompanyPending && showAdminLink" to="/auth/login-admin">
          <i class="fas fa-shield-alt"></i> Quản trị
        </b-dropdown-item>
        <b-dropdown-item href="#" @click.prevent="logout">
          <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </b-dropdown-item>
      </b-dropdown>
    </nav>

    <b-sidebar
      v-if="!isCompanyPending"
      id="sidebar-right"
      title="Thông báo"
      right
      shadow
      @shown="handleNotificationsShown"
      @hidden="handleNotificationsHidden"
    >
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

    <b-modal
      v-model="limitReminder.show"
      centered
      hide-footer
      title="Nhắc nhở hạn mức hóa đơn"
      modal-class="invoice-limit-modal"
    >
      <div class="limit-reminder">
        <div class="limit-reminder-icon" :class="limitReminder.variant">
          <i :class="limitReminder.icon"></i>
        </div>
        <div class="limit-reminder-body">
          <h5>{{ limitReminder.title }}</h5>
          <p>{{ limitReminder.message }}</p>
          <b-progress height="10px" :value="limitReminder.percent" :variant="limitReminder.progressVariant" class="mb-3"></b-progress>
          <div class="limit-reminder-stats">
            <div>
              <span>Đã dùng</span>
              <strong>{{ formatNumber(limitReminder.used) }}</strong>
            </div>
            <div>
              <span>Tổng hạn mức</span>
              <strong>{{ formatNumber(limitReminder.total) }}</strong>
            </div>
            <div>
              <span>Còn lại</span>
              <strong>{{ formatNumber(limitReminder.remaining) }}</strong>
            </div>
          </div>
        </div>
      </div>
      <div class="text-right mt-3">
        <b-button variant="success" to="/invoice-packages" @click="closeLimitReminder">
          Mua thêm hóa đơn
        </b-button>
        <b-button variant="secondary" class="ml-2" @click="closeLimitReminder">
          Đóng
        </b-button>
      </div>
    </b-modal>
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
      lastNotificationCount: 0,
      notificationsOpen: false,
      readNotificationKeys: [],
      limitReminder: {
        show: false,
        threshold: 0,
        percent: 0,
        total: 0,
        used: 0,
        remaining: 0,
        title: '',
        message: '',
        icon: 'fas fa-info-circle',
        variant: 'info',
        progressVariant: 'info'
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
    },
    unreadNotificationCount() {
      return this.list.filter(item => !this.isNotificationRead(item)).length
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

    this.loadNotificationReadState()

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
  watch: {
    '$route.path'() {
      document.body.classList.remove('show-sidebar')
    }
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
            this.loadNotificationReadState()
            this.fetchNotifications()
            this.startPolling()
            this.checkInvoiceLimitReminder({ oncePerLogin: true })
          }
        })
        .catch(() => {})
    } catch {}
    try {
      window.addEventListener('invoice-limit-used', this.handleInvoiceLimitUsed)
      window.addEventListener('invoice-limit-check', this.handleInvoiceLimitUsed)
    } catch {}
  },
  beforeDestroy() {
    // Dọn interval polling khi component bị hủy
    this.stopPolling()
    try {
      window.removeEventListener('invoice-limit-used', this.handleInvoiceLimitUsed)
      window.removeEventListener('invoice-limit-check', this.handleInvoiceLimitUsed)
    } catch {}
  },
  methods: {
    // Trả về chữ cái đầu viết hoa từ tên; mặc định là '?'
    nameInitial(name) {
      const n = (name || '').trim()
      return n ? n.charAt(0).toUpperCase() : '?'
    },
    async reloadData() {
      if (this.reloading) return
      this.reloading = true
      const startedAt = Date.now()
      let refreshedCurrentPage = false
      try {
        refreshedCurrentPage = await this.refreshCurrentPage()
        if (!refreshedCurrentPage) this.forceCurrentRouteRefresh()

        const jobs = []
        if (!this.isCompanyPending) jobs.push(this.fetchNotifications())
        jobs.push(this.checkInvoiceLimitReminder({ oncePerLogin: true }))
        await Promise.allSettled(jobs)
      } catch (error) {
        console.debug('Reload current page failed:', error)
        try { this.$toastr && this.$toastr.error('Không thể làm mới dữ liệu') } catch {}
      } finally {
        const elapsed = Date.now() - startedAt
        if (elapsed < 350) await this.delay(350 - elapsed)
        this.reloading = false
      }
    },
    delay(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    forceCurrentRouteRefresh() {
      try {
        window.dispatchEvent(new CustomEvent('app-force-route-refresh', {
          detail: { path: this.$route?.fullPath || window.location.pathname }
        }))
      } catch {}
    },
    async refreshCurrentPage() {
      const parent = this.$parent
      const roots = []
      const pageView = this.normalizeComponentRef(parent?.$refs?.pageView)
      if (pageView) roots.push(pageView)
      if (parent && Array.isArray(parent.$children)) {
        roots.push(...parent.$children.filter(child => child && child !== this))
      }

      const seen = new Set()
      for (const root of roots) {
        if (await this.refreshComponent(root, seen)) return true
      }
      return false
    },
    normalizeComponentRef(ref) {
      return Array.isArray(ref) ? ref[0] : ref
    },
    async refreshComponent(component, seen) {
      if (!component || seen.has(component)) return false
      seen.add(component)

      const methodNames = [
        'reload',
        'refresh',
        'refreshPage',
        'loadStatistics',
        'loadData',
        'fetchList',
        'fetchData',
        'loadList',
        'getData',
        'getList',
        'importData',
        'onRefresh'
      ]
      for (const name of methodNames) {
        if (typeof component[name] === 'function') {
          await component[name]()
          return true
        }
      }

      const refs = component.$refs || {}
      for (const key of Object.keys(refs)) {
        const values = Array.isArray(refs[key]) ? refs[key] : [refs[key]]
        for (const ref of values) {
          if (ref && !seen.has(ref) && typeof ref.refresh === 'function') {
            seen.add(ref)
            await ref.refresh()
            return true
          }
        }
      }

      const children = component.$children || []
      for (const child of children) {
        if (await this.refreshComponent(child, seen)) return true
      }
      return false
    },
    menuToggle() {
      const isMobile = window.matchMedia && window.matchMedia('(max-width: 768px)').matches
      if (isMobile) {
        document.body.classList.remove('nav-sm', 'nav-md')
        document.body.classList.toggle('show-sidebar')
        return
      }
      document.body.classList.toggle('nav-sm')
      document.body.classList.remove('show-sidebar')
      document.body.classList.toggle('nav-md', !document.body.classList.contains('nav-sm'))
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
        this.clearInvoiceLimitPopupKeys();
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
          const id = r.id ?? r._id ?? r.uuid ?? [created || '', String(title).trim(), uid ?? 'system'].join(':')
          return {
            id,
            title: String(title).trim(),
            description: r.description ?? r.detail ?? '',
            username: hasUser ? (String(uname || '').trim() || 'Hệ thống') : 'Hệ thống',
            created_at: created,
            avatar: r.avatar ?? null,
          }
        })
        if (this.notificationsOpen) this.markNotificationsRead()

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
    resolveNotificationReadKey() {
      let tokenHash = ''
      try {
        const path = window.location?.pathname || ''
        const isAdmin = /administrator|admin/.test(path)
        const primaryKey = isAdmin ? 'token-admin' : 'token'
        const altKey = isAdmin ? 'token' : 'token-admin'
        const token = localStorage.getItem(primaryKey) || localStorage.getItem(altKey) || ''
        if (token) {
          let hash = 0
          for (let i = 0; i < token.length; i += 1) {
            hash = ((hash * 31) + token.charCodeAt(i)) >>> 0
          }
          tokenHash = hash.toString(36)
        }
      } catch {}
      const context = this.isCustomerContext() ? 'customer' : 'admin'
      const account = tokenHash || this.app.auth.username || this.$app?.info?.user?.username || 'anonymous'
      return `notification-read:${context}:${account}`
    },
    normalizeNotificationKey(item) {
      if (!item) return ''
      const raw = item.id ?? [item.created_at || '', item.title || '', item.username || ''].join(':')
      return String(raw || '').trim()
    },
    loadNotificationReadState() {
      try {
        const raw = localStorage.getItem(this.resolveNotificationReadKey())
        const parsed = raw ? JSON.parse(raw) : []
        this.readNotificationKeys = Array.isArray(parsed) ? parsed.map(String).filter(Boolean) : []
      } catch {
        this.readNotificationKeys = []
      }
    },
    saveNotificationReadState(keys) {
      const unique = Array.from(new Set((keys || []).map(String).filter(Boolean))).slice(-200)
      this.readNotificationKeys = unique
      try {
        localStorage.setItem(this.resolveNotificationReadKey(), JSON.stringify(unique))
      } catch {}
    },
    isNotificationRead(item) {
      const key = this.normalizeNotificationKey(item)
      return key ? this.readNotificationKeys.includes(key) : false
    },
    markNotificationsRead() {
      const currentKeys = this.list.map(item => this.normalizeNotificationKey(item)).filter(Boolean)
      if (currentKeys.length === 0) return
      this.saveNotificationReadState([...(this.readNotificationKeys || []), ...currentKeys])
    },
    handleNotificationsShown() {
      this.notificationsOpen = true
      this.markNotificationsRead()
    },
    handleNotificationsHidden() {
      this.notificationsOpen = false
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
    handleInvoiceLimitUsed(event) {
      const oncePerThreshold = event?.type === 'invoice-limit-used'
      const oncePerLogin = event?.type === 'invoice-limit-check'
      this.checkInvoiceLimitReminder({ oncePerThreshold, oncePerLogin })
    },
    isCustomerContext() {
      try {
        const path = window.location?.pathname || ''
        return !path.startsWith('/administrator') && !path.startsWith('/auth/login-admin')
      } catch {
        return true
      }
    },
    resolveCompanyId() {
      try {
        const id = this.$app?.info?.company?.id
        if (id != null && id !== '') return id
      } catch {}
      return 'current'
    },
    resolveLoginKey(companyId, total, threshold) {
      let tokenHash = 'no-token'
      try {
        const token = localStorage.getItem('token') || ''
        if (token) {
          let hash = 0
          for (let i = 0; i < token.length; i += 1) {
            hash = ((hash * 31) + token.charCodeAt(i)) >>> 0
          }
          tokenHash = hash.toString(36)
        }
      } catch {}
      return `invoice-limit-login-popup:${companyId}:${total}:${threshold}:${tokenHash}`
    },
    async checkInvoiceLimitReminder(options = {}) {
      if (this.isCompanyPending || !this.isCustomerContext()) return
      try {
        const { data } = await axios.get('/dashboard/stats', {
          meta: { suppressGlobalErrorToast: true }
        })
        const total = Number(data?.totalInvoices || 0)
        const used = Number(data?.usedInvoices || 0)
        if (total <= 0 || used <= 0) return

        const remaining = Math.max(0, total - used)
        const percent = Math.floor((used / total) * 100)
        const threshold = [100, 75, 50].find(level => percent >= level)
        if (!threshold) return

        const companyId = this.resolveCompanyId()
        const signStorageKey = `invoice-limit-sign-popup:${companyId}:${total}:${threshold}`
        const loginStorageKey = this.resolveLoginKey(companyId, total, threshold)
        if (options.oncePerThreshold && sessionStorage.getItem(signStorageKey)) return
        if (options.oncePerLogin && sessionStorage.getItem(loginStorageKey)) return

        if (options.oncePerThreshold) {
          sessionStorage.setItem(signStorageKey, new Date().toISOString())
        }
        if (options.oncePerLogin) {
          sessionStorage.setItem(loginStorageKey, new Date().toISOString())
        }
        this.showInvoiceLimitReminder({ threshold, percent, total, used, remaining })
      } catch {}
    },
    showInvoiceLimitReminder({ threshold, percent, total, used, remaining }) {
      const usedText = new Intl.NumberFormat('vi-VN').format(used)
      const totalText = new Intl.NumberFormat('vi-VN').format(total)
      const remainingText = new Intl.NumberFormat('vi-VN').format(remaining)
      const exactThreshold = percent === threshold
      const actionText = exactThreshold ? 'đạt' : 'vượt'
      const title = `Cảnh báo ${actionText} mốc ${threshold}% hạn mức hóa đơn`
      const message = exactThreshold && threshold >= 100
        ? `Bạn đã sử dụng hết ${usedText}/${totalText} hóa đơn, đạt mốc 100% hạn mức. Vui lòng mua thêm gói hóa đơn để tiếp tục phát hành.`
        : exactThreshold
        ? `Bạn đã sử dụng ${usedText}/${totalText} hóa đơn (${percent}%), đạt mốc ${threshold}% hạn mức. Còn lại ${remainingText} hóa đơn, vui lòng cân nhắc mua thêm.`
        : `Bạn đã sử dụng ${usedText}/${totalText} hóa đơn (${percent}%), đã vượt mốc ${threshold}%. Còn lại ${remainingText} hóa đơn, vui lòng cân nhắc mua thêm.`
      const variant = threshold >= 100 ? 'danger' : threshold >= 75 ? 'warning' : 'info'
      this.limitReminder = {
        show: true,
        threshold,
        percent,
        total,
        used,
        remaining,
        title,
        message,
        icon: threshold >= 100 ? 'fas fa-exclamation-triangle' : 'fas fa-bell',
        variant,
        progressVariant: variant
      }
    },
    closeLimitReminder() {
      this.limitReminder.show = false
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    },
    clearInvoiceLimitPopupKeys() {
      try {
        Object.keys(sessionStorage)
          .filter(key => key.startsWith('invoice-limit-popup:') || key.startsWith('invoice-limit-sign-popup:') || key.startsWith('invoice-limit-login-popup:'))
          .forEach(key => sessionStorage.removeItem(key))
      } catch {}
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
/* Kiểu thanh điều hướng trên đã tinh chỉnh */
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

/* Kiểu mẫu chuông thông báo */
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

/* Kiểu tiêu đề popover */
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

.invoice-limit-modal .modal-content {
  border: 0;
  border-radius: 8px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
}

.invoice-limit-modal .modal-header {
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
}

.limit-reminder {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.limit-reminder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  border-radius: 8px;
  font-size: 1.25rem;
}

.limit-reminder-icon.info {
  background: #e8f7ff;
  color: #0984e3;
}

.limit-reminder-icon.warning {
  background: #fff7e6;
  color: #d97706;
}

.limit-reminder-icon.danger {
  background: #fff1f2;
  color: #dc2626;
}

.limit-reminder-body {
  flex: 1;
  min-width: 0;
}

.limit-reminder-body h5 {
  margin: 0 0 8px;
  font-weight: 800;
  color: #1f2937;
}

.limit-reminder-body p {
  margin: 0 0 14px;
  color: #4b5563;
  line-height: 1.55;
}

.limit-reminder-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.limit-reminder-stats div {
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
}

.limit-reminder-stats span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.limit-reminder-stats strong {
  display: block;
  margin-top: 2px;
  color: #1f2937;
  font-size: 1rem;
}

@media (max-width: 768px) {
  .b-sidebar#sidebar-right { max-height: 100vh !important; }
  .b-sidebar#sidebar-right .list-unstyled { max-height: calc(100vh - 56px) !important; }
  .limit-reminder {
    display: block;
  }
  .limit-reminder-icon {
    margin-bottom: 12px;
  }
  .limit-reminder-stats {
    grid-template-columns: 1fr;
  }
}
</style>
