<template>
  <div class="container-fluid py-3 login-sessions">
    <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap">
      <div>
        <h4 class="mb-1 font-weight-bold">Phiên đăng nhập</h4>
      </div>
      <div class="mt-2 mt-md-0">
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="fetchList">
          <i class="fas fa-sync-alt mr-1"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="danger" :disabled="loggingOutOthers || activeOtherCount === 0" @click="logoutOthers">
          <b-spinner v-if="loggingOutOthers" small class="mr-1"></b-spinner>
          Đăng xuất thiết bị khác
        </b-button>
      </div>
    </div>

    <b-card class="shadow-sm sessions-card">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        :busy="isBusy"
        :items="items"
        :fields="fields"
        empty-text="Không có phiên đăng nhập"
        class="mb-0 table-modern sessions-table"
      >
        <template #cell(device)="{ item }">
          <div class="font-weight-bold">
            <i :class="deviceIcon(item.userAgent)" class="mr-1"></i>
            {{ deviceName(item.userAgent) }}
            <b-badge v-if="item.current" variant="primary" class="ml-1">Hiện tại</b-badge>
          </div>
          <small class="text-muted d-block user-agent">{{ item.userAgent || '-' }}</small>
        </template>

        <template #cell(ipAddress)="{ item }">
          <span class="text-monospace">{{ item.ipAddress || '-' }}</span>
        </template>

        <template #cell(loginType)="{ item }">
          <b-badge :variant="item.loginType === 'ADMIN' ? 'info' : 'secondary'">
            {{ item.loginType === 'ADMIN' ? 'Admin' : 'Người dùng' }}
          </b-badge>
        </template>

        <template #cell(active)="{ item }">
          <b-badge :variant="item.active ? 'success' : 'secondary'">
            {{ item.active ? 'Đang hoạt động' : 'Đã kết thúc' }}
          </b-badge>
          <div v-if="item.revokedReason" class="small text-muted mt-1">{{ revokeText(item.revokedReason) }}</div>
        </template>

        <template #cell(lastSeenAt)="{ item }">
          <div>{{ formatDateTime(item.lastSeenAt || item.issuedAt) }}</div>
          <small class="text-muted">Tạo: {{ formatDateTime(item.issuedAt) }}</small>
        </template>

        <template #cell(expiresAt)="{ item }">
          {{ formatDateTime(item.expiresAt) }}
        </template>

        <template #cell(option)="{ item }">
          <b-button
            size="sm"
            variant="outline-danger"
            :disabled="!item.active || revokingId === item.sessionId"
            @click="revoke(item)"
          >
            <b-spinner v-if="revokingId === item.sessionId" small></b-spinner>
            <span v-else>{{ item.current ? 'Đăng xuất' : 'Đăng xuất' }}</span>
          </b-button>
        </template>
      </b-table>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'SettingLoginSessions',
  data() {
    return {
      isBusy: false,
      loggingOutOthers: false,
      revokingId: null,
      items: [],
      fields: [
        { key: 'device', label: 'Thiết bị', thStyle: { minWidth: '260px' } },
        { key: 'ipAddress', label: 'IP', thStyle: { width: '130px' } },
        { key: 'loginType', label: 'Loại', thStyle: { width: '105px' }, tdClass: 'text-center' },
        { key: 'active', label: 'Trạng thái', thStyle: { width: '150px' }, tdClass: 'text-center' },
        { key: 'lastSeenAt', label: 'Hoạt động gần nhất', thStyle: { width: '170px' } },
        { key: 'expiresAt', label: 'Hết hạn', thStyle: { width: '145px' } },
        { key: 'option', label: '', thStyle: { width: '110px' }, tdClass: 'text-center text-nowrap' },
      ],
    }
  },
  computed: {
    activeOtherCount() {
      return this.items.filter(item => item.active && !item.current).length
    },
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.isBusy = true
      try {
        const { data } = await axios.get('/setting/sessions')
        this.items = data.items || []
      } finally {
        this.isBusy = false
      }
    },
    async logoutOthers() {
      if (this.loggingOutOthers) return
      this.loggingOutOthers = true
      try {
        const { data } = await axios.post('/setting/sessions/logout-others')
        this.toastSuccess(data?.message || 'Đã đăng xuất thiết bị khác')
        await this.fetchList()
      } finally {
        this.loggingOutOthers = false
      }
    },
    async revoke(item) {
      if (!item?.sessionId || this.revokingId) return
      this.revokingId = item.sessionId
      try {
        const { data } = await axios.delete(`/setting/sessions/${encodeURIComponent(item.sessionId)}`)
        this.toastSuccess(data?.message || 'Đã đăng xuất phiên')
        if (data?.current) {
          this.clearTokens()
          window.location.href = this.isAdminContext() ? '/auth/login-admin' : '/auth/login'
          return
        }
        await this.fetchList()
      } finally {
        this.revokingId = null
      }
    },
    clearTokens() {
      try {
        localStorage.removeItem('token')
        localStorage.removeItem('token-admin')
        localStorage.removeItem('last-account')
        localStorage.removeItem('last-admin-account')
      } catch {}
    },
    isAdminContext() {
      try {
        const path = window.location?.pathname || ''
        return path.startsWith('/administrator') || localStorage.getItem('token-admin')
      } catch {
        return false
      }
    },
    deviceName(userAgent) {
      const ua = String(userAgent || '')
      const browser = /Edg\//.test(ua) ? 'Edge'
        : /Chrome\//.test(ua) ? 'Chrome'
        : /Firefox\//.test(ua) ? 'Firefox'
        : /Safari\//.test(ua) ? 'Safari'
        : 'Trình duyệt'
      const os = /Windows/.test(ua) ? 'Windows'
        : /Mac OS|Macintosh/.test(ua) ? 'macOS'
        : /Android/.test(ua) ? 'Android'
        : /iPhone|iPad/.test(ua) ? 'iOS'
        : /Linux/.test(ua) ? 'Linux'
        : ''
      return os ? `${browser} trên ${os}` : browser
    },
    deviceIcon(userAgent) {
      const ua = String(userAgent || '')
      if (/Android|iPhone|iPad/.test(ua)) return 'fas fa-mobile-alt'
      return 'fas fa-desktop'
    },
    revokeText(reason) {
      if (reason === 'LOGOUT_OTHERS') return 'Đăng xuất từ thiết bị khác'
      if (reason === 'USER_REVOKED') return 'Người dùng đăng xuất'
      return reason
    },
    formatDateTime(value) {
      if (!value) return '-'
      try {
        const d = new Date(value)
        if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ')
        const dd = String(d.getDate()).padStart(2, '0')
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const yyyy = d.getFullYear()
        const hh = String(d.getHours()).padStart(2, '0')
        const mi = String(d.getMinutes()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy} ${hh}:${mi}`
      } catch {
        return String(value)
      }
    },
    toastSuccess(message) {
      try {
        if (this.$toastr) this.$toastr.success(message)
        else if (window.toastr) window.toastr.success(message)
      } catch {}
    },
  },
}
</script>

<style scoped>
.login-sessions {
  max-width: 100%;
}

.sessions-card ::v-deep .table-responsive {
  overflow-x: auto;
}

.sessions-table td,
.sessions-table th {
  vertical-align: middle;
}

.user-agent {
  max-width: 460px;
  overflow-wrap: anywhere;
}
</style>
