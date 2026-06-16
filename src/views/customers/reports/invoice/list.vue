<template>
  <div class="container-fluid py-3 customer-report-invoices">
    <!-- Tiêu đề và thao tác -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">Báo cáo hóa đơn</h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" :disabled="isBusy || !hasData" @click="exportExcel">
          <i class="fas fa-file-excel"></i>
          Xuất Excel
        </b-button>
      </div>
    </div>

    <!-- Bộ lọc -->
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="3" class="mb-2">
          <b-form-group label="Danh mục" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model="filters.category" size="sm">
              <b-form-select-option :value="null">-- Tất cả --</b-form-select-option>
              <b-form-select-option :value="1">Hóa đơn giá trị gia tăng</b-form-select-option>
              <b-form-select-option :value="2">Hóa đơn bán hàng</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-group label="Kỳ báo cáo" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model="filters.periodType" size="sm">
              <b-form-select-option value="month">Theo tháng</b-form-select-option>
              <b-form-select-option value="quarter">Theo quý</b-form-select-option>
              <b-form-select-option value="none">Không theo kỳ</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2" v-if="filters.periodType === 'month'">
          <b-form-group label="Tháng" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model.number="filters.month" size="sm">
              <b-form-select-option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2" v-if="filters.periodType === 'quarter'">
          <b-form-group label="Quý" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model.number="filters.quarter" size="sm">
              <b-form-select-option v-for="q in 4" :key="q" :value="q">Quý {{ q }}</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-group label="Năm" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model.number="filters.year" size="sm">
              <b-form-select-option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-group label="Từ ngày" label-cols="4" label-size="sm" label-align="right">
            <b-form-datepicker
              v-model="filters.fromDate"
              :max="filters.toDate || undefined"
              size="sm"
              locale="vi"
              :disabled="filters.periodType !== 'none'"
              placeholder="Từ ngày"
              :date-format-options="{ day: '2-digit', month: '2-digit', year: 'numeric' }"
            />
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-group label="Đến ngày" label-cols="4" label-size="sm" label-align="right">
            <b-form-datepicker
              v-model="filters.toDate"
              :min="filters.fromDate || undefined"
              size="sm"
              locale="vi"
              :disabled="filters.periodType !== 'none'"
              placeholder="Đến ngày"
              :date-format-options="{ day: '2-digit', month: '2-digit', year: 'numeric' }"
            />
          </b-form-group>
        </b-col>
        <b-col md="3" class="mb-2">
          <b-form-group label="Trạng thái" label-cols="4" label-size="sm" label-align="right">
            <b-form-select v-model="filters.status" size="sm">
              <b-form-select-option :value="null">-- Tất cả --</b-form-select-option>
              <b-form-select-option v-for="(label, key) in statusMap" :key="key" :value="Number(key)">{{ label }}</b-form-select-option>
            </b-form-select>
          </b-form-group>
        </b-col>
        <b-col md="12" class="mb-2 d-flex align-items-center justify-content-end">
          <b-button size="sm" variant="outline-secondary" class="mr-2" @click="resetFilters">Xóa lọc</b-button>
          <b-button size="sm" variant="primary" @click="applyFilters">
            <i class="fas fa-filter"></i>
            Áp dụng
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <b-row class="mb-3 report-summary">
      <b-col md="3" sm="6" class="mb-2">
        <div class="summary-tile summary-count">
          <div class="summary-label">Số hóa đơn</div>
          <div class="summary-value">{{ formatNumber(summary.invoiceCount) }}</div>
        </div>
      </b-col>
      <b-col md="3" sm="6" class="mb-2">
        <div class="summary-tile summary-goods">
          <div class="summary-label">Tổng tiền hàng</div>
          <div class="summary-value">{{ formatNumber(summary.totalBeforeTax) }}</div>
        </div>
      </b-col>
      <b-col md="3" sm="6" class="mb-2">
        <div class="summary-tile summary-tax">
          <div class="summary-label">Tổng tiền thuế</div>
          <div class="summary-value">{{ formatNumber(summary.totalVat) }}</div>
        </div>
      </b-col>
      <b-col md="3" sm="6" class="mb-2">
        <div class="summary-tile summary-total">
          <div class="summary-label">Tổng thanh toán</div>
          <div class="summary-value">{{ formatNumber(summary.totalAmount) }}</div>
        </div>
      </b-col>
    </b-row>

    <!-- Bảng báo cáo hóa đơn -->
    <b-card class="shadow-sm">
      <b-table
        class="customer-report-invoices-table"
        bordered
        hover
        responsive
        small
        show-empty
        :items="list.items"
        :fields="fields"
        :busy="isBusy"
        empty-text="Không có dữ liệu"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (list.current_page - 1) * list.per_page }}
        </template>

        <template #cell(orderCode)="{ item }">
          <span class="text-wrap-anywhere">{{ item.orderCode || item.billCode || '—' }}</span>
        </template>

        <template #cell(formCode)="{ item }">
          <code>{{ buildFormCode(item) }}</code>
        </template>

        <template #cell(no)="{ item }">
          {{ item.no || '—' }}
        </template>

        <template #cell(dateExport)="{ item }">
          {{ formatDate(item.dateExport) }}
        </template>

        <template #cell(customerName)="{ item }">
          <span class="text-wrap-anywhere">{{ item.customerName || '—' }}</span>
        </template>

        <template #cell(paymentType)="{ item }">
          {{ paymentTypeLabel(item.paymentType) }}
        </template>

        <template #cell(status)="{ item }">
          <b-badge :variant="statusVariant(item.status)">{{ statusMap[item.status] || '—' }}</b-badge>
        </template>

        <template #cell(amount)="{ item }">
          <div class="text-right font-weight-bold">
            {{ formatNumber(item.amount) }}
          </div>
        </template>
      </b-table>

      <!-- Khung tải khi chuyển trang -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

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
  name: 'CustomerReportInvoiceList',
  components: { PaginationBar },
  data () {
    const currentYear = new Date().getFullYear()
    return {
      isBusy: false,
      list: {
        current_page: 1,
        items: [],
        last_page: 1,
        per_page: 10,
        total: 0,
        from: 0,
        to: 0
      },
      summary: {
        invoiceCount: 0,
        totalBeforeTax: 0,
        totalVat: 0,
        totalAmount: 0
      },
      pageSizes: [10, 20, 50, 100],
      filters: {
        category: null,
        periodType: 'month',
        month: new Date().getMonth() + 1,
        quarter: 1,
        year: currentYear,
        fromDate: null,
        toDate: null,
        status: null
      },
      statusMap: {
        0: 'Mới khởi tạo',
        1: 'Đã ký',
        2: 'Đã gửi thuế',
        3: 'Đã phát hành',
        4: 'Bị thay thế',
        5: 'Bị điều chỉnh',
        6: 'Đã hủy',
        7: 'Không đủ điều kiện'
      },
      yearOptions: Array.from({ length: 6 }).map((_, i) => currentYear - 3 + i),
      fields: [
        { key: 'index', label: '#', thStyle: { width: '4%' } },
        { key: 'orderCode', label: 'Mã đơn hàng', thStyle: { width: '13%' } },
        { key: 'formCode', label: 'Ký hiệu', thStyle: { width: '10%' } },
        { key: 'no', label: 'Số hóa đơn', thStyle: { width: '8%' } },
        { key: 'dateExport', label: 'Ngày lập', thStyle: { width: '10%' } },
        { key: 'customerName', label: 'Tên đơn vị / khách hàng', thStyle: { width: '19%' } },
        { key: 'paymentType', label: 'Hình thức thanh toán', thStyle: { width: '13%' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '11%' } },
        { key: 'amount', label: 'Tổng tiền', thStyle: { width: '12%' }, tdClass: 'text-right' }
      ]
    }
  },
  computed: {
    hasData () {
      return (this.list.items || []).length > 0
    }
  },
  watch: {
    'filters.periodType' () {
      this.syncDateRangeFromPeriod()
    },
    'filters.month' () {
      this.syncDateRangeFromPeriod()
    },
    'filters.quarter' () {
      this.syncDateRangeFromPeriod()
    },
    'filters.year' () {
      this.syncDateRangeFromPeriod()
    }
  },
  created () {
    this.syncDateRangeFromPeriod()
    this.fetchList()
  },
  methods: {
    buildFormCode (item) {
      const form = item.formCode || ''
      const serial = item.serial || ''
      if (!form && !serial) return '—'
      if (!serial) return form
      return `${form}${serial}`
    },
    syncDateRangeFromPeriod () {
      const year = Number(this.filters.year) || new Date().getFullYear()
      if (this.filters.periodType === 'month') {
        const month = Number(this.filters.month) || (new Date().getMonth() + 1)
        const from = new Date(year, month - 1, 1)
        const to = new Date(year, month, 0)
        this.filters.fromDate = from
        this.filters.toDate = to
      } else if (this.filters.periodType === 'quarter') {
        const q = Number(this.filters.quarter) || 1
        const startMonth = (q - 1) * 3
        const from = new Date(year, startMonth, 1)
        const to = new Date(year, startMonth + 3, 0)
        this.filters.fromDate = from
        this.filters.toDate = to
      } else {
        // Không theo kỳ: cho phép backend dùng fromDate/toDate nếu cần, mặc định để null
        this.filters.fromDate = null
        this.filters.toDate = null
      }
    },
    resetFilters () {
      const currentYear = new Date().getFullYear()
      this.filters = {
        category: null,
        periodType: 'month',
        month: new Date().getMonth() + 1,
        quarter: 1,
        year: currentYear,
        fromDate: null,
        toDate: null,
        status: null
      }
      this.syncDateRangeFromPeriod()
      this.applyFilters()
    },
    buildQueryParams () {
      const params = {
        page: this.list.current_page,
        size: this.list.per_page
      }
      if (this.filters.category != null) params.category = this.filters.category
      if (this.filters.status != null) params.status = this.filters.status
      if (this.filters.periodType === 'month') {
        params.periodType = 'month'
        params.month = this.filters.month
        params.year = this.filters.year
      } else if (this.filters.periodType === 'quarter') {
        params.periodType = 'quarter'
        params.quarter = this.filters.quarter
        params.year = this.filters.year
      } else {
        params.periodType = 'none'
        const fmt = v => {
          if (!v) return null
          try {
            const d = typeof v === 'string' ? new Date(v) : v
            const yyyy = d.getFullYear()
            const mm = String(d.getMonth() + 1).padStart(2, '0')
            const dd = String(d.getDate()).padStart(2, '0')
            return `${yyyy}-${mm}-${dd}`
          } catch (e) {
            return null
          }
        }
        const df = fmt(this.filters.fromDate)
        const dt = fmt(this.filters.toDate)
        if (df) params.fromDate = df
        if (dt) params.toDate = dt
      }
      return params
    },
    normalizeSummary (summary) {
      return {
        invoiceCount: Number(summary?.invoiceCount || 0) || 0,
        totalBeforeTax: Number(summary?.totalBeforeTax || 0) || 0,
        totalVat: Number(summary?.totalVat || 0) || 0,
        totalAmount: Number(summary?.totalAmount || 0) || 0
      }
    },
    normalizePageResponse (raw) {
      const list = { ...this.list }
      if (raw && Array.isArray(raw.items)) {
        list.items = raw.items
        list.current_page = Number(raw.current_page || list.current_page) || 1
        list.per_page = Number(raw.per_page || list.per_page) || 10
        list.total = Number(raw.total || raw.items.length) || 0
        list.last_page = Number(raw.last_page || Math.ceil(list.total / list.per_page) || 1)
        list.from = list.total === 0 ? 0 : ((list.current_page - 1) * list.per_page) + 1
        const count = raw.items.length
        list.to = list.total === 0 ? 0 : (list.from + count - 1)
        return list
      }
      if (Array.isArray(raw)) {
        list.items = raw
        list.total = raw.length
        list.current_page = 1
        list.last_page = 1
        list.from = raw.length ? 1 : 0
        list.to = raw.length
        return list
      }
      return list
    },
    async fetchList () {
      this.isBusy = true
      try {
        const params = this.buildQueryParams()
        const { data } = await axios.get('/reports/invoices', { params })
        this.list = this.normalizePageResponse(data)
        this.summary = this.normalizeSummary(data?.summary)
      } catch (e) {
        this.$bvToast && this.$bvToast.toast('Không thể tải dữ liệu báo cáo hóa đơn', { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      } finally {
        this.isBusy = false
      }
    },
    onPageSizeChange (size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange (page) {
      this.list.current_page = Number(page) || 1
      this.fetchList()
    },
    applyFilters () {
      this.list.current_page = 1
      this.fetchList()
    },
    reload () {
      this.fetchList()
    },
    formatDate (val) {
      if (!val) return '—'
      try {
        const d = typeof val === 'string' ? new Date(val) : val
        const dd = String(d.getDate()).padStart(2, '0')
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const yyyy = d.getFullYear()
        return `${dd}/${mm}/${yyyy}`
      } catch (e) {
        return String(val)
      }
    },
    formatNumber (val) {
      if (val == null || val === '') return ''
      const n = Number(val)
      if (Number.isNaN(n)) return String(val)
      return n.toLocaleString('vi-VN')
    },
    paymentTypeLabel (t) {
      const m = {
        1: 'Tiền mặt',
        2: 'Chuyển khoản',
        3: 'Tiền mặt/Chuyển khoản'
      }
      return m[Number(t)] || ''
    },
    statusVariant (s) {
      const n = Number(s)
      if (n === 3) return 'success'
      if (n === 6 || n === 4 || n === 5 || n === 7) return 'danger'
      if (n === 2) return 'warning'
      if (n === 1) return 'info'
      return 'secondary'
    },
    async exportExcel () {
      this.isBusy = true
      try {
        const params = this.buildQueryParams()
        const res = await axios.get('/reports/invoices/export', {
          params,
          responseType: 'blob'
        })
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        const ts = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
        a.download = `bao-cao-hoa-don-${ts}.xlsx`
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
      } catch (e) {
        this.$bvToast && this.$bvToast.toast('Không thể xuất Excel báo cáo hóa đơn', { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      } finally {
        this.isBusy = false
      }
    }
  }
}
</script>

<style scoped>
.customer-report-invoices {
  width: 100%;
  max-width: none;
}

.customer-report-invoices::v-deep .table-responsive {
  overflow-x: hidden;
}

.customer-report-invoices::v-deep .customer-report-invoices-table {
  width: 100%;
  table-layout: fixed;
}

.customer-report-invoices::v-deep .customer-report-invoices-table th,
.customer-report-invoices::v-deep .customer-report-invoices-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}

.text-wrap-anywhere {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.summary-tile {
  min-height: 86px;
  border: 1px solid #e5e7eb;
  border-left-width: 4px;
  border-radius: 6px;
  background: #fff;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-label {
  color: #6c757d;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.summary-value {
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.25;
  margin-top: 6px;
  overflow-wrap: anywhere;
}

.summary-count { border-left-color: #2563eb; }
.summary-goods { border-left-color: #059669; }
.summary-tax { border-left-color: #d97706; }
.summary-total { border-left-color: #7c3aed; }
</style>
