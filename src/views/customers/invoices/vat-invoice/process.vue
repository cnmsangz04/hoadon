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

          <b-col lg="8" md="12" v-if="isProcessing">
            <b-form-group label="Lý do điều chỉnh/thay thế" label-class="form-label">
              <b-form-select v-model="relatedReason" :options="relatedReasonOptions" :disabled="isBusy" />
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
      relatedReason: null,
      foundInvoice: null,
      isBusy: false,
      relatedReasonOptions: [
        { value: null, text: '-- Chọn lý do điều chỉnh/thay thế --' },
        { value: '1', text: 'Điều chỉnh giá trị quyết toán dự án đầu tư hoặc điều chỉnh giá bán theo quy định chuyên ngành' },
        { value: '2', text: 'Điều chỉnh giá trị, khối lượng theo kết luận của cơ quan nhà nước có thẩm quyền' },
        { value: '3', text: 'Điều chỉnh giá bán buôn điện' },
        { value: '4', text: 'Điều chỉnh do chiết khấu thương mại' },
        { value: '5', text: 'Điều chỉnh do trả lại hàng hóa không phải tài sản phải đăng ký hoặc tài sản đã đăng ký tên người mua' },
        { value: '6', text: 'Thay thế do trả lại hàng hóa là tài sản phải đăng ký và đã đăng ký tên người mua' },
        { value: '7', text: 'Điều chỉnh do hoàn phí, giảm phí, giảm hoa hồng môi giới bảo hiểm hoặc khoản chi để giảm thu' },
        { value: '8', text: 'Điều chỉnh do hủy/chấm dứt giao dịch hoặc hủy một phần dịch vụ thu tiền trước nhiều kỳ' },
        { value: '9', text: 'Điều chỉnh do hoàn phí của tổ chức tín dụng hoặc tổ chức cung ứng dịch vụ thanh toán' },
        { value: '10', text: 'Điều chỉnh đối với dịch vụ viễn thông sử dụng thẻ trả trước' },
        { value: '11', text: 'Điều chỉnh do thay đổi giá trị bán khí thiên nhiên khi quy đổi sang Việt Nam đồng' },
        { value: '12', text: 'Điều chỉnh đối với hóa đơn đổi, hoàn chứng từ vận chuyển hàng không' },
        { value: '13', text: 'Điều chỉnh/thay thế hóa đơn đã lập sai' },
        { value: '14', text: 'Thay thế hóa đơn điện tử đã lập sai là hóa đơn điện tử từ máy tính tiền' }
      ],
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
    isProcessing () {
      return this.invoiceType === 1 || this.invoiceType === 2
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
      if (!this.relatedReason) {
        this.notifyError('Vui lòng chọn lý do điều chỉnh/thay thế')
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
            invoiceTypeAdjust: this.isAdjust ? this.invoiceTypeAdjust : 0,
            relatedReason: this.relatedReason
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
  margin-bottom: var(--ui-label-gap);
}
</style>
