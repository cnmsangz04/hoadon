<template>
  <section class="ai-invoice-assistant" :class="{ 'is-embedded': embedded }">
    <header v-if="!embedded" class="assistant-heading">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m12 3 2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4L12 3Z" />
        <path d="m20 2 .7 2.3L23 5l-2.3.7L20 8l-.7-2.3L17 5l2.3-.7L20 2Z" />
      </svg>
      <h3>Trợ lý AI</h3>
    </header>
    <div class="assistant-workflow">
      <template v-if="!embedded">
      <div class="command-composer" :class="{ 'is-disabled': busy || !!proposalId }">
        <textarea
          :id="commandInputId"
          ref="commandInput"
          v-model.trim="command"
          class="command-input"
          rows="2"
          maxlength="2000"
          placeholder="Ví dụ: Chọn khách hàng ABC, thêm 5 bàn phím giá 350.000đ và thanh toán chuyển khoản."
          :disabled="busy || !!proposalId"
          aria-label="Yêu cầu cho trợ lý AI"
          @keydown.ctrl.enter.prevent="preview"
          @keydown.meta.enter.prevent="preview"
        ></textarea>
      </div>
      <div class="command-footer">
        <span class="command-hint"><i class="far fa-check-circle" aria-hidden="true"></i>Xem trước khi áp dụng</span>
        <button type="button" class="analyze-button" title="Phân tích yêu cầu (Ctrl + Enter)"
                :disabled="busy || !command || !!proposalId" @click="preview">
          <b-spinner v-if="busy" small />
          <span>{{ busy ? 'Đang phân tích' : 'Phân tích' }}</span>
          <i v-if="!busy" class="fas fa-arrow-right" aria-hidden="true"></i>
        </button>
      </div>
      </template>

      <b-alert v-if="error" show variant="danger" class="assistant-error mt-2 mb-0">{{ error }}</b-alert>

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
        <b-row class="preview-customer">
          <b-col cols="12" class="mb-2">
            <div class="small text-muted">Khách hàng</div>
            <div class="font-weight-bold">{{ customerLabel(after.customer) }}</div>
          </b-col>
          <b-col cols="6">
            <div class="small text-muted">Thanh toán</div>
            <div>{{ paymentLabel(after.paymentType) }}</div>
          </b-col>
          <b-col cols="6">
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
        <div class="preview-actions">
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
    applyLocally: { type: Boolean, default: false },
    embedded: { type: Boolean, default: false }
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
    controlState () { return { busy: this.busy, locked: !!this.proposalId } },
    commandInputId () { return `ai-invoice-command-${this._uid}` },
    canResolve () {
      return this.choices.length > 0 && this.choices.every(choice =>
        !choice.candidates?.length || this.selections[choice.actionId]
      ) && this.choices.every(choice => choice.candidates?.length)
    }
  },
  watch: {
    controlState: {
      immediate: true,
      handler (state) { this.$emit('control-state', state) }
    },
    command () {
      this.$nextTick(this.resizeCommandInput)
    }
  },
  mounted () {
    this.resizeCommandInput()
    if (typeof ResizeObserver !== 'undefined') {
      this._commandResizeObserver = new ResizeObserver(entries => {
        const width = entries[0].contentRect.width
        if (width !== this._commandInputWidth) {
          this._commandInputWidth = width
          this.resizeCommandInput()
        }
      })
      if (this.$refs.commandInput) this._commandResizeObserver.observe(this.$refs.commandInput)
    } else {
      window.addEventListener('resize', this.resizeCommandInput)
    }
  },
  beforeDestroy () {
    this._commandResizeObserver?.disconnect()
    window.removeEventListener('resize', this.resizeCommandInput)
  },
  methods: {
    resizeCommandInput () {
      const input = this.$refs.commandInput
      if (!input || !input.getClientRects().length) return
      input.style.height = 'auto'
      input.style.height = `${Math.min(input.scrollHeight, 200)}px`
    },
    async preview () {
      if (!this.command || this.busy || this.proposalId) return
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
        this.busy = true
        try {
          const result = this.after
          await this.cancelProposal()
          this.$emit('applied', result)
          this.$toastr?.success('Đã điền đề xuất vào biểu mẫu; phiếu xuất kho chưa được lưu', 'Thành công')
        } finally {
          this.busy = false
        }
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
      if (this.busy) return
      this.busy = true
      try {
        await this.cancelProposal()
      } finally {
        this.busy = false
      }
    },
    async cancelProposal () {
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
.ai-invoice-assistant { min-width: 0; min-height: 154px; padding: 12px; border: 1px solid #e6ebf2; border-radius: 8px; background: #fff; box-shadow: 0 1px 4px rgba(17, 24, 39, .04); }
.ai-invoice-assistant.is-embedded { min-height: 0; padding: 0; border: 0; background: transparent; box-shadow: none; }
.assistant-heading { display: flex; align-items: center; gap: 7px; min-height: 20px; margin-bottom: 10px; }
.assistant-heading svg { flex: 0 0 17px; width: 17px; height: 17px; color: #3b73df; }
.assistant-heading h3 { margin: 0; color: #334155; font-size: 13px; font-weight: 600; line-height: 1.4; }
.command-composer { min-width: 0; border: 1px solid #d8dee8; border-radius: 6px; background: #fbfcfe; transition: border-color .15s ease, box-shadow .15s ease; }
.command-composer:focus-within { border-color: #78a5ed; box-shadow: 0 0 0 3px rgba(67, 120, 218, .1); }
.command-composer.is-disabled { background: #f8fafc; }
.command-input { display: block; width: 100%; min-width: 0; height: 52px; min-height: 52px; max-height: 200px; margin: 0; padding: 8px 10px; border: 0; border-radius: 6px; outline: none; background: transparent; color: #26364e; font: inherit; font-size: 13px; line-height: 1.4; resize: none; overflow-y: auto; }
.command-input::placeholder { color: #8995a6; }
.analyze-button { display: inline-flex; flex: 0 0 auto; align-items: center; justify-content: center; gap: 7px; min-width: 102px; min-height: 32px; padding: 6px 10px; border: 1px solid #007bff; border-radius: 6px; background: #007bff; color: #fff; font: inherit; font-size: 13px; font-weight: 600; line-height: 1.25; cursor: pointer; transition: background .15s ease; }
.analyze-button:hover:not(:disabled) { border-color: #0062cc; background: #0069d9; }
.analyze-button:disabled { opacity: .65; cursor: default; }
.analyze-button:focus-visible { outline: 2px solid #7aa7f7; outline-offset: 2px; }
.analyze-button i { font-size: 11px; }
.command-footer { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.command-hint { display: inline-flex; align-items: center; gap: 5px; color: #7a889b; font-size: 12px; }
.command-hint i { color: #6b9687; }
.assistant-error, .assistant-section { min-width: 0; }
.assistant-section { margin-top: 20px; }
.choice-box, .preview-box { background: #fff; border: 1px solid #e1e7ef; border-radius: 8px; padding: 14px; }
.preview-box ::v-deep .table { background: #fff; }
.preview-box ::v-deep .table-responsive { max-width: 100%; }
.preview-box ::v-deep table { min-width: 600px; }
.preview-actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
@media (max-width: 575px) {
  .command-hint { font-size: 11px; }
  .preview-actions .btn { flex: 1; margin-right: 0 !important; }
}
</style>
