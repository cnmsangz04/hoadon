<template>
  <b-card class="ai-invoice-assistant mb-3" border-variant="primary">
    <div class="d-flex align-items-center justify-content-between flex-wrap">
      <div>
        <h6 class="mb-1 font-weight-bold"><i class="fas fa-wand-magic-sparkles mr-2"></i>Trợ lý lập hóa đơn</h6>
        <div class="small text-muted">Nhập câu lệnh tiếng Việt. Mọi thay đổi chỉ được lưu sau khi bạn xác nhận.</div>
      </div>
      <b-button size="sm" variant="outline-primary" @click="expanded = !expanded">
        {{ expanded ? 'Thu gọn' : 'Mở trợ lý' }}
      </b-button>
    </div>

    <div v-if="expanded" class="mt-3">
      <b-input-group>
        <b-form-textarea
          v-model.trim="command"
          rows="2"
          max-rows="4"
          maxlength="2000"
          placeholder="Ví dụ: Chọn khách hàng A, thêm 5 bàn phím giá 350.000 đồng và thanh toán chuyển khoản"
          :disabled="busy || !!proposalId"
        />
        <b-input-group-append>
          <b-button variant="primary" :disabled="busy || !command || !!proposalId" @click="preview">
            <b-spinner v-if="busy" small />
            <span v-else>Phân tích</span>
          </b-button>
        </b-input-group-append>
      </b-input-group>

      <b-alert v-if="error" show variant="danger" class="mt-3 mb-0">{{ error }}</b-alert>

      <div v-if="actions.length" class="mt-3">
        <div class="font-weight-bold mb-2">Thao tác được nhận diện</div>
        <ul class="mb-2 pl-4">
          <li v-for="action in actions" :key="action.id">{{ action.description }}</li>
        </ul>
      </div>

      <div v-if="choices.length" class="choice-box mt-3">
        <b-alert show :variant="status === 'NEEDS_SELECTION' ? 'warning' : 'info'">{{ message }}</b-alert>
        <b-form-group v-for="choice in choices" :key="choice.actionId" :label="choiceLabel(choice)">
          <b-form-select
            v-if="choice.candidates && choice.candidates.length"
            v-model="selections[choice.actionId]"
            :options="candidateOptions(choice)"
          />
          <div v-else class="text-danger small">Không tìm thấy dữ liệu phù hợp với “{{ choice.query }}”.</div>
        </b-form-group>
        <b-button
          v-if="canResolve"
          size="sm"
          variant="primary"
          :disabled="busy"
          @click="preview"
        >Tiếp tục với lựa chọn</b-button>
      </div>

      <div v-if="proposalId && after" class="preview-box mt-3">
        <b-alert show variant="info">{{ message }}</b-alert>
        <b-row>
          <b-col md="6">
            <div class="small text-muted">Khách hàng</div>
            <div class="font-weight-bold">{{ customerLabel(after.customer) }}</div>
          </b-col>
          <b-col md="3">
            <div class="small text-muted">Thanh toán</div>
            <div>{{ paymentLabel(after.paymentType) }}</div>
          </b-col>
          <b-col md="3">
            <div class="small text-muted">Ngày lập</div>
            <div>{{ after.dateExport || '—' }}</div>
          </b-col>
        </b-row>
        <b-table
          class="mt-3 mb-2"
          small bordered responsive
          :items="after.detail || []"
          :fields="previewFields"
          show-empty
          empty-text="Chưa có hàng hóa"
        >
          <template #cell(price)="data">{{ formatMoney(data.item.price) }}</template>
          <template #cell(total)="data">{{ formatMoney(data.item.total) }}</template>
          <template #cell(amount)="data">{{ formatMoney(data.item.amount) }}</template>
        </b-table>
        <div class="text-right font-weight-bold">Tổng thanh toán: {{ formatMoney(after.amount) }} {{ after.currency || 'VND' }}</div>
        <div class="d-flex justify-content-end mt-3">
          <b-button variant="outline-secondary" class="mr-2" :disabled="busy" @click="cancel">Hủy đề xuất</b-button>
          <b-button variant="success" :disabled="busy" @click="confirm">
            <b-spinner v-if="busy" small class="mr-1" />Xác nhận và lưu nháp
          </b-button>
        </div>
      </div>
    </div>
  </b-card>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AiInvoiceAssistant',
  props: {
    invoiceId: { type: Number, default: null },
    formId: { type: Number, default: null },
    category: { type: Number, required: true },
    snapshot: { type: Object, required: true }
  },
  data () {
    return {
      expanded: false,
      busy: false,
      command: '',
      error: '',
      status: '',
      message: '',
      actions: [],
      choices: [],
      selections: {},
      proposalId: null,
      after: null,
      previewFields: [
        { key: 'num', label: '#' },
        { key: 'code', label: 'Mã' },
        { key: 'name', label: 'Hàng hóa/Dịch vụ' },
        { key: 'unit', label: 'ĐVT' },
        { key: 'quantity', label: 'Số lượng' },
        { key: 'price', label: 'Đơn giá' },
        { key: 'total', label: 'Tiền hàng' },
        { key: 'amount', label: 'Thanh toán' }
      ]
    }
  },
  computed: {
    canResolve () {
      return this.choices.length > 0 && this.choices.every(choice =>
        !choice.candidates?.length || this.selections[choice.actionId]
      ) && this.choices.every(choice => choice.candidates?.length)
    }
  },
  methods: {
    async preview () {
      if (!this.command || this.busy) return
      this.busy = true
      this.error = ''
      try {
        const { data } = await axios.post('/ai/invoice-assistant/preview', {
          command: this.command,
          invoiceId: this.invoiceId,
          formId: this.formId,
          category: this.category,
          current: this.snapshot,
          selections: this.selections
        }, { meta: { suppressGlobalErrorToast: true } })
        this.status = data.status
        this.message = data.message
        this.actions = data.actions || []
        this.choices = data.choices || []
        this.proposalId = data.proposalId || null
        this.after = data.after || null
        const nextSelections = { ...this.selections }
        this.choices.forEach(choice => {
          if (choice.candidates?.length === 1) nextSelections[choice.actionId] = choice.candidates[0].id
        })
        this.selections = nextSelections
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không thể phân tích câu lệnh lúc này'
      } finally {
        this.busy = false
      }
    },
    async confirm () {
      if (!this.proposalId || this.busy) return
      this.busy = true
      this.error = ''
      try {
        const { data } = await axios.post(`/ai/invoice-assistant/proposals/${this.proposalId}/confirm`, {}, {
          meta: { suppressGlobalErrorToast: true }
        })
        this.$emit('applied', data)
        this.$toastr?.success('Đã áp dụng thay đổi và lưu hóa đơn nháp', 'Thành công')
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không thể áp dụng đề xuất'
      } finally {
        this.busy = false
      }
    },
    async cancel () {
      if (this.proposalId) {
        try {
          await axios.post(`/ai/invoice-assistant/proposals/${this.proposalId}/cancel`, {}, {
            meta: { suppressGlobalErrorToast: true }
          })
        } catch (_) {}
      }
      this.resetPreview()
    },
    resetPreview () {
      this.status = ''
      this.message = ''
      this.actions = []
      this.choices = []
      this.selections = {}
      this.proposalId = null
      this.after = null
      this.error = ''
    },
    choiceLabel (choice) {
      const type = choice.entityType === 'CUSTOMER' ? 'khách hàng' : (choice.entityType === 'PRODUCT' ? 'hàng hóa' : 'dòng hàng hóa')
      return `Chọn ${type} cho “${choice.query}”`
    },
    candidateOptions (choice) {
      return [{ value: null, text: '-- Vui lòng chọn --' }].concat((choice.candidates || []).map(item => ({
        value: item.id,
        text: [item.code, item.name, item.detail].filter(Boolean).join(' · ')
      })))
    },
    customerLabel (customer) {
      if (!customer) return 'Chưa chọn'
      return customer.name || customer.buyer || customer.code || 'Chưa chọn'
    },
    paymentLabel (value) {
      return Number(value) === 1 ? 'Tiền mặt' : (Number(value) === 2 ? 'Chuyển khoản' : 'Tiền mặt/Chuyển khoản')
    },
    formatMoney (value) {
      return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
    }
  }
}
</script>

<style scoped>
.ai-invoice-assistant { background: linear-gradient(135deg, #f7fbff 0%, #ffffff 70%); }
.choice-box, .preview-box { border: 1px solid #dce6f2; border-radius: 8px; padding: 14px; background: #fff; }
</style>
