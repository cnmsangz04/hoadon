<template>
  <div class="container-fluid py-3 invoice-process">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">{{ pageTitle }}</h4>
      <b-button size="sm" variant="outline-secondary" @click="$router.push({ name: listRouteName })">
        <i class="fas fa-list mr-1"></i> Danh sách {{ invoiceCategory === 6 ? 'phiếu' : 'hóa đơn' }}
      </b-button>
    </div>

    <b-card class="shadow-sm">
      <b-form @submit.prevent="confirmInvoice">
        <b-row>
          <b-col lg="4" md="6">
            <b-form-group label="Loại chứng từ" label-class="form-label">
              <b-form-select
                v-model="processingKind"
                :options="processingKindOptions"
                :disabled="isBusy"
              />
            </b-form-group>
          </b-col>

          <b-col lg="4" md="6">
              <b-form-group :label="`Số ${invoiceCategory === 6 ? 'phiếu' : 'hóa đơn'} cần xử lý`" label-class="form-label">
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
      <h6 class="font-weight-bold mb-3">{{ invoiceCategory === 6 ? 'Phiếu xuất kho đã chọn' : 'Hóa đơn đã chọn' }}</h6>
      <b-row>
        <b-col md="3" class="mb-2">
          <div class="text-muted small">{{ invoiceCategory === 6 ? 'Số phiếu' : 'Số hóa đơn' }}</div>
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
      selectedCategory: null,
      selectedStockIssueType: null,
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
  mounted () {
    this.initializeProcessingKind()
  },
  computed: {
    isAdjust () {
      return this.mode === 'adjust'
    },
    mode () {
      return this.$route?.meta?.processingMode === 'adjust' ? 'adjust' : 'replace'
    },
    processingScope () {
      if (this.$route?.meta?.processingScope) return this.$route.meta.processingScope
      return Number(this.$route?.meta?.invoiceCategory || 1) === 6 ? 'stock' : 'invoice'
    },
    processingKind: {
      get () {
        return this.processingScope === 'stock' ? `6:${this.stockIssueType}` : String(this.invoiceCategory)
      },
      set (value) {
        this.setProcessingKind(value)
      }
    },
    processingKindOptions () {
      if (this.processingScope === 'stock') {
        return [
          { value: '6:1', text: 'Phiếu xuất kho kiêm vận chuyển nội bộ' },
          { value: '6:2', text: 'Phiếu xuất kho hàng gửi bán đại lý' }
        ]
      }
      return [
        { value: '1', text: 'Hóa đơn GTGT' },
        { value: '2', text: 'Hóa đơn bán hàng' }
      ]
    },
    invoiceType () {
      return this.isAdjust ? 2 : 1
    },
    invoiceCategory () {
      if (this.processingScope === 'stock') return 6
      const queryCategory = Number(this.$route?.query?.category)
      const category = this.selectedCategory || queryCategory || Number(this.$route?.meta?.invoiceCategory || 1)
      return [1, 2, 6].includes(category) ? category : 1
    },
    stockIssueType () {
      const queryType = Number(this.$route?.query?.stockIssueType)
      const routeType = this.selectedStockIssueType || queryType || Number(this.$route?.meta?.stockIssueType || 1)
      return routeType === 2 ? 2 : 1
    },
    listRouteName () {
      if (this.invoiceCategory === 6) return this.stockIssueType === 2 ? 'CustomerStockIssueConsignmentList' : 'CustomerStockIssueInternalTransportList'
      return this.invoiceCategory === 2 ? 'CustomerSalesInvoiceList' : 'CustomerVatInvoiceList'
    },
    createRouteName () {
      if (this.invoiceCategory === 6) return this.stockIssueType === 2 ? 'CustomerStockIssueConsignmentCreate' : 'CustomerStockIssueInternalTransportCreate'
      return this.invoiceCategory === 2 ? 'CustomerSalesInvoiceCreate' : 'CustomerVatInvoiceCreate'
    },
    isProcessing () {
      return this.invoiceType === 1 || this.invoiceType === 2
    },
    pageTitle () {
      if (this.invoiceCategory === 6) return 'Phiếu xuất kho ' + (this.isAdjust ? 'điều chỉnh' : 'thay thế')
      const prefix = 'Hóa đơn '
      return prefix + (this.isAdjust ? 'điều chỉnh' : 'thay thế')
    },
    ruleText () {
      const subject = this.invoiceCategory === 6 ? 'phiếu xuất kho' : 'hóa đơn'
      if (this.isAdjust) {
        return `Chỉ điều chỉnh ${subject} gốc đã phát hành hoặc đã bị điều chỉnh.`
      }
      return `Chỉ thay thế ${subject} đã phát hành và là bản gốc hoặc bản thay thế.`
    }
  },
  watch: {
    '$route.query': {
      deep: true,
      handler () {
        this.initializeProcessingKind()
        this.invoiceNo = ''
        this.relatedReason = null
        this.foundInvoice = null
      }
    }
  },
  methods: {
    initializeProcessingKind () {
      if (this.processingScope === 'stock') {
        this.selectedCategory = 6
        const value = Number(this.$route?.query?.stockIssueType || this.$route?.meta?.stockIssueType || 1)
        this.selectedStockIssueType = value === 2 ? 2 : 1
        return
      }
      const value = Number(this.$route?.query?.category || this.$route?.meta?.invoiceCategory || 1)
      this.selectedCategory = value === 2 ? 2 : 1
      this.selectedStockIssueType = null
    },
    setProcessingKind (value) {
      const [category, stockIssueType] = String(value).split(':')
      this.selectedCategory = Number(category) === 6 ? 6 : Number(category) === 2 ? 2 : 1
      this.selectedStockIssueType = Number(category) === 6 && Number(stockIssueType) === 2 ? 2 : 1
      this.invoiceNo = ''
      this.relatedReason = null
      this.foundInvoice = null
    },
    async confirmInvoice () {
      const no = Number(this.invoiceNo)
      if (!Number.isFinite(no) || no <= 0) {
        this.notifyError(`Vui lòng nhập số ${this.invoiceCategory === 6 ? 'phiếu' : 'hóa đơn'} hợp lệ`)
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
            invoiceType: this.invoiceType,
            category: this.invoiceCategory,
            stockIssueType: this.invoiceCategory === 6 ? this.stockIssueType : undefined
          },
          meta: { suppressGlobalErrorToast: true }
        })
        this.foundInvoice = data
        this.$router.push({
          name: this.createRouteName,
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
