<template>
  <div class="administrators-index">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div>
        <h4 class="mb-1 font-weight-bold">Thống kê mua gói hóa đơn</h4>
      </div>
      <b-button size="sm" variant="success" :disabled="exporting" @click="exportExcel">
        <i class="fas fa-file-excel mr-1"></i>
        Xuất Excel
      </b-button>
    </div>

    <b-card class="mb-3 shadow-sm filter-card">
      <b-row>
        <b-col md="3" class="mb-2">
          <label class="filter-label">Từ tháng</label>
          <b-form-input v-model="filters.startMonth" type="month" />
        </b-col>
        <b-col md="3" class="mb-2">
          <label class="filter-label">Đến tháng</label>
          <b-form-input v-model="filters.endMonth" type="month" />
        </b-col>
        <b-col md="3" class="mb-2">
          <label class="filter-label">Công ty</label>
          <v-select v-model="filters.companyId" :options="companyOptions" label="label" :reduce="c => c.value" placeholder="Tất cả công ty" />
        </b-col>
        <b-col md="2" class="mb-2">
          <label class="filter-label">Thanh toán</label>
          <b-form-select v-model="filters.paymentMethod" :options="paymentMethodOptions" />
        </b-col>
        <b-col md="1" class="mb-2 d-flex align-items-end">
          <b-button size="sm" variant="primary" block @click="loadStatistics">
            Lọc
          </b-button>
        </b-col>
      </b-row>
    </b-card>

    <div v-if="loading" class="text-center py-5">
      <b-spinner label="Đang tải..."></b-spinner>
    </div>

    <template v-else>
      <div class="stats-grid mb-3">
        <div class="stat-card">
          <div class="stat-label">Tổng giao dịch</div>
          <div class="stat-value">{{ formatNumber(stats.totalOrders) }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Số hóa đơn đã bán</div>
          <div class="stat-value">{{ formatNumber(stats.totalInvoices) }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Doanh thu</div>
          <div class="stat-value">{{ formatCurrency(stats.totalRevenue) }}</div>
        </div>
      </div>

      <b-card class="shadow-sm mb-3">
        <div class="chart-header">
          <h6 class="mb-0 font-weight-bold">Biểu đồ theo tháng</h6>
          <small class="text-muted">Dựa trên giao dịch thanh toán thành công</small>
        </div>
        <div class="bar-chart" :class="{ empty: maxRevenue <= 0 }">
          <div v-for="item in monthlyRows" :key="item.month" class="bar-item">
            <div class="bar-wrap">
              <div class="bar" :style="{ height: barHeight(item.revenue) }"></div>
            </div>
            <div class="bar-value">{{ shortMoney(item.revenue) }}</div>
            <div class="bar-label">{{ formatMonth(item.month) }}</div>
          </div>
        </div>
        <div v-if="monthlyRows.length === 0" class="text-center text-muted py-4">Không có dữ liệu</div>
      </b-card>

      <b-card class="shadow-sm">
        <b-table
          bordered
          hover
          responsive
          small
          show-empty
          :items="pagedMonthlyRows"
          :fields="fields"
          empty-text="Không có dữ liệu"
        >
          <template #cell(month)="{ item }">
            {{ formatMonth(item.month) }}
          </template>
          <template #cell(orderCount)="{ item }">
            {{ formatNumber(item.orderCount) }}
          </template>
          <template #cell(invoiceQuantity)="{ item }">
            {{ formatNumber(item.invoiceQuantity) }}
          </template>
          <template #cell(revenue)="{ item }">
            <span class="font-weight-bold">{{ formatCurrency(item.revenue) }}</span>
          </template>
        </b-table>
        <pagination-bar
          :current.sync="list.current_page"
          :size.sync="list.per_page"
          :total="monthlyRows.length"
          :sizes="pageSizes"
          @page-change="onPageChange"
          @size-change="onPageSizeChange"
        />
      </b-card>
    </template>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'AdministratorsIndex',
  components: { vSelect, PaginationBar },
  data() {
    const now = new Date()
    const year = now.getFullYear()
    return {
      loading: false,
      exporting: false,
      companyOptions: [],
      filters: {
        startMonth: `${year}-01`,
        endMonth: `${year}-12`,
        companyId: null,
        paymentMethod: null,
      },
      paymentMethodOptions: [
        { value: null, text: 'Tất cả' },
        { value: 'MOMO', text: 'MoMo (tất cả)' },
        { value: 'MOMO_WALLET', text: 'MoMo ví điện tử' },
        { value: 'MOMO_ATM', text: 'MoMo ATM nội địa' },
        { value: 'MOMO_CREDIT', text: 'MoMo thẻ quốc tế' },
        { value: 'MOMO_PAY_LATER', text: 'MoMo trả sau' },
        { value: 'VNPAY', text: 'VNPAY' },
        { value: 'ZALOPAY', text: 'ZaloPay' },
      ],
      stats: {
        totalOrders: 0,
        totalInvoices: 0,
        totalRevenue: 0,
        monthly: [],
      },
      list: {
        current_page: 1,
        per_page: 10
      },
      pageSizes: [10, 20, 50, 100],
      fields: [
        { key: 'month', label: 'Tháng' },
        { key: 'orderCount', label: 'Số lượt mua', tdClass: 'text-right' },
        { key: 'invoiceQuantity', label: 'Số hóa đơn', tdClass: 'text-right' },
        { key: 'revenue', label: 'Doanh thu', tdClass: 'text-right' },
      ],
    }
  },
  computed: {
    monthlyRows() {
      return Array.isArray(this.stats.monthly) ? this.stats.monthly : []
    },
    pagedMonthlyRows() {
      const start = (this.list.current_page - 1) * this.list.per_page
      return this.monthlyRows.slice(start, start + this.list.per_page)
    },
    maxRevenue() {
      return this.monthlyRows.reduce((max, item) => Math.max(max, Number(item.revenue || 0)), 0)
    },
  },
  mounted() {
    this.loadCompanies()
    this.loadStatistics()
  },
  methods: {
    async loadStatistics() {
      this.loading = true
      try {
        const { data } = await axios.post('/administrator/invoice-packages/statistics', this.normalizedFilter(), {
          meta: { suppressGlobalErrorToast: true },
        })
        this.stats = data || this.stats
        this.list.current_page = 1
      } catch (err) {
        const message = err?.message === 'Network Error'
          ? 'Không kết nối được backend thống kê'
          : err?.response?.data?.message || 'Không thể tải thống kê mua gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.loading = false
      }
    },
    async loadCompanies() {
      try {
        const { data } = await axios.post('/administrator/company/list', {}, {
          params: { page: 0, size: 5000 },
          meta: { suppressGlobalErrorToast: true },
        })
        this.companyOptions = (data.content || []).map(c => ({
          value: c.id,
          label: c.name || `#${c.id}`,
        }))
      } catch {
        this.companyOptions = []
      }
    },
    async exportExcel() {
      this.exporting = true
      try {
        const res = await axios.post('/administrator/invoice-packages/export', this.normalizedFilter(), {
          responseType: 'blob',
          meta: { suppressGlobalErrorToast: true },
        })
        const blob = new Blob([res.data], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'bao-cao-mua-goi-hoa-don.xlsx'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (err) {
        const message = err?.message === 'Network Error'
          ? 'Không kết nối được backend xuất báo cáo'
          : err?.response?.data?.message || 'Không thể xuất báo cáo Excel'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.exporting = false
      }
    },
    normalizedFilter() {
      return {
        companyId: this.filters.companyId || null,
        paymentMethod: this.filters.paymentMethod || null,
        fromDate: this.monthStart(this.filters.startMonth),
        toDate: this.monthEnd(this.filters.endMonth),
      }
    },
    monthStart(value) {
      return value ? `${value}-01` : null
    },
    monthEnd(value) {
      if (!value) return null
      const [year, month] = value.split('-').map(Number)
      const last = new Date(year, month, 0).getDate()
      return `${value}-${String(last).padStart(2, '0')}`
    },
    barHeight(value) {
      if (this.maxRevenue <= 0) return '4px'
      return `${Math.max(8, (Number(value || 0) / this.maxRevenue) * 150)}px`
    },
    formatMonth(value) {
      if (!value) return '—'
      const parts = String(value).split('-')
      return parts.length === 2 ? `${parts[1]}/${parts[0]}` : value
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    },
    formatCurrency(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
    },
    shortMoney(value) {
      const n = Number(value || 0)
      if (n >= 1000000000) return `${Math.round(n / 100000000) / 10} tỷ`
      if (n >= 1000000) return `${Math.round(n / 100000) / 10} tr`
      if (n >= 1000) return `${Math.round(n / 1000)}k`
      return String(n)
    },
    onPageChange(page) {
      this.list.current_page = Number(page) || 1
    },
    onPageSizeChange(size) {
      this.list.per_page = Number(size) || this.list.per_page
      this.list.current_page = 1
    },
  },
}
</script>

<style scoped>
.administrators-index {
  padding: 20px;
  background: #f5f6fa;
  min-height: calc(100vh - 80px);
}

.filter-card,
.administrators-index .card {
  border-radius: 8px;
}

.filter-label {
  display: block;
  margin-bottom: var(--ui-label-gap);
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  background: #fff;
  padding: 18px;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
}

.stat-label {
  color: #718096;
  font-size: 13px;
}

.stat-value {
  margin-top: 6px;
  font-size: 1.5rem;
  font-weight: 700;
  color: #234e52;
}

.chart-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.bar-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(64px, 1fr));
  gap: 12px;
  align-items: end;
  min-height: 230px;
}

.bar-item {
  text-align: center;
  min-width: 0;
}

.bar-wrap {
  height: 160px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: #f7fafc;
  border-radius: 8px;
  padding: 8px;
}

.bar {
  width: 32px;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #1abc9c 0%, #16a085 100%);
  transition: height 0.2s ease;
}

.bar-value {
  margin-top: 8px;
  font-size: 12px;
  color: #4a5568;
  white-space: nowrap;
}

.bar-label {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #2d3748;
}

.table th {
  background: #f7f9fc;
}

@media (max-width: 768px) {
  .administrators-index {
    padding: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
