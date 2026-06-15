<template>
  <div v-if="isCompanyPending" class="pending-page">
    <div class="pending-panel">
      <div class="pending-icon">
        <i class="fas fa-hourglass-half"></i>
      </div>
      <div>
        <h2>Tài khoản đang chờ kích hoạt</h2>
        <p>Công ty của bạn đã đăng ký thành công. Vui lòng mua gói hóa đơn để hệ thống tự kích hoạt tài khoản và mở các chức năng sử dụng hóa đơn.</p>
        <b-button variant="success" class="mt-3" to="/invoice-packages">
          <i class="fas fa-box-open mr-1"></i>
          Xem gói hóa đơn
        </b-button>
      </div>
    </div>
  </div>

  <div v-else class="dashboard-page">
    <div class="dashboard-head">
      <div>
        <h2>Thống kê hóa đơn</h2>
      </div>
      <div class="head-actions">
        <b-button size="sm" variant="outline-primary" :disabled="loading" @click="loadStats">
          <i class="fas fa-sync-alt mr-1"></i>
          Làm mới
        </b-button>
        <b-button size="sm" variant="success" to="/invoice-packages">
          <i class="fas fa-plus-circle mr-1"></i>
          Mua thêm hóa đơn
        </b-button>
      </div>
    </div>

    <div v-if="loading" class="loading-box">
      <b-spinner label="Đang tải..."></b-spinner>
      <div class="text-muted mt-2">Đang tải dữ liệu thống kê</div>
    </div>

    <template v-else>
      <b-alert v-if="limitAlert" show :variant="limitAlert.variant" class="mb-3">
        <div class="d-flex align-items-center justify-content-between flex-wrap">
          <span>
            <i :class="limitAlert.icon" class="mr-1"></i>
            {{ limitAlert.text }}
          </span>
          <b-button v-if="limitAlert.action" size="sm" variant="success" to="/invoice-packages" class="mt-2 mt-md-0">
            Mua gói ngay
          </b-button>
        </div>
      </b-alert>

      <div class="stats-grid">
        <div v-for="card in kpiCards" :key="card.key" class="stat-card" :class="card.tone">
          <div class="stat-icon">
            <i :class="card.icon"></i>
          </div>
          <div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-note">{{ card.note }}</div>
          </div>
        </div>
      </div>

      <div class="dashboard-grid">
        <section class="dashboard-card usage-card">
          <div class="section-head">
            <div>
              <h5>Hạn mức hóa đơn</h5>
              <span>{{ usageSummary }}</span>
            </div>
            <b-badge :variant="limitStatus.variant">{{ limitStatus.text }}</b-badge>
          </div>

          <div class="usage-number">
            <strong>{{ usagePercentage }}%</strong>
            <span>đã sử dụng</span>
          </div>

          <b-progress height="12px" :value="usagePercentage" :variant="progressVariant" class="mb-3"></b-progress>

          <div class="quota-grid">
            <div>
              <span>Đã dùng</span>
              <strong>{{ formatNumber(invoiceStats.usedInvoices) }}</strong>
            </div>
            <div>
              <span>Còn lại</span>
              <strong>{{ formatNumber(invoiceStats.remainingInvoices) }}</strong>
            </div>
            <div>
              <span>Tổng hạn mức</span>
              <strong>{{ formatNumber(invoiceStats.totalInvoices) }}</strong>
            </div>
          </div>
        </section>

        <section class="dashboard-card insight-card">
          <div class="section-head">
            <div>
              <h5>Tổng quan năm nay</h5>
              <span>Các chỉ số phát hành hóa đơn trong năm hiện tại</span>
            </div>
          </div>

          <div class="insight-list">
            <div class="insight-row">
              <div class="insight-icon success">
                <i class="fas fa-file-signature"></i>
              </div>
              <div>
                <span>Hóa đơn đã phát hành</span>
                <strong>{{ formatNumber(invoiceStats.issuedThisYear) }}</strong>
              </div>
            </div>
            <div class="insight-row">
              <div class="insight-icon primary">
                <i class="fas fa-coins"></i>
              </div>
              <div>
                <span>Giá trị phát hành</span>
                <strong>{{ formatCurrency(invoiceStats.valueThisYear) }}</strong>
              </div>
            </div>
            <div class="insight-row">
              <div class="insight-icon neutral">
                <i class="fas fa-chart-line"></i>
              </div>
              <div>
                <span>Giá trị trung bình/hóa đơn</span>
                <strong>{{ formatCurrency(averageInvoiceValue) }}</strong>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="quick-actions">
        <router-link class="quick-action" to="/invoice/create">
          <i class="fas fa-file-invoice"></i>
          <div>
            <strong>Lập hóa đơn</strong>
            <span>Tạo nhanh hóa đơn GTGT mới</span>
          </div>
        </router-link>
        <router-link class="quick-action" to="/invoice/vat-invoice/list">
          <i class="fas fa-list"></i>
          <div>
            <strong>Danh sách hóa đơn</strong>
            <span>Kiểm tra hóa đơn đã tạo</span>
          </div>
        </router-link>
        <router-link class="quick-action" to="/reports/invoice/list">
          <i class="fas fa-chart-bar"></i>
          <div>
            <strong>Báo cáo hóa đơn</strong>
            <span>Xem báo cáo và đối soát</span>
          </div>
        </router-link>
      </div>
    </template>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerIndex',
  data() {
    return {
      loading: true,
      invoiceStats: {
        totalInvoices: 0,
        usedInvoices: 0,
        remainingInvoices: 0,
        issuedThisYear: 0,
        valueThisYear: 0,
      },
    }
  },
  computed: {
    isCompanyPending() {
      const status = this.$app?.info?.company?.status ?? localStorage.getItem('company-status')
      return String(status) === '2'
    },
    totalInvoices() {
      return Number(this.invoiceStats.totalInvoices || 0)
    },
    usedInvoices() {
      return Number(this.invoiceStats.usedInvoices || 0)
    },
    remainingInvoices() {
      return Number(this.invoiceStats.remainingInvoices || 0)
    },
    usagePercentage() {
      if (this.totalInvoices <= 0) return 0
      return Math.min(100, Math.round((this.usedInvoices / this.totalInvoices) * 100))
    },
    remainingPercentage() {
      if (this.totalInvoices <= 0) return 0
      return Math.max(0, 100 - this.usagePercentage)
    },
    averageInvoiceValue() {
      const issued = Number(this.invoiceStats.issuedThisYear || 0)
      if (issued <= 0) return 0
      return Number(this.invoiceStats.valueThisYear || 0) / issued
    },
    progressVariant() {
      if (this.totalInvoices <= 0 || this.remainingPercentage <= 10) return 'danger'
      if (this.remainingPercentage <= 25) return 'warning'
      return 'success'
    },
    limitStatus() {
      if (this.totalInvoices <= 0) {
        return { text: 'Chưa có hạn mức', variant: 'secondary' }
      }
      if (this.remainingPercentage <= 10) {
        return { text: 'Sắp hết', variant: 'danger' }
      }
      if (this.remainingPercentage <= 25) {
        return { text: 'Cần theo dõi', variant: 'warning' }
      }
      return { text: 'Ổn định', variant: 'success' }
    },
    limitAlert() {
      if (this.totalInvoices <= 0) {
        return {
          variant: 'warning',
          icon: 'fas fa-info-circle',
          text: 'Công ty chưa có hạn mức hóa đơn. Vui lòng mua gói hóa đơn để bắt đầu sử dụng.',
          action: true,
        }
      }
      if (this.remainingPercentage <= 10) {
        return {
          variant: 'danger',
          icon: 'fas fa-exclamation-triangle',
          text: `Hạn mức chỉ còn ${this.formatNumber(this.remainingInvoices)} hóa đơn. Bạn nên mua thêm để không gián đoạn phát hành.`,
          action: true,
        }
      }
      if (this.remainingPercentage <= 25) {
        return {
          variant: 'warning',
          icon: 'fas fa-exclamation-circle',
          text: `Hạn mức còn ${this.remainingPercentage}%. Hãy theo dõi để chủ động bổ sung khi cần.`,
          action: false,
        }
      }
      return null
    },
    usageSummary() {
      if (this.totalInvoices <= 0) return 'Chưa phát sinh hạn mức hóa đơn'
      return `${this.formatNumber(this.usedInvoices)} / ${this.formatNumber(this.totalInvoices)} hóa đơn`
    },
    kpiCards() {
      return [
        {
          key: 'total',
          label: 'Tổng hóa đơn đã mua',
          value: this.formatNumber(this.invoiceStats.totalInvoices),
          note: 'Hạn mức đang có',
          icon: 'fas fa-layer-group',
          tone: 'tone-primary',
        },
        {
          key: 'used',
          label: 'Đã sử dụng',
          value: this.formatNumber(this.invoiceStats.usedInvoices),
          note: `${this.usagePercentage}% tổng hạn mức`,
          icon: 'fas fa-check-circle',
          tone: 'tone-success',
        },
        {
          key: 'remaining',
          label: 'Còn lại',
          value: this.formatNumber(this.invoiceStats.remainingInvoices),
          note: `${this.remainingPercentage}% chưa dùng`,
          icon: 'fas fa-wallet',
          tone: 'tone-warning',
        },
        {
          key: 'issued',
          label: 'Phát hành năm nay',
          value: this.formatNumber(this.invoiceStats.issuedThisYear),
          note: 'Hóa đơn thành công',
          icon: 'fas fa-file-alt',
          tone: 'tone-info',
        },
        {
          key: 'value',
          label: 'Giá trị năm nay',
          value: this.formatCurrency(this.invoiceStats.valueThisYear),
          note: 'Tổng giá trị hóa đơn',
          icon: 'fas fa-coins',
          tone: 'tone-dark',
        },
      ]
    },
  },
  methods: {
    async loadStats() {
      if (this.isCompanyPending) {
        this.loading = false
        return
      }
      this.loading = true
      try {
        const response = await axios.get('/dashboard/stats', {
          meta: { suppressGlobalErrorToast: true },
        })
        this.invoiceStats = response.data || this.invoiceStats
        try {
          window.dispatchEvent(new CustomEvent('invoice-limit-check'))
        } catch {}
      } catch (error) {
        const message = error?.message === 'Network Error'
          ? 'Không kết nối được backend thống kê'
          : error?.response?.data?.message || 'Không thể tải dữ liệu thống kê'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.loading = false
      }
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    },
    formatCurrency(value) {
      return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
      }).format(Number(value || 0))
    },
  },
  mounted() {
    this.loadStats()
  },
}
</script>

<style scoped>
.dashboard-page,
.pending-page {
  min-height: calc(100vh - 140px);
}

.dashboard-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dashboard-head h2 {
  margin: 0 0 6px;
  font-size: 1.55rem;
  font-weight: 800;
  color: #1f2937;
}

.dashboard-head p {
  margin: 0;
  color: #667085;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.loading-box {
  padding: 52px 16px;
  text-align: center;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.pending-panel {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding: 28px 32px;
  border-radius: 8px;
  background: #fff8e1;
  border: 1px solid #ffe08a;
  color: #6b5200;
}

.pending-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  border-radius: 8px;
  background: #fff3bf;
  color: #b7791f;
  font-size: 1.25rem;
}

.pending-panel h2 {
  margin: 0 0 10px;
  font-size: 1.45rem;
  font-weight: 800;
}

.pending-panel p {
  margin: 0;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.04);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
}

.stat-label {
  color: #667085;
  font-size: 13px;
  font-weight: 600;
}

.stat-value {
  margin-top: 4px;
  color: #1f2937;
  font-size: 1.35rem;
  font-weight: 800;
  line-height: 1.2;
}

.stat-note {
  margin-top: 4px;
  color: #8a94a6;
  font-size: 12px;
}

.tone-success .stat-icon {
  background: #e8fff7;
  color: #16a085;
}

.tone-warning .stat-icon {
  background: #fff7e6;
  color: #f59e0b;
}

.tone-info .stat-icon {
  background: #e8f7ff;
  color: #0984e3;
}

.tone-dark .stat-icon {
  background: #f1f5f9;
  color: #334155;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.04);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-head h5 {
  margin: 0 0 4px;
  color: #1f2937;
  font-size: 1.05rem;
  font-weight: 800;
}

.section-head span {
  color: #667085;
  font-size: 13px;
}

.usage-number {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.usage-number strong {
  color: #16a085;
  font-size: 2.2rem;
  line-height: 1;
}

.usage-number span {
  color: #667085;
  font-weight: 600;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quota-grid div {
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
}

.quota-grid span,
.insight-row span {
  display: block;
  color: #667085;
  font-size: 13px;
}

.quota-grid strong,
.insight-row strong {
  display: block;
  margin-top: 4px;
  color: #1f2937;
  font-size: 1.05rem;
}

.insight-list {
  display: grid;
  gap: 12px;
}

.insight-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.insight-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 8px;
  background: #eef2f7;
  color: #334155;
}

.insight-icon.success {
  background: #e8fff7;
  color: #16a085;
}

.insight-icon.primary {
  background: #eef6ff;
  color: #2563eb;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #1f2937;
  text-decoration: none;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.quick-action:hover {
  border-color: #16a085;
  color: #1f2937;
  text-decoration: none;
  transform: translateY(-2px);
}

.quick-action i {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 8px;
  background: #e8fff7;
  color: #16a085;
}

.quick-action strong {
  display: block;
  font-weight: 800;
}

.quick-action span {
  display: block;
  margin-top: 2px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 992px) {
  .dashboard-grid,
  .quick-actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-head,
  .pending-panel {
    display: block;
  }

  .head-actions {
    justify-content: flex-start;
    margin-top: 12px;
  }

  .pending-icon {
    margin-bottom: 14px;
  }

  .quota-grid {
    grid-template-columns: 1fr;
  }
}
</style>
