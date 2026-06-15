<template>
  <div class="container-fluid py-3 login-history">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Lịch sử đăng nhập</h4>
      <b-button size="sm" variant="outline-primary" @click="reload">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="6" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filters.keyword"
              placeholder="Nhập từ khóa tìm kiếm..."
              @keyup.enter="applyFilters"
            />
          </b-input-group>
        </b-col>
        <b-col md="6" class="mb-2 text-md-right">
          <b-button size="sm" variant="primary" class="mr-2" @click="applyFilters">
            <i class="fas fa-search mr-1"></i>
            Tìm kiếm
          </b-button>
          <b-button size="sm" variant="outline-secondary" @click="resetFilters">
            Xóa lọc
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        responsive
        small
        show-empty
        class="mb-0 table-modern table-compact"
        :busy="isBusy"
        :items="list.data"
        :fields="fields"
        empty-text="Chưa có lịch sử đăng nhập"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(username)="{ item }">
          <span class="font-weight-bold">{{ item.username || '—' }}</span>
        </template>

        <template #cell(ipAddress)="{ item }">
          <code>{{ item.ipAddress || '—' }}</code>
        </template>

        <template #cell(userAgent)="{ item }">
          <span class="browser-text">{{ item.userAgent || '—' }}</span>
        </template>

        <template #cell(loginAt)="{ item }">
          {{ formatDateTime(item.loginAt) }}
        </template>
      </b-table>

      <pagination-bar
        :current.sync="list.current_page"
        :size.sync="list.per_page"
        :total="list.total"
        :sizes="pageSizes"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'SettingLoginHistoryList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      filters: {
        keyword: ''
      },
      pageSizes: [10, 20, 50, 100],
      list: {
        current_page: 1,
        data: [],
        last_page: 1,
        per_page: 10,
        total: 0,
        from: 0,
        to: 0
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '60px' }, tdClass: 'text-center' },
        { key: 'username', label: 'Tài khoản', thStyle: { width: '160px' } },
        { key: 'ipAddress', label: 'IP', thStyle: { width: '150px' } },
        { key: 'userAgent', label: 'Trình duyệt', thStyle: { minWidth: '360px' } },
        { key: 'loginAt', label: 'Ngày', thStyle: { width: '170px' } }
      ]
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.isBusy = true
      try {
        const { data } = await axios.get('/setting/login-history', {
          params: {
            keyword: this.filters.keyword || undefined,
            page: this.list.current_page - 1,
            size: this.list.per_page
          }
        })
        this.list.data = data.data || []
        this.list.total = Number(data.total || 0)
        this.list.per_page = Number(data.per_page || this.list.per_page)
        this.list.current_page = Number(data.current_page || this.list.current_page)
        this.list.last_page = Number(data.last_page || Math.ceil(this.list.total / this.list.per_page) || 1)
        this.list.from = Number(data.from || 0)
        this.list.to = Number(data.to || 0)
      } catch (e) {
        this.list.data = []
        this.list.total = 0
        const message = e?.response?.data?.message || 'Không thể tải lịch sử đăng nhập'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.isBusy = false
      }
    },
    reload() {
      this.fetchList()
    },
    applyFilters() {
      this.list.current_page = 1
      this.fetchList()
    },
    resetFilters() {
      this.filters.keyword = ''
      this.applyFilters()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
      this.fetchList()
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.fetchList()
    },
    formatDateTime(value) {
      if (!value) return '—'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const dd = String(date.getDate()).padStart(2, '0')
      const mm = String(date.getMonth() + 1).padStart(2, '0')
      const yyyy = date.getFullYear()
      const hh = String(date.getHours())
      const mi = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${dd}-${mm}-${yyyy} ${hh}:${mi}:${ss}`
    }
  }
}
</script>

<style scoped>
.login-history .card {
  border-radius: 10px;
}

.login-history .table th {
  background: #f7f9fc;
  color: #4a5568;
  vertical-align: middle;
}

.login-history .table td {
  vertical-align: middle;
}

.browser-text {
  display: block;
  max-width: 720px;
  white-space: normal;
  word-break: break-word;
}
</style>
