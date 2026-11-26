<template>
  <section class="ai-invoice-assistant">
    <div class="tool-intro">
      <strong>{{ applyLocally ? 'Trợ lý lập phiếu xuất kho' : 'Trợ lý lập hóa đơn' }}</strong>
      <span>Nhập yêu cầu bằng tiếng Việt, kiểm tra đề xuất trước khi áp dụng.</span>
    </div>

    <div class="assistant-workflow">
      <div class="command-row">
        <b-form-textarea
          v-model.trim="command"
          rows="2"
          max-rows="5"
          maxlength="2000"
          placeholder="Ví dụ: Chọn khách hàng A, thêm 5 bàn phím giá 350.000 đồng và thanh toán chuyển khoản"
          :disabled="busy || !!proposalId"
        />
        <b-button size="sm" variant="primary" :disabled="busy || !command || !!proposalId" @click="preview">
          <b-spinner v-if="busy" small class="mr-1" />
          <i v-else class="fas fa-search mr-1"></i>Phân tích
        </b-button>
      </div>

      <b-alert v-if="error" show variant="danger" class="mt-2 mb-0">{{ error }}</b-alert>

      <div v-if="actions.length" class="assistant-section">
        <div class="font-weight-bold mb-1">Thao tác được nhận diện</div>
        <ul class="mb-0 pl-4">
          <li v-for="action in actions" :key="action.id">{{ action.description }}</li>
        </ul>
      </div>

      <div v-if="choices.length" class="choice-box assistant-section">
        <b-alert show :variant="status === 'NEEDS_SELECTION' ? 'warning' : 'info'" class="mb-2">{{ message }}</b-alert>
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

      <div v-if="proposalId && after" class="preview-box assistant-section">
        <b-alert show variant="info" class="mb-2">{{ message }}</b-alert>
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
          class="mt-2 mb-2"
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
        <div class="d-flex justify-content-end mt-2">
          <b-button size="sm" variant="outline-secondary" class="mr-2" :disabled="busy" @click="cancel">Hủy đề xuất</b-button>
          <b-button size="sm" variant="success" :disabled="busy" @click="confirm">
            <b-spinner v-if="busy" small class="mr-1" />{{ applyLocally ? 'Áp dụng vào biểu mẫu' : 'Xác nhận và lưu nháp' }}
          </b-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AiInvoiceAssistant',
  props: {
    invoiceId: { type: Number, default: null },
    formId: { type: Number, default: null },
    category: { type: Number, required: true },
    snapshot: { type: Object, required: true },
    applyLocally: { type: Boolean, default: false }
  },
  data () {
    return {
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
      if (this.applyLocally) {
        const result = this.after
        await this.cancel()
        this.$emit('applied', result)
        this.$toastr?.success('Đã điền đề xuất vào biểu mẫu; phiếu xuất kho chưa được lưu', 'Thành công')
        return
      }
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
.tool-intro { align-items: baseline; display: flex; flex-wrap: wrap; gap: 4px 10px; margin-bottom: 10px; }
.tool-intro strong { color: #334155; font-size: 13px; }
.tool-intro span { color: #718096; font-size: 12px; }
.command-row { align-items: flex-end; display: flex; gap: 8px; }
.command-row textarea { flex: 1; min-height: 62px; resize: vertical; }
.command-row .btn { flex: 0 0 auto; min-width: 105px; }
.assistant-section { margin-top: 12px; }
.choice-box, .preview-box { background: #f8fafc; border: 1px solid #e1e7ef; border-radius: 7px; padding: 12px; }
.preview-box ::v-deep .table { background: #fff; }
@media (max-width: 575px) {
  .command-row { align-items: stretch; flex-direction: column; }
  .command-row .btn { align-self: flex-end; }
}
</style>
