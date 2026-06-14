<template>
  <div v-if="isCompanyPending" class="pending-page">
    <div class="pending-card">
      <h2>Tài khoản đang chờ kích hoạt</h2>
      <p>Công ty của bạn đã đăng ký thành công và đang chờ quản trị viên kích hoạt. Các chức năng sẽ được mở sau khi công ty được kích hoạt.</p>
    </div>
  </div>

  <div v-else class="dashboard">
    <h2>Thống kê hóa đơn</h2>

    <div v-if="loading" class="text-center py-4">
      <b-spinner label="Đang tải..."></b-spinner>
    </div>

    <div v-else class="stats-container">
      <div class="stat-card">
        <h3>{{ invoiceStats.totalInvoices || 0 }}</h3>
        <p>Tổng số hóa đơn đã mua</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.usedInvoices || 0 }}</h3>
        <p>Số hóa đơn đã sử dụng</p>
      </div>
      <div class="stat-card">
        <h3>{{ invoiceStats.remainingInvoices || 0 }}</h3>
        <p>Số hóa đơn còn lại</p>
      </div>
      <div class="stat-card warning">
        <h3>{{ invoiceStats.issuedThisYear || 0 }}</h3>
        <p>Số hóa đơn phát hành trong năm</p>
      </div>
      <div class="stat-card info">
        <h3>{{ formatCurrency(invoiceStats.valueThisYear) }}</h3>
        <p>Giá trị hóa đơn phát hành trong năm</p>
      </div>
    </div>
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
  },
  methods: {
    async loadStats() {
      if (this.isCompanyPending) {
        this.loading = false
        return
      }
      this.loading = true
      try {
        const response = await axios.get('/dashboard/stats')
        this.invoiceStats = response.data
      } catch (error) {
        console.error('Error loading dashboard stats:', error)
        this.$toastr && this.$toastr.error('Không thể tải dữ liệu thống kê')
      } finally {
        this.loading = false
      }
    },
    formatCurrency(value) {
      if (!value) return '0 ₫'
      return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
      }).format(value)
    },
  },
  mounted() {
    this.loadStats()
  },
}
</script>

<style scoped>
.dashboard,
.pending-page {
  background: #fff;
  padding: 20px;
  border-radius: 6px;
}

.pending-card {
  padding: 28px 32px;
  border-radius: 8px;
  background: #fff8e1;
  border: 1px solid #ffe08a;
  color: #6b5200;
}

.pending-card h2 {
  margin: 0 0 12px;
  font-size: 1.6rem;
  font-weight: 700;
}

.pending-card p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
}

.stats-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 20px;
}

.stat-card {
  flex: 1 1 200px;
  background: #00b894;
  color: #fff;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.stat-card h3 {
  font-size: 2rem;
  margin-bottom: 8px;
  font-weight: bold;
}

.stat-card p {
  margin: 0;
  font-size: 0.9rem;
  opacity: 0.9;
}

.stat-card.warning {
  background: #fdcb6e;
}

.stat-card.info {
  background: #0984e3;
}
</style>
