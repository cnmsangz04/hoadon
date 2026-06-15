<template>
  <div class="container-fluid py-3 invoice-process">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">{{ pageTitle }}</h4>
      <b-button size="sm" variant="outline-secondary" @click="$router.push({ name: 'CustomerVatInvoiceList' })">
        <i class="fas fa-list mr-1"></i> Danh sách hóa đơn
      </b-button>
    </div>

    <b-card class="shadow-sm">
      <b-form @submit.prevent="confirmInvoice">
        <b-row>
          <b-col lg="4" md="6">
            <b-form-group label="Số hóa đơn cần xử lý" label-class="form-label">
              <b-form-input
                v-model.trim="invoiceNo"
                type="number"
                min="1"
                placeholder="Nhập số hóa đơn"
                :disabled="isBusy"
              />
            </b-form-group>
          </b-col>

          <b-col v-if="isAdjust" lg="4" md="6">
            <b-form-group label="Loại điều chỉnh" label-class="form-label">
              <b-form-select
                v-model="invoiceTypeAdjust"
                :options="adjustTypeOptions"
                :disabled="isBusy"
              />
            </b-form-group>
          </b-col>

          <b-col lg="4" md="12" class="d-flex align-items-end mb-3">
            <b-button type="submit" variant="primary" :disabled="isBusy">
              <b-spinner v-if="isBusy" small class="mr-2" />
              <i v-else class="fas fa-check mr-1"></i>
              Xác nhận
            </b-button>
          </b-col>
        </b-row>
      </b-form>

      <b-alert show :variant="isAdjust ? 'info' : 'warning'" class="mb-0">
        {{ ruleText }}
      </b-alert>
    </b-card>

    <b-card v-if="foundInvoice" class="shadow-sm mt-3">
      <h6 class="font-weight-bold mb-3">Hóa đơn đã chọn</h6>
      <b-row>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Số hóa đơn</div>
          <div class="font-weight-bold">{{ foundInvoice.no || '—' }}</div>
        </b-col>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Ký hiệu</div>
          <div>{{ (foundInvoice.formCode || '') + (foundInvoice.serial || '') || '—' }}</div>
        </b-col>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Ngày lập</div>
          <div>{{ foundInvoice.dateExport || '—' }}</div>
        </b-col>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Trạng thái</div>
          <b-badge variant="success">{{ foundInvoice.statusText || '—' }}</b-badge>
        </b-col>
        <b-col md="6" class="mb-2">
          <div class="text-muted small">Khách hàng</div>
          <div>{{ foundInvoice.customerName || '—' }}</div>
        </b-col>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Mã tra cứu</div>
          <code>{{ foundInvoice.lookupCode || '—' }}</code>
        </b-col>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">Tổng tiền</div>
          <div>{{ formatCurrency(foundInvoice.amount) }}</div>
        </b-col>
      </b-row>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'VatInvoiceProcess',
  data () {
    return {
      invoiceNo: '',
      invoiceTypeAdjust: 1,
      foundInvoice: null,
      isBusy: false,
      adjustTypeOptions: [
        { value: 1, text: 'Điều chỉnh tăng' },
        { value: 2, text: 'Điều chỉnh giảm' },
        { value: 3, text: 'Điều chỉnh thông tin' }
      ]
    }
  },
  computed: {
    isAdjust () {
      return this.mode === 'adjust'
    },
    mode () {
      return this.$route?.meta?.processingMode === 'adjust' ? 'adjust' : 'replace'
    },
    invoiceType () {
      return this.isAdjust ? 2 : 1
    },
    pageTitle () {
      return this.isAdjust ? 'Hóa đơn điều chỉnh' : 'Hóa đơn thay thế'
    },
    ruleText () {
      if (this.isAdjust) {
        return 'Chỉ điều chỉnh hóa đơn gốc đã phát hành hoặc đã bị điều chỉnh.'
      }
      return 'Chỉ thay thế hóa đơn đã phát hành và là hóa đơn gốc hoặc hóa đơn thay thế.'
    }
  },
  methods: {
    async confirmInvoice () {
      const no = Number(this.invoiceNo)
      if (!Number.isFinite(no) || no <= 0) {
        this.notifyError('Vui lòng nhập số hóa đơn hợp lệ')
        return
      }
      this.isBusy = true
      try {
        const { data } = await axios.get('/invoices/processing/lookup', {
          params: {
            no,
            invoiceType: this.invoiceType
          },
          meta: { suppressGlobalErrorToast: true }
        })
        this.foundInvoice = data
        this.$router.push({
          name: 'CustomerVatInvoiceCreate',
          query: {
            referenceId: data.id,
            invoiceType: this.invoiceType,
            invoiceTypeAdjust: this.isAdjust ? this.invoiceTypeAdjust : 0
          }
        })
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không tìm thấy hóa đơn đủ điều kiện xử lý'
        this.notifyError(msg)
      } finally {
        this.isBusy = false
      }
    },
    notifyError (message) {
      if (this.$toastr && typeof this.$toastr.error === 'function') {
        this.$toastr.error(message, 'Lỗi')
      } else if (this.$bvToast) {
        this.$bvToast.toast(message, { title: 'Lỗi', variant: 'danger', solid: true, autoHideDelay: 4000 })
      }
    },
    formatCurrency (v) {
      try {
        return Number(v || 0).toLocaleString('vi-VN')
      } catch {
        return '0'
      }
    }
  }
}
</script>

<style scoped>
.invoice-process {
  background: #f3f6fa;
  min-height: 100vh;
}

.form-label {
  font-weight: 600;
  color: #334155;
}
</style>
