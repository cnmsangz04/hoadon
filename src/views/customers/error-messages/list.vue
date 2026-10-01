<template>
  <div class="container-fluid py-3 error-messages">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Danh sách thông báo hóa đơn sai sót</h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="fetchList">
          <i class="fas fa-sync-alt"></i> Làm mới
        </b-button>
        <b-button size="sm" variant="success" @click="$router.push({ name: 'CustomerErrorMessageCreate' })">
          <i class="fas fa-plus"></i> Lập thông báo
        </b-button>
      </div>
    </div>

    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="4" class="mb-2">
          <b-input-group>
            <b-input-group-prepend is-text><i class="fas fa-search text-muted"></i></b-input-group-prepend>
            <b-form-input v-model.trim="filters.keyword" placeholder="Tìm theo mã gửi / số TB / tên NNT" @keyup.enter="applyFilters" />
          </b-input-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.notificationType" :options="notificationTypeOptions">
            <template #first><b-form-select-option :value="null">Tất cả loại thông báo</b-form-select-option></template>
          </b-form-select>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-select v-model="filters.status" :options="statusOptions">
            <template #first><b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option></template>
          </b-form-select>
        </b-col>
        <b-col md="2" class="text-right">
          <b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button>
        </b-col>
      </b-row>
      <b-row class="mt-2">
        <b-col md="3" class="mb-2">
          <b-form-datepicker v-model="filters.dateFrom" :max="filters.dateTo || undefined" placeholder="Từ ngày" size="sm" locale="vi" :date-format-options="dateFmt" />
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-datepicker v-model="filters.dateTo" :min="filters.dateFrom || undefined" placeholder="Đến ngày" size="sm" locale="vi" :date-format-options="dateFmt" />
        </b-col>
        <b-col md="6" class="text-right">
          <b-button size="sm" variant="outline-secondary" class="mr-2" @click="resetFilters">Xóa lọc</b-button>
          <b-button size="sm" variant="outline-primary" @click="applyFilters"><i class="fas fa-filter"></i> Áp dụng</b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-card class="shadow-sm">
      <b-table bordered hover responsive small show-empty :busy="isBusy" :items="list.data" :fields="fields" empty-text="Không có dữ liệu">
        <template #cell(index)="{ index }">{{ index + 1 + (list.current_page - 1) * list.per_page }}</template>
        <template #cell(formPattern)="{ item }"><code>{{ item.formPattern || '04/SS-HĐĐT' }}</code></template>
        <template #cell(noticeDate)="{ item }">{{ formatDate(item.noticeDate) }}</template>
        <template #cell(notificationType)="{ item }">{{ notificationTypeText(item.notificationType) }}</template>
        <template #cell(invoiceSummary)="{ item }">{{ invoiceSummary(item) }}</template>
        <template #cell(status)="{ item }"><b-badge :variant="statusVariant(item.status)">{{ statusText(item.status) }}</b-badge></template>
        <template #cell(option)="{ item }">
          <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret boundary="window">
            <template #button-content><i class="fas fa-ellipsis-h"></i></template>
            <b-dropdown-item class="text-center" href="#" @click.prevent="openEdit(item)">Cập nhật</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="viewItem(item)">Xem / In</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="downloadXml(item)">Tải XML</b-dropdown-item>
            <b-dropdown-item class="text-center" href="#" @click.prevent="downloadPdf(item)">Tải PDF</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center" href="#" @click.prevent="signItem(item)">Ký USB token</b-dropdown-item>
            <b-dropdown-item v-if="canSend(item)" class="text-center" href="#" @click.prevent="sendItem(item)">Gửi CQT</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) > 1" class="text-center" href="#" @click.prevent="showHistory(item)">Lịch sử truyền nhận</b-dropdown-item>
            <b-dropdown-item v-if="Number(item.status) === 0" class="text-center text-danger" href="#" @click.prevent="deleteItem(item)">Xóa thông báo</b-dropdown-item>
          </b-dropdown>
        </template>
      </b-table>
      <pagination-bar :current.sync="list.current_page" :size.sync="list.per_page" :total="list.total" :sizes="pageSizes" @page-change="onPageChange" @size-change="onPageSizeChange" />
    </b-card>

    <b-modal ref="viewModal" size="xl" title="Thông báo hóa đơn sai sót" body-class="p-0">
      <iframe v-if="previewHtml" class="preview-frame" :srcdoc="previewHtml"></iframe>
      <template #modal-footer>
        <b-button size="sm" variant="light" @click="$refs.viewModal.hide()">Đóng</b-button>
        <b-button size="sm" variant="primary" @click="printPreview">In</b-button>
      </template>
    </b-modal>

    <b-modal ref="historyModal" size="lg" title="Lịch sử truyền nhận">
      <b-table-simple bordered small responsive>
        <b-thead><b-tr><b-th>#</b-th><b-th>Tiêu đề</b-th><b-th>Mô tả</b-th><b-th>Ngày</b-th></b-tr></b-thead>
        <b-tbody>
          <b-tr v-for="(row, idx) in historyRows" :key="row.id || idx">
            <b-td>{{ idx + 1 }}</b-td><b-td>{{ row.title }}</b-td><b-td>{{ row.description }}</b-td><b-td>{{ formatDateTime(row.createdAt) }}</b-td>
          </b-tr>
          <b-tr v-if="!historyRows.length"><b-td colspan="4" class="text-center">Không có dữ liệu</b-td></b-tr>
        </b-tbody>
      </b-table-simple>
      <template #modal-footer><b-button size="sm" variant="light" @click="$refs.historyModal.hide()">Đóng</b-button></template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'
import { toastError, toastSuccess } from '@/utils/toast'

export default {
  name: 'ErrorMessageList',
  components: { PaginationBar },
  data() {
    return {
      isBusy: false,
      previewId: null,
      previewHtml: '',
      historyRows: [],
      dateFmt: { day: '2-digit', month: '2-digit', year: 'numeric' },
      pageSizes: [10, 20, 50, 100],
      list: { current_page: 1, per_page: 10, total: 0, data: [] },
      filters: { keyword: '', status: null, notificationType: null, dateFrom: null, dateTo: null },
      fields: [
        { key: 'index', label: '#', thStyle: { width: '4%' } },
        { key: 'formPattern', label: 'Mẫu số', thStyle: { width: '11%' } },
        { key: 'noticeDate', label: 'Ngày lập', thStyle: { width: '10%' } },
        { key: 'notificationType', label: 'Loại thông báo', thStyle: { width: '18%' } },
        { key: 'taxResponseNumber', label: 'Số TB CQT', thStyle: { width: '13%' } },
        { key: 'invoiceSummary', label: 'Hóa đơn sai sót', thStyle: { width: '26%' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '9%' } },
        { key: 'option', label: 'Chức năng', thStyle: { width: '9%' } }
      ],
      notificationTypeOptions: [
        { value: 1, text: 'Thông báo của NNT' },
        { value: 2, text: 'Theo thông báo của CQT' }
      ],
      statusOptions: [
        { value: 0, text: 'Khởi tạo' },
        { value: 1, text: 'Đã ký' },
        { value: 2, text: 'Đã gửi' },
        { value: 3, text: 'Hợp lệ' },
        { value: 4, text: 'Lỗi dữ liệu' },
        { value: 5, text: 'CQT tiếp nhận' }
      ]
    }
  },
  created() { this.fetchList() },
  methods: {
    async fetchList() {
      this.isBusy = true
      try {
        const { data } = await axios.get('/error-messages/list', { params: this.query() })
        this.list = { ...this.list, ...data, data: Array.isArray(data.data) ? data.data : [] }
      } finally { this.isBusy = false }
    },
    query() {
      const q = { page: this.list.current_page, size: this.list.per_page }
      if (this.filters.keyword) q.keyword = this.filters.keyword
      if (this.filters.status !== null) q.status = this.filters.status
      if (this.filters.notificationType !== null) q.notificationType = this.filters.notificationType
      if (this.filters.dateFrom) q.dateFrom = this.filters.dateFrom
      if (this.filters.dateTo) q.dateTo = this.filters.dateTo
      return q
    },
    applyFilters() { this.list.current_page = 1; this.fetchList() },
    resetFilters() { this.filters = { keyword: '', status: null, notificationType: null, dateFrom: null, dateTo: null }; this.applyFilters() },
    onPageChange(page) { this.list.current_page = Number(page) || 1; this.fetchList() },
    onPageSizeChange(size) { this.list.per_page = Number(size) || 10; this.list.current_page = 1; this.fetchList() },
    formatDate(v) { return v ? new Date(v).toLocaleDateString('vi-VN') : '—' },
    formatDateTime(v) { return v ? new Date(v).toLocaleString('vi-VN') : '—' },
    notificationTypeText(v) { return Number(v) === 2 ? 'Theo thông báo của CQT' : 'Thông báo của NNT' },
    statusText(v) { return ({ 0: 'Khởi tạo', 1: 'Đã ký', 2: 'Đã gửi', 3: 'Hợp lệ', 4: 'Lỗi dữ liệu', 5: 'CQT tiếp nhận' })[Number(v)] || '—' },
    statusVariant(v) {
      const n = Number(v)
      if ([3, 5].includes(n)) return 'success'
      if (n === 4) return 'danger'
      if (n === 2) return 'warning'
      if (n === 1) return 'primary'
      return 'secondary'
    },
    invoiceSummary(item) {
      const rows = item.invoices || []
      if (!rows.length) return '—'
      return rows.map(r => `${r.serial || ''}${r.invoiceNo ? ' #' + r.invoiceNo : ''}`).join(', ')
    },
    canSend(item) { return [1, 3, 4].includes(Number(item?.status)) },
    openEdit(item) { this.$router.push({ name: 'CustomerErrorMessageEdit', params: { id: item.id } }) },
    async viewItem(item) {
      this.previewId = item.id
      const { data } = await axios.get(`/error-messages/${item.id}/view`, { responseType: 'text', headers: { Accept: 'text/html' } })
      this.previewHtml = data || ''
      this.$refs.viewModal.show()
    },
    printPreview() {
      const frame = this.$el.querySelector('.preview-frame')
      if (frame && frame.contentWindow) frame.contentWindow.print()
    },
    async signItem(item) {
      try {
        const ok = await this.confirm('Xác nhận ký số', `Ký số thông báo #${item.id} bằng USB token?`, 'Ký')
        if (!ok) return
        const { data } = await axios.post(`/error-messages/${item.id}/sign-token/prepare`)
        if (!data?.hash) throw new Error('Không tạo được mã hash ký số')
        this.launchPasigner(data.hash)
        toastSuccess('Đã mở ứng dụng ký số USB token')
      } catch (e) { toastError(e?.response?.data?.message || e?.message || 'Không thể ký số') }
    },
    async downloadXml(item) {
      const { data } = await axios.get(`/error-messages/${item.id}/download-xml`, { responseType: 'text', headers: { Accept: 'application/xml' } })
      this.downloadBlob(data, `thong-bao-sai-sot-${item.id}.xml`, 'application/xml;charset=utf-8')
    },
    async downloadPdf(item) {
      const { data } = await axios.get(`/error-messages/${item.id}/download-pdf`, { responseType: 'blob', headers: { Accept: 'application/pdf' } })
      this.downloadBlob(data, `thong-bao-sai-sot-${item.id}.pdf`, 'application/pdf')
    },
    downloadBlob(data, filename, type) {
      const blob = data instanceof Blob ? data : new Blob([data], { type })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    },
    async sendItem(item) {
      const ok = await this.confirm('Xác nhận gửi CQT', 'Gửi thông báo sai sót lên Cơ quan thuế?', 'Gửi')
      if (!ok) return
      await axios.post(`/error-messages/${item.id}/send`, null, { successMessage: 'Đã gửi thông báo lên CQT' })
      this.fetchList()
    },
    async deleteItem(item) {
      const ok = await this.confirm('Xóa thông báo', `Xóa thông báo #${item.id}?`, 'Xóa', 'danger')
      if (!ok) return
      await axios.delete(`/error-messages/${item.id}`, { successMessage: 'Đã xóa thông báo' })
      this.fetchList()
    },
    async showHistory(item) {
      const { data } = await axios.get(`/error-messages/${item.id}/history`)
      this.historyRows = Array.isArray(data) ? data : []
      this.$refs.historyModal.show()
    },
    launchPasigner(hash) {
      const pid = Math.random().toString(36).slice(2, 8)
      location.href = 'pasigner://' + encodeURIComponent(JSON.stringify({ src: 'web', action: 'signature', pid, data: { hash, domain: window.location.hostname } }))
    },
    confirm(title, content, okTitle, variant = 'primary') {
      return this.$bvModal.msgBoxConfirm(content, { title, size: 'sm', buttonSize: 'sm', okTitle, cancelTitle: 'Hủy', okVariant: variant })
    }
  }
}
</script>

<style scoped>
.error-messages::v-deep .table { table-layout: fixed; }
.error-messages::v-deep th,
.error-messages::v-deep td { overflow-wrap: anywhere; vertical-align: middle; }
.preview-frame { width: 100%; height: 75vh; border: 0; display: block; }
</style>
