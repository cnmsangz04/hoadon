<template>
  <div class="container-fluid py-3 mail-history">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Lịch sử gửi mail</h4>
      <b-button size="sm" variant="outline-primary" @click="reload">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="5" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text>
              <i class="fas fa-search text-muted"></i>
            </b-input-group-prepend>
            <b-form-input
              v-model.trim="filters.keyword"
              placeholder="Tìm kiếm tên người nhận, email, tiêu đề..."
              @keyup.enter="applyFilters"
            />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions" />
        </b-col>
        <b-col md="4" class="mb-2 text-md-right">
          <b-button size="sm" variant="primary" class="mr-2" @click="applyFilters">
            <i class="fas fa-filter mr-1"></i>
            Áp dụng
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
        :busy="isBusy"
        :items="list.data"
        :fields="fields"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(templateKey)="{ item }">
          <div class="font-weight-bold">{{ item.templateName || mailTypeText(item.templateKey) }}</div>
          <small class="text-muted">{{ item.subject || item.templateName || '—' }}</small>
        </template>

        <template #cell(sender)="">
          <span>Hệ thống</span>
        </template>

        <template #cell(toName)="{ item }">
          {{ item.toName || '—' }}
        </template>

        <template #cell(toEmail)="{ item }">
          <b-badge variant="success" class="mr-1">To</b-badge>
          <span>{{ item.toEmail || '—' }}</span>
        </template>

        <template #cell(attempts)="{ item }">
          <strong>{{ item.attempts || 0 }}</strong>
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">
            {{ statusText(item.status) }}
          </b-badge>
        </template>

        <template #cell(updatedAt)="{ item }">
          {{ formatDateTime(item.sentAt || item.failedAt || item.updatedAt || item.createdAt) }}
        </template>

        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content>
              <i class="fas fa-ellipsis-h"></i>
            </template>
            <b-dropdown-item href="#" @click.prevent="showDetail(item)">
              Chi tiết
            </b-dropdown-item>
            <b-dropdown-divider></b-dropdown-divider>
            <b-dropdown-item href="#" :disabled="retryingId === item.id" @click.prevent="retryMail(item)">
              <i class="fas fa-paper-plane mr-1"></i>
              Gửi lại
            </b-dropdown-item>
          </b-dropdown>
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

    <b-modal ref="detailModal" title="Chi tiết gửi mail" ok-only ok-title="Đóng" size="lg">
      <b-table-simple small bordered responsive>
        <b-tbody>
          <b-tr><b-th style="width: 180px">Người nhận</b-th><b-td>{{ selected.toName || '—' }}</b-td></b-tr>
          <b-tr><b-th>Email nhận</b-th><b-td>{{ selected.toEmail || '—' }}</b-td></b-tr>
          <b-tr><b-th>Tiêu đề</b-th><b-td>{{ selected.subject || '—' }}</b-td></b-tr>
          <b-tr><b-th>Trạng thái</b-th><b-td>{{ statusText(selected.status) }}</b-td></b-tr>
          <b-tr><b-th>Số lần gửi</b-th><b-td>{{ selected.attempts || 0 }}</b-td></b-tr>
          <b-tr><b-th>Hóa đơn</b-th><b-td>{{ selected.invoiceId || '—' }}</b-td></b-tr>
          <b-tr><b-th>Ngày tạo</b-th><b-td>{{ formatDateTime(selected.createdAt) }}</b-td></b-tr>
          <b-tr><b-th>Ngày gửi</b-th><b-td>{{ formatDateTime(selected.sentAt) }}</b-td></b-tr>
          <b-tr><b-th>Lỗi</b-th><b-td class="text-danger">{{ selected.error || '—' }}</b-td></b-tr>
        </b-tbody>
      </b-table-simple>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'EmailMailHistory',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      retryingId: null,
      selected: {},
      filters: {
        keyword: '',
        status: null,
      },
      statusOptions: [
        { value: null, text: 'Tất cả trạng thái' },
        { value: 'queued', text: 'Chờ gửi' },
        { value: 'processing', text: 'Đang gửi' },
        { value: 'retry', text: 'Chờ gửi lại' },
        { value: 'sent', text: 'Đã gửi' },
        { value: 'failed', text: 'Gửi lỗi' },
      ],
      pageSizes: [10, 20, 50, 100],
      list: {
        current_page: 1,
        data: [],
        last_page: 1,
        per_page: 10,
        total: 0,
        from: 0,
        to: 0,
      },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '70px' }, tdClass: 'text-center' },
        { key: 'templateKey', label: 'Loại email', thStyle: { minWidth: '220px' } },
        { key: 'sender', label: 'Người gửi', thStyle: { width: '130px' } },
        { key: 'toName', label: 'Người nhận', thStyle: { minWidth: '200px' } },
        { key: 'toEmail', label: 'Email nhận', thStyle: { minWidth: '260px' } },
        { key: 'attempts', label: 'Số lần gửi', thStyle: { width: '110px' }, tdClass: 'text-center' },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '130px' }, tdClass: 'text-center' },
        { key: 'updatedAt', label: 'Ngày cập nhật', thStyle: { width: '170px' } },
        { key: 'option', label: '', thStyle: { width: '70px' }, tdClass: 'text-center' },
      ],
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.isBusy = true
      try {
        const { data } = await axios.get('/mail-jobs', {
          params: {
            page: this.list.current_page,
            size: this.list.per_page,
            q: this.filters.keyword || undefined,
            status: this.filters.status || undefined,
          },
        })
        const rows = data.items || data.data || []
        this.list.data = rows
        this.list.total = data.total || 0
        this.list.per_page = data.per_page || this.list.per_page
        this.list.current_page = data.current_page || this.list.current_page
        this.list.last_page = data.last_page || 1
        this.list.from = rows.length ? (this.list.current_page - 1) * this.list.per_page + 1 : 0
        this.list.to = rows.length ? this.list.from + rows.length - 1 : 0
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
      this.filters = { keyword: '', status: null }
      this.applyFilters()
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
      this.fetchList()
    },
    showDetail(item) {
      this.selected = item || {}
      this.$refs.detailModal.show()
    },
    async retryMail(item) {
      if (!item || !item.id) return
      const ok = await this.$bvModal.msgBoxConfirm(
        `Đưa email gửi đến ${item.toEmail || 'người nhận này'} vào hàng đợi gửi lại?`,
        {
          title: 'Gửi lại email',
          okTitle: 'Gửi lại',
          cancelTitle: 'Hủy',
          okVariant: 'primary',
          centered: true,
        }
      )
      if (!ok) return

      this.retryingId = item.id
      try {
        const { data } = await axios.post(`/mail-jobs/${item.id}/retry`)
        this.$bvToast.toast(data?.message || 'Đã đưa email vào hàng đợi gửi lại', {
          title: 'Thành công',
          variant: 'success',
          solid: true,
        })
        this.fetchList()
      } catch (err) {
        const message = err?.response?.data?.message || 'Không thể gửi lại email'
        this.$bvToast.toast(message, {
          title: 'Lỗi',
          variant: 'danger',
          solid: true,
        })
      } finally {
        this.retryingId = null
      }
    },
    mailTypeText(key) {
      if (key === 'ISSUE_INVOICE_MAIL') return 'Thông báo phát hành hóa đơn'
      if (key === 'ACCOUNT_INFO_MAIL') return 'Gửi thông tin tài khoản'
      if (key === 'LOGIN_INFO_MAIL') return 'Gửi thông tin đăng nhập'
      return key || 'Email hệ thống'
    },
    statusText(status) {
      switch (status) {
        case 'queued': return 'Chờ gửi'
        case 'processing': return 'Đang gửi'
        case 'retry': return 'Chờ gửi lại'
        case 'sent': return 'Đã gửi'
        case 'failed': return 'Gửi lỗi'
        default: return 'Không rõ'
      }
    },
    statusVariant(status) {
      switch (status) {
        case 'sent': return 'success'
        case 'failed': return 'danger'
        case 'processing': return 'info'
        case 'retry': return 'warning'
        case 'queued': return 'secondary'
        default: return 'light'
      }
    },
    formatDateTime(value) {
      if (!value) return '—'
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
  },
}
</script>

<style scoped>
.mail-history .table th {
  background: #eef4fa;
  vertical-align: middle;
}

.mail-history .table td {
  vertical-align: middle;
}
</style>
