<template>
  <div class="container-fluid py-3 customer-report-invoices">
    <!-- Header v� thao t�c -->
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

    <!-- B? l?c -->
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
              :disabled="true"
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
              :disabled="true"
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

    <!-- B?ng b�o c�o h�a don -->
    <b-card class="shadow-sm">
      <b-table
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
          <span>{{ item.orderCode || item.billCode || '—' }}</span>
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
          {{ item.customerName || '—' }}
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

      <!-- Skeleton t?i khi chuy?n trang -->
      <div v-if="isBusy" class="mt-2">
        <b-skeleton width="100%" height="20px" animated class="mb-2" />
        <b-skeleton width="96%" height="20px" animated class="mb-2" />
        <b-skeleton width="92%" height="20px" animated class="mb-2" />
      </div>

      <b-row class="mt-2">
        <b-col cols="6">
          <b-form inline>
            <b-form-select
              size="sm"
              class="d-inline-block mb-2 mr-2 pl-2 pr-4"
              v-model.number="list.per_page"
              :options="pageSizes"
              @input="onPageSizeChange"
            />
            <div class="pt-1 text-muted">
              <i class="fas fa-globe mr-1"></i> Hiển thị từ
              <b class="pl-1 pr-2">{{ list.from || 0 }}</b>
              đến
              <b class="pl-1 pr-2">{{ list.to || 0 }}</b>
              trong tổng số
              <b class="pl-1 pr-2">{{ list.total || 0 }}</b>
              bản ghi.
            </div>
          </b-form>
        </b-col>
        <b-col cols="6">
          <b-pagination
            align="right"
            v-model.number="list.current_page"
            :per-page="list.per_page"
            :total-rows="list.total"
            :hide-goto-end-buttons="true"
            v-if="list.last_page > 1"
            size="sm"
            pills
            @input="onPageChange"
          />
        </b-col>
      </b-row>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerReportInvoiceList',
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
        { key: 'index', label: '#', thStyle: { width: '50px' } },
        { key: 'orderCode', label: 'Mã đơn hàng', thStyle: { width: '140px' } },
        { key: 'formCode', label: 'Ký hiệu', thStyle: { width: '120px' } },
        { key: 'no', label: 'Số hóa đơn', thStyle: { width: '110px' } },
        { key: 'dateExport', label: 'Ngày lập', thStyle: { width: '120px' } },
        { key: 'customerName', label: 'Tên đơn vị / khách hàng', thStyle: { width: '150px' } },
        { key: 'paymentType', label: 'Hình thức thanh toán', thStyle: { width: '120px' } },
        { key: 'status', label: 'Trạng thái', thStyle: { width: '130px' } },
        { key: 'amount', label: 'Tổng tiền', thStyle: { width: '140px' }, tdClass: 'text-right' }
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
      } catch (e) {
        this.$bvToast && this.$bvToast.toast('Không thể tải dữ liệu báo cáo hóa đơn', { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      } finally {
        this.isBusy = false
      }
    },
    onPageSizeChange () {
      this.list.current_page = 1
      this.fetchList()
    },
    onPageChange () {
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
  max-width: 1200px;
  margin: 0 auto;
}
</style>