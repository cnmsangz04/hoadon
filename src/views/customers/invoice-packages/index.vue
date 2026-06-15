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
          <b-badge :variant="paymentStatusVariant(item.paymentStatus)">
            {{ paymentStatusText(item.paymentStatus) }}
          </b-badge>
        </template>
        <template #cell(paidAt)="{ item }">
          {{ formatDateTime(item.paidAt || item.createdAt) }}
        </template>
        <template #cell(actions)="{ item }">
          <b-button
            v-if="canRetryPayment(item)"
            size="sm"
            variant="outline-primary"
            :disabled="retryingPurchaseId === item.id || purchasing"
            @click="retryPayment(item)"
          >
            <b-spinner v-if="retryingPurchaseId === item.id" small class="mr-1"></b-spinner>
            Thanh toán lại
          </b-button>
          <span v-else class="text-muted">—</span>
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

    <b-modal
      ref="paymentModal"
      title="Đăng ký mua gói hóa đơn"
      hide-footer
      centered
      size="lg"
      modal-class="payment-modal"
      @hidden="onPaymentModalHidden"
    >
      <div v-if="selectedPackage" class="payment-modal-body">
        <div class="payment-summary">
          <div>
            <div class="payment-summary-label">Gói đã chọn</div>
            <div class="payment-summary-title">{{ selectedPackage.name }}</div>
            <div class="payment-summary-subtitle">{{ formatNumber(selectedPackage.invoiceQuantity) }} hóa đơn</div>
          </div>
          <div class="payment-summary-price">
            <span>Thanh toán</span>
            <strong>{{ formatCurrency(selectedPackage.totalPrice) }}</strong>
          </div>
        </div>

        <div class="payment-section">
          <div class="payment-section-title">Phương thức thanh toán</div>
          <div class="payment-method-grid">
            <button
              v-for="method in paymentMethods"
              :key="method.value"
              type="button"
              class="payment-option"
              :class="{ active: paymentMethod === method.value }"
              :disabled="purchasing || paymentWaiting"
              @click="paymentMethod = method.value"
            >
              <span class="payment-option-icon">
                <i :class="method.icon"></i>
              </span>
              <span class="payment-option-copy">
                <strong>{{ method.label }}</strong>
                <small>{{ method.description }}</small>
              </span>
              <span class="payment-option-check">
                <i class="fas fa-check"></i>
              </span>
            </button>
          </div>
        </div>

        <div class="payment-note">
          <i class="fas fa-info-circle"></i>
          <span>{{ paymentHelpText }}</span>
        </div>

        <div v-if="paymentWaiting" class="payment-waiting">
          <div class="payment-waiting-spinner">
            <b-spinner small label="Đang chờ thanh toán"></b-spinner>
          </div>
          <div>
            <strong>Đang chờ xác nhận thanh toán</strong>
            <p>
              Bạn có thể quét mã hoặc hoàn tất thanh toán trên tab vừa mở. Trang này sẽ tự cập nhật khi cổng thanh toán xác nhận thành công.
            </p>
            <button
              v-if="paymentCheckoutUrl"
              type="button"
              class="payment-link-button"
              @click="openCheckoutUrl"
            >
              Mở lại cổng thanh toán
            </button>
          </div>
        </div>

        <div class="payment-actions">
          <b-button variant="outline-secondary" :disabled="purchasing" @click="$refs.paymentModal.hide()">
            {{ paymentWaiting ? 'Ẩn' : 'Hủy' }}
          </b-button>
          <b-button variant="primary" class="payment-submit" :disabled="purchasing || paymentWaiting" @click="confirmPayment">
            <b-spinner v-if="purchasing" small class="mr-1"></b-spinner>
            {{ paymentWaiting ? 'Đang chờ thanh toán...' : paymentButtonText }}
          </b-button>
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
      retryingPurchaseId: null,
      paymentWaiting: false,
      paymentCheckoutUrl: null,
      waitingPurchaseId: null,
      paymentPollTimer: null,
      paymentPollAttempts: 0,
      packages: [],
      purchases: [],
      selectedPackage: null,
      paymentMethod: 'MOMO',
      paymentMethods: [
        {
          value: 'MOMO',
          label: 'MoMo',
          description: 'Ví điện tử sandbox',
          icon: 'fas fa-wallet',
        },
        {
          value: 'VNPAY',
          label: 'VNPAY',
          description: 'ATM, QR hoặc thẻ test',
          icon: 'fas fa-credit-card',
        },
      ],
      purchaseList: {
        current_page: 1,
        per_page: 10,
        total: 0,
      },
      pageSizes: [10, 20, 50, 100],
      purchaseFields: [
        { key: 'index', label: '#', thStyle: { width: '5%' }, tdClass: 'text-center' },
        { key: 'packageName', label: 'Gói / mã giao dịch', thStyle: { width: '24%' } },
        { key: 'invoiceQuantity', label: 'Số hóa đơn', thStyle: { width: '10%' }, tdClass: 'text-right' },
        { key: 'totalPrice', label: 'Thành tiền', thStyle: { width: '13%' }, tdClass: 'text-right' },
        { key: 'paymentMethod', label: 'Thanh toán', thStyle: { width: '10%' }, tdClass: 'text-center' },
        { key: 'paymentStatus', label: 'Trạng thái', thStyle: { width: '12%' }, tdClass: 'text-center' },
        { key: 'paidAt', label: 'Ngày mua', thStyle: { width: '14%' } },
        { key: 'actions', label: 'Thao tác', thStyle: { width: '12%' }, tdClass: 'text-center' },
      ],
    }
  },
  computed: {
    isCompanyPending() {
      const status = this.$app?.info?.company?.status ?? localStorage.getItem('company-status')
      return String(status) === '2'
    },
    paymentButtonText() {
      if (this.paymentMethod === 'MOMO') return 'Thanh toán MoMo'
      if (this.paymentMethod === 'VNPAY') return 'Thanh toán VNPAY'
      return 'Thanh toán giả lập'
    },
    paymentHelpText() {
      const gateway = this.paymentMethod === 'VNPAY' ? 'VNPAY' : 'MoMo'
      return `Cổng ${gateway}.`
    },
  },
  mounted() {
    this.handlePaymentReturnMessage()
    this.reload()
  },
  beforeDestroy() {
    this.clearPaymentPollTimer()
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
      this.resetPaymentWaiting()
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
        if (await this.handlePaymentCheckout(data, this.paymentMethod)) {
          return
        }
        const purchase = data?.purchase || {}
        this.applyCompanyStatus(purchase)
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
    async retryPayment(item) {
      if (!item || !item.id) return
      this.resetPaymentWaiting()
      this.selectedPackage = null
      this.paymentMethod = item.paymentMethod || 'MOMO'
      this.retryingPurchaseId = item.id
      try {
        const { data } = await axios.post(`/invoice-packages/purchases/${item.id}/retry-payment`)
        if (await this.handleRetryCheckout(data, item.paymentMethod)) {
          return
        }
        const purchase = data?.purchase || {}
        this.applyCompanyStatus(purchase)
        this.$toastr && this.$toastr.success(data?.message || 'Thanh toán thành công')
        this.purchaseList.current_page = 1
        await this.loadPurchases()
      } catch (err) {
        const message = err?.response?.data?.message || 'Không thể thanh toán lại giao dịch'
        this.$toastr && this.$toastr.error(message)
      } finally {
        this.retryingPurchaseId = null
      }
    },
    async handlePaymentCheckout(data, fallbackMethod) {
      const purchase = data?.purchase || {}
      if (!purchase.payUrl) return false

      this.paymentWaiting = true
      this.waitingPurchaseId = purchase.id
      this.paymentCheckoutUrl = purchase.payUrl
      this.paymentMethod = purchase.paymentMethod || fallbackMethod || this.paymentMethod
      if (!this.selectedPackage) {
        this.selectedPackage = {
          id: purchase.packageId,
          name: purchase.packageName,
          invoiceQuantity: purchase.invoiceQuantity,
          totalPrice: purchase.totalPrice,
        }
      }

      const checkoutWindow = window.open(purchase.payUrl, '_blank', 'noopener')
      const gateway = purchase.paymentMethod || fallbackMethod || 'thanh toán'
      if (checkoutWindow) {
        this.$toastr && this.$toastr.info(data?.message || `Đã mở cổng thanh toán ${gateway}`)
      } else {
        this.$toastr && this.$toastr.warning('Trình duyệt đã chặn tab thanh toán. Bấm "Mở lại cổng thanh toán" để tiếp tục.')
      }
      this.upsertPurchaseRow(purchase)
      this.purchaseList.current_page = 1
      await this.loadPurchases()
      this.startPaymentPolling(purchase.id)
      return true
    },
    async handleRetryCheckout(data, fallbackMethod) {
      const purchase = data?.purchase || {}
      if (!purchase.payUrl) return false

      const checkoutWindow = window.open(purchase.payUrl, '_blank', 'noopener')
      const gateway = purchase.paymentMethod || fallbackMethod || 'thanh toán'
      if (checkoutWindow) {
        this.$toastr && this.$toastr.info(data?.message || `Đã mở cổng thanh toán ${gateway}`)
      } else {
        this.$toastr && this.$toastr.warning('Trình duyệt đã chặn tab thanh toán. Bấm "Thanh toán lại" lần nữa để mở cổng thanh toán mới.')
      }
      this.upsertPurchaseRow(purchase)
      this.purchaseList.current_page = 1
      await this.loadPurchases()
      this.startPaymentPolling(purchase.id)
      return true
    },
    startPaymentPolling(purchaseId) {
      if (!purchaseId) return
      this.clearPaymentPollTimer()
      this.waitingPurchaseId = purchaseId
      this.paymentPollAttempts = 0
      this.pollPaymentStatus()
      this.paymentPollTimer = window.setInterval(this.pollPaymentStatus, 3000)
    },
    clearPaymentPollTimer() {
      if (this.paymentPollTimer) {
        window.clearInterval(this.paymentPollTimer)
        this.paymentPollTimer = null
      }
    },
    resetPaymentWaiting() {
      this.clearPaymentPollTimer()
      this.paymentWaiting = false
      this.paymentCheckoutUrl = null
      this.waitingPurchaseId = null
      this.paymentPollAttempts = 0
    },
    onPaymentModalHidden() {
      this.resetPaymentWaiting()
      this.selectedPackage = null
    },
    async pollPaymentStatus() {
      if (!this.waitingPurchaseId) return
      this.paymentPollAttempts += 1
      try {
        const { data } = await axios.get(`/invoice-packages/purchases/${this.waitingPurchaseId}`, {
          meta: { suppressGlobalErrorToast: true },
        })
        this.upsertPurchaseRow(data)

        if (data.paymentStatus === 'SUCCESS') {
          this.applyCompanyStatus(data)
          this.resetPaymentWaiting()
          this.$refs.paymentModal && this.$refs.paymentModal.hide()
          this.$toastr && this.$toastr.success('Thanh toán thành công. Hạn mức hóa đơn đã được cập nhật.')
          this.purchaseList.current_page = 1
          await this.loadPurchases()
          return
        }

        if (data.paymentStatus === 'FAILED') {
          this.resetPaymentWaiting()
          this.$toastr && this.$toastr.error(data.paymentMessage || 'Thanh toán chưa thành công')
          await this.loadPurchases()
          return
        }

        if (this.paymentPollAttempts >= 120) {
          this.clearPaymentPollTimer()
          this.$toastr && this.$toastr.warning('Chưa nhận được xác nhận thanh toán. Hệ thống vẫn sẽ cập nhật khi cổng thanh toán gửi kết quả.')
        }
      } catch {
        if (this.paymentPollAttempts >= 120) {
          this.clearPaymentPollTimer()
        }
      }
    },
    upsertPurchaseRow(purchase) {
      if (!purchase || !purchase.id) return
      const index = this.purchases.findIndex(item => item.id === purchase.id)
      if (index >= 0) {
        this.$set(this.purchases, index, { ...this.purchases[index], ...purchase })
      } else {
        this.purchases.unshift(purchase)
      }
    },
    applyCompanyStatus(purchase) {
      if (!purchase || purchase.companyStatus == null) return
      localStorage.setItem('company-status', String(purchase.companyStatus))
      if (this.$app) {
        this.$app.info.company = {
          ...(this.$app.info.company || {}),
          status: purchase.companyStatus,
        }
      }
    },
    openCheckoutUrl() {
      if (!this.paymentCheckoutUrl) return
      window.open(this.paymentCheckoutUrl, '_blank', 'noopener')
    },
    canRetryPayment(item) {
      const status = item?.paymentStatus ? String(item.paymentStatus).toUpperCase() : ''
      return ['PENDING', 'FAILED'].includes(status)
    },
    handlePaymentReturnMessage() {
      const query = this.$route?.query || {}
      const status = query.momoStatus || query.vnpayStatus
      if (!status) return

      const gateway = query.vnpayStatus ? 'VNPAY' : 'MoMo'
      const message = query.message || (status === 'success'
        ? `Thanh toán ${gateway} thành công`
        : `Thanh toán ${gateway} chưa thành công`)

      if (status === 'success') {
        localStorage.setItem('company-status', '1')
        if (this.$app) {
          this.$app.info.company = {
            ...(this.$app.info.company || {}),
            status: 1,
          }
        }
        this.$toastr && this.$toastr.success(message)
      } else {
        this.$toastr && this.$toastr.error(message)
      }

      const cleaned = { ...query }
      delete cleaned.momoStatus
      delete cleaned.vnpayStatus
      delete cleaned.orderId
      delete cleaned.message
      if (this.$router) {
        this.$router.replace({ query: cleaned }).catch(() => {})
      }
    },
    paymentStatusVariant(status) {
      if (status === 'SUCCESS') return 'success'
      if (status === 'PENDING') return 'warning'
      if (status === 'FAILED') return 'danger'
      return 'secondary'
    },
    paymentStatusText(status) {
      if (status === 'SUCCESS') return 'Thành công'
      if (status === 'PENDING') return 'Chờ thanh toán'
      if (status === 'FAILED') return 'Thất bại'
      return status || '—'
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
  padding: 16px;
  margin-bottom: 18px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.payment-summary-label,
.payment-summary-price span {
  color: #667085;
  font-size: 0.82rem;
  text-transform: uppercase;
  font-weight: 700;
}

.payment-summary-title {
  margin-top: 4px;
  font-size: 1.18rem;
  font-weight: 800;
  color: #1f2937;
}

.payment-summary-subtitle {
  margin-top: 2px;
  color: #667085;
}

.payment-summary-price {
  text-align: right;
  min-width: 150px;
}

.payment-summary-price strong {
  display: block;
  margin-top: 4px;
  font-size: 1.5rem;
  font-weight: 800;
  color: #16a085;
}

.payment-section {
  margin-bottom: 14px;
}

.payment-section-title {
  margin-bottom: 10px;
  color: #344054;
  font-weight: 800;
}

.payment-method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.payment-option {
  width: 100%;
  border: 1px solid #d6dee8;
  border-radius: 8px;
  background: #fff;
  color: #263445;
  padding: 14px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.payment-option:hover:not(:disabled) {
  border-color: #16a085;
  box-shadow: 0 8px 18px rgba(22, 160, 133, 0.08);
}

.payment-option.active {
  border-color: #16a085;
  background: #f0fdfa;
  box-shadow: 0 0 0 3px rgba(22, 160, 133, 0.12);
}

.payment-option:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.payment-option-icon,
.payment-option-check {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.payment-option-icon {
  background: #eef6ff;
  color: #2563eb;
}

.payment-option-copy {
  display: grid;
  gap: 2px;
}

.payment-option-copy strong {
  font-size: 1rem;
}

.payment-option-copy small {
  color: #667085;
}

.payment-option-check {
  background: #16a085;
  color: #fff;
  opacity: 0;
  transform: scale(0.9);
}

.payment-option.active .payment-option-check {
  opacity: 1;
  transform: scale(1);
}

.payment-note {
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e3a8a;
  padding: 12px 14px;
  margin-top: 14px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  line-height: 1.45;
}

.payment-note i {
  margin-top: 3px;
}

.payment-waiting {
  border: 1px solid #d6dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  margin-top: 14px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.payment-waiting-spinner {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #f0fdfa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #16a085;
  flex: 0 0 auto;
}

.payment-waiting strong {
  color: #1f2937;
}

.payment-waiting p {
  margin: 4px 0 0;
  color: #667085;
  line-height: 1.45;
}

.payment-link-button {
  border: 0;
  background: transparent;
  color: #2563eb;
  padding: 8px 0 0;
  font-weight: 700;
}

.payment-link-button:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.payment-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.payment-submit {
  min-width: 170px;
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

  .payment-summary,
  .payment-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .payment-summary-price {
    text-align: left;
  }

  .payment-method-grid {
    grid-template-columns: 1fr;
  }

  .payment-submit {
    width: 100%;
  }
}
</style>
