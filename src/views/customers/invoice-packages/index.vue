<template>
  <div class="invoice-packages-page">
    <div class="page-head">
      <div>
        <h3>Gói hóa đơn</h3>
        <p>Chọn gói phù hợp để kích hoạt hoặc bổ sung số lượng hóa đơn cho công ty.</p>
      </div>
      <b-button size="sm" variant="outline-primary" @click="reload">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-alert v-if="isCompanyPending" show variant="warning" class="mb-3">
      Tài khoản công ty đang chờ kích hoạt. Sau khi thanh toán thành công, hệ thống sẽ tự tạo hạn mức hóa đơn và kích hoạt tài khoản.
    </b-alert>

    <div v-if="loadingPackages" class="text-center py-5">
      <b-spinner label="Đang tải..."></b-spinner>
    </div>

    <div v-else class="package-grid">
      <div v-for="item in packages" :key="item.id" class="package-card">
        <div class="package-top">
          <div>
            <div class="package-name">{{ item.name }}</div>
            <div class="package-price">{{ formatCurrency(item.totalPrice) }}</div>
          </div>
          <b-badge v-if="item.includeTrial" variant="success">Trải nghiệm</b-badge>
        </div>

        <div class="package-info">
          <div>
            <span>Số Hóa Đơn</span>
            <strong>{{ formatNumber(item.invoiceQuantity) }}</strong>
          </div>
          <div>
            <span>Đơn Giá</span>
            <strong>{{ formatCurrency(item.unitPrice) }}/hđ</strong>
          </div>
          <div>
            <span>Mô tả</span>
            <strong>{{ item.description || '—' }}</strong>
          </div>
        </div>

        <b-button block variant="success" :disabled="purchasing" @click="openPayment(item)">
          Đăng ký ngay
        </b-button>
      </div>

      <b-card v-if="packages.length === 0" class="empty-card text-center">
        Chưa có gói hóa đơn đang kích hoạt.
      </b-card>
    </div>

    <b-card class="mt-4 shadow-sm">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <h5 class="mb-0 font-weight-bold">Lịch sử mua gói</h5>
        <b-button size="sm" variant="outline-secondary" @click="loadPurchases">
          Tải lại
        </b-button>
      </div>

      <b-table
        class="invoice-package-purchases-table"
        bordered
        hover
        responsive
        small
        show-empty
        :busy="loadingPurchases"
        :items="purchases"
        :fields="purchaseFields"
        empty-text="Chưa có giao dịch mua gói"
      >
        <template #cell(index)="{ index }">
          {{ index + 1 + (purchaseList.current_page - 1) * purchaseList.per_page }}
        </template>
        <template #cell(packageName)="{ item }">
          <div class="font-weight-bold text-wrap-anywhere">{{ item.packageName }}</div>
          <small class="text-muted text-wrap-anywhere">{{ item.paymentCode }}</small>
        </template>
        <template #cell(invoiceQuantity)="{ item }">
          {{ formatNumber(item.invoiceQuantity) }}
        </template>
        <template #cell(totalPrice)="{ item }">
          <span class="font-weight-bold">{{ formatCurrency(item.totalPrice) }}</span>
        </template>
        <template #cell(paymentMethod)="{ item }">
          <b-badge variant="info">{{ item.paymentMethod }}</b-badge>
        </template>
        <template #cell(paymentStatus)="{ item }">
          <b-badge :variant="item.paymentStatus === 'SUCCESS' ? 'success' : 'secondary'">
            {{ item.paymentStatus === 'SUCCESS' ? 'Thành công' : item.paymentStatus }}
          </b-badge>
        </template>
        <template #cell(paidAt)="{ item }">
          {{ formatDateTime(item.paidAt || item.createdAt) }}
        </template>
      </b-table>

      <pagination-bar
        :current.sync="purchaseList.current_page"
        :size.sync="purchaseList.per_page"
        :total="purchaseList.total"
        :sizes="pageSizes"
        @page-change="onPurchasePageChange"
        @size-change="onPurchasePageSizeChange"
      />
    </b-card>

    <b-modal ref="paymentModal" title="Đăng ký mua gói hóa đơn" hide-footer>
      <div v-if="selectedPackage">
        <div class="payment-summary">
          <div class="font-weight-bold">{{ selectedPackage.name }}</div>
          <div>{{ formatNumber(selectedPackage.invoiceQuantity) }} hóa đơn</div>
          <div class="payment-amount">{{ formatCurrency(selectedPackage.totalPrice) }}</div>
        </div>

        <b-form-group label="Phương thức thanh toán" label-class="font-weight-bold">
          <b-form-radio-group v-model="paymentMethod" buttons button-variant="outline-primary">
            <b-form-radio value="MOMO">Momo</b-form-radio>
            <b-form-radio value="VNPAY">VNPAY</b-form-radio>
          </b-form-radio-group>
        </b-form-group>

        <b-alert show variant="info">
          Cổng thanh toán thật sẽ tích hợp sau. Hiện tại hệ thống giả lập thanh toán thành công để kiểm thử luồng mua gói.
        </b-alert>

        <div class="text-right">
          <b-button variant="primary" :disabled="purchasing" @click="confirmPayment">
            <b-spinner v-if="purchasing" small class="mr-1"></b-spinner>
            Thanh toán giả lập
          </b-button>
          <b-button variant="secondary" class="ml-2" :disabled="purchasing" @click="$refs.paymentModal.hide()">Hủy</b-button>
        </div>
      </div>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'

export default {
  name: 'CustomerInvoicePackages',
  components: { PaginationBar },
  data() {
    return {
      loadingPackages: false,
      loadingPurchases: false,
      purchasing: false,
      packages: [],
      purchases: [],
      selectedPackage: null,
      paymentMethod: 'MOMO',
      purchaseList: {
        current_page: 1,
        per_page: 10,
        total: 0,
      },
      pageSizes: [10, 20, 50, 100],
      purchaseFields: [
        { key: 'index', label: '#', thStyle: { width: '5%' }, tdClass: 'text-center' },
        { key: 'packageName', label: 'Gói / mã giao dịch', thStyle: { width: '28%' } },
        { key: 'invoiceQuantity', label: 'Số hóa đơn', thStyle: { width: '12%' }, tdClass: 'text-right' },
        { key: 'totalPrice', label: 'Thành tiền', thStyle: { width: '15%' }, tdClass: 'text-right' },
        { key: 'paymentMethod', label: 'Thanh toán', thStyle: { width: '12%' }, tdClass: 'text-center' },
        { key: 'paymentStatus', label: 'Trạng thái', thStyle: { width: '13%' }, tdClass: 'text-center' },
        { key: 'paidAt', label: 'Ngày mua', thStyle: { width: '15%' } },
      ],
    }
  },
  computed: {
    isCompanyPending() {
      const status = this.$app?.info?.company?.status ?? localStorage.getItem('company-status')
      return String(status) === '2'
    },
  },
  mounted() {
    this.reload()
  },
  methods: {
    async reload() {
      await Promise.all([this.loadPackages(), this.loadPurchases()])
    },
    async loadPackages() {
      this.loadingPackages = true
      try {
        const { data } = await axios.get('/invoice-packages', {
          meta: { suppressGlobalErrorToast: true },
        })
        this.packages = Array.isArray(data) ? data : []
      } catch (err) {
        this.packages = []
        const message = err?.message === 'Network Error'
          ? 'Không kết nối được backend gói hóa đơn'
          : err?.response?.data?.message || 'Không thể tải gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.loadingPackages = false
      }
    },
    async loadPurchases() {
      this.loadingPurchases = true
      try {
        const page = this.purchaseList.current_page - 1
        const { data } = await axios.get('/invoice-packages/my-purchases', {
          params: { page, size: this.purchaseList.per_page },
          meta: { suppressGlobalErrorToast: true },
        })
        this.purchases = data.content || []
        this.purchaseList.total = data.totalElements || 0
      } catch {
        this.purchases = []
        this.purchaseList.total = 0
      } finally {
        this.loadingPurchases = false
      }
    },
    onPurchasePageChange(page) {
      this.purchaseList.current_page = Number(page) || 1
      this.loadPurchases()
    },
    onPurchasePageSizeChange(size) {
      this.purchaseList.per_page = Number(size) || this.purchaseList.per_page
      this.purchaseList.current_page = 1
      this.loadPurchases()
    },
    openPayment(item) {
      this.selectedPackage = item
      this.paymentMethod = 'MOMO'
      this.$refs.paymentModal.show()
    },
    async confirmPayment() {
      if (!this.selectedPackage) return
      this.purchasing = true
      try {
        const { data } = await axios.post('/invoice-packages/purchase', {
          packageId: this.selectedPackage.id,
          paymentMethod: this.paymentMethod,
        })
        const purchase = data?.purchase || {}
        if (purchase.companyStatus != null) {
          localStorage.setItem('company-status', String(purchase.companyStatus))
          if (this.$app) {
            this.$app.info.company = {
              ...(this.$app.info.company || {}),
              status: purchase.companyStatus,
            }
          }
        }
        this.$toastr && this.$toastr.success(data?.message || 'Thanh toán thành công')
        this.$refs.paymentModal.hide()
        this.purchaseList.current_page = 1
        await this.loadPurchases()
      } catch (err) {
        const message = err?.response?.data?.message || 'Không thể thanh toán gói hóa đơn'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.purchasing = false
      }
    },
    formatCurrency(value) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
    },
    formatNumber(value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    },
    formatDateTime(value) {
      if (!value) return '—'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
      const dd = String(date.getDate()).padStart(2, '0')
      const mm = String(date.getMonth() + 1).padStart(2, '0')
      const yyyy = date.getFullYear()
      const hh = String(date.getHours()).padStart(2, '0')
      const mi = String(date.getMinutes()).padStart(2, '0')
      return `${dd}/${mm}/${yyyy} ${hh}:${mi}`
    },
  },
}
</script>

<style scoped>
.invoice-packages-page {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-head h3 {
  margin: 0 0 4px;
  font-size: 1.5rem;
  font-weight: 700;
  color: #22313f;
}

.page-head p {
  margin: 0;
  color: #667085;
}

.package-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.package-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.package-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.package-name {
  font-size: 1.2rem;
  font-weight: 700;
  color: #1f2937;
}

.package-price {
  margin-top: 6px;
  font-size: 1.45rem;
  font-weight: 800;
  color: #16a085;
}

.package-info {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.package-info div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf2f7;
}

.package-info span {
  color: #718096;
}

.package-info strong {
  color: #2d3748;
  text-align: right;
}

.empty-card {
  grid-column: 1 / -1;
}

.payment-summary {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 16px;
  background: #f8fafc;
}

.payment-amount {
  margin-top: 8px;
  font-size: 1.35rem;
  font-weight: 800;
  color: #16a085;
}

.table th {
  background: #f7f9fc;
}

.invoice-packages-page::v-deep .table-responsive {
  overflow-x: hidden;
}

.invoice-packages-page::v-deep .invoice-package-purchases-table {
  width: 100%;
  table-layout: fixed;
}

.invoice-packages-page::v-deep .invoice-package-purchases-table th,
.invoice-packages-page::v-deep .invoice-package-purchases-table td {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  vertical-align: middle;
}

.text-wrap-anywhere {
  overflow-wrap: anywhere;
  word-break: break-word;
}

@media (max-width: 768px) {
  .invoice-packages-page {
    padding: 14px;
  }

  .page-head {
    display: block;
  }
}
</style>
