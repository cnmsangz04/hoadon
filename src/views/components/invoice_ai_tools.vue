<template>
  <section class="invoice-ai-tools">
    <div ref="entry" class="ai-entry" :class="{ 'is-disabled': busy || locked }"
         @dragover.prevent @drop.prevent="fileDropped">
      <svg class="entry-sparkle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m12 3 2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4L12 3Z" />
        <path d="m20 2 .7 2.3L23 5l-2.3.7L20 8l-.7-2.3L17 5l2.3-.7L20 2Z" />
      </svg>
      <input ref="uploadInput" class="sr-only" type="file" tabindex="-1"
             accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
             :disabled="busy || locked" aria-label="Chọn ảnh hoặc PDF chứng từ" @change="fileSelected" />
      <textarea v-if="!file" ref="commandInput" :value="command" rows="1" maxlength="2000"
                class="entry-input" :disabled="busy || locked" :readonly="!showAssistant"
                :style="{ paddingRight: actionsWidth + 'px' }"
                :placeholder="promptPlaceholder"
                aria-label="Yêu cầu hỗ trợ nhập liệu"
                @input="updateCommand" @compositionstart="$event.target.composing = true"
                @compositionend="finishComposition" @keydown="entryKeydown"></textarea>
      <div v-else class="attached-file" :style="{ paddingRight: actionsWidth + 'px' }">
        <i class="far fa-file-alt" aria-hidden="true"></i>
        <div class="attached-file-text">
          <strong :title="file.name">{{ file.name }}</strong>
          <span>Nhận diện nội bộ</span>
        </div>
        <button type="button" class="remove-file" :disabled="busy" aria-label="Bỏ chứng từ" @click="clearFile">
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
      <div ref="actions" class="entry-actions">
        <button v-if="command && !file" type="button" class="clear-command" :disabled="busy || locked"
                aria-label="Xóa yêu cầu" @click="clearCommand"><i class="fas fa-times" aria-hidden="true"></i></button>
        <button type="button" class="attach-button" :disabled="busy || locked"
                title="Nhận diện chứng từ · JPG, PNG, PDF · Tối đa 10 MB" aria-label="Tải chứng từ để nhận diện nội bộ"
                @click="$refs.uploadInput.click()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m8 12 6.5-6.5a3 3 0 0 1 4.2 4.2l-8.5 8.5a5 5 0 0 1-7.1-7.1l9.2-9.2" />
            <path d="m8 12 6.5-6.5" transform="translate(1 4)" />
          </svg>
          <span>Chứng từ</span>
        </button>
        <button type="button" class="process-button" :disabled="!canProcess"
                :aria-label="file || !showAssistant ? 'Nhận diện chứng từ' : 'Tạo đề xuất bằng AI'" @click="process">
          <b-spinner v-if="busy" small />
          <span>{{ busy ? 'Đang xử lý' : file || !showAssistant ? 'Nhận diện' : 'Tạo bằng AI' }}</span>
          <kbd v-if="!busy" aria-hidden="true">↵</kbd>
        </button>
      </div>
    </div>
    <p v-if="error" class="entry-error" role="alert">{{ error }}</p>
    <DocumentRecognition ref="document" :category="category" embedded
                         @control-state="documentState = $event" @applied="applied('document-applied', $event)" />
    <AiInvoiceAssistant v-if="showAssistant" ref="assistant" :invoice-id="invoiceId" :form-id="formId"
                        :category="category" :snapshot="snapshot" :apply-locally="category === 6" embedded
                        @control-state="assistantState = $event" @applied="applied('assistant-applied', $event)" />
  </section>
</template>

<script>
import AiInvoiceAssistant from '@/views/components/ai_invoice_assistant.vue'
import DocumentRecognition from '@/views/components/document_recognition.vue'

export default {
  name: 'InvoiceAiTools',
  components: { AiInvoiceAssistant, DocumentRecognition },
  props: {
    category: { type: Number, required: true },
    invoiceId: { type: Number, default: null },
    formId: { type: Number, default: null },
    snapshot: { type: Object, required: true },
    showAssistant: { type: Boolean, default: false }
  },
  data () {
    return {
      command: '',
      file: null,
      error: '',
      actionsWidth: 220,
      submitting: false,
      documentState: { busy: false },
      assistantState: { busy: false, locked: false }
    }
  },
  computed: {
    busy () { return this.submitting || this.documentState.busy || this.assistantState.busy },
    locked () { return this.assistantState.locked },
    canProcess () { return !this.busy && !this.locked && (!!this.file || this.showAssistant && !!this.command.trim()) },
    promptPlaceholder () {
      if (!this.showAssistant) return 'Chọn ảnh hoặc PDF để nhận diện chứng từ…'
      return this.category === 6 ? 'Mô tả phiếu xuất kho cần lập…' : 'Mô tả hóa đơn cần lập…'
    }
  },
  watch: {
    command (value) {
      const assistant = this.$refs.assistant
      if (assistant && value.trim() !== assistant.command && !this.busy && !this.locked) assistant.resetPreview()
      this.$nextTick(this.resizeInput)
    },
    file () { this.$nextTick(this.resizeInput) },
    showAssistant (visible) {
      if (!visible) this.assistantState = { busy: false, locked: false }
    }
  },
  mounted () {
    this.resizeInput()
    if (typeof ResizeObserver !== 'undefined') {
      this._entryObserver = new ResizeObserver(entries => {
        const width = entries[0].contentRect.width
        if (width !== this._entryWidth) {
          this._entryWidth = width
          this.resizeInput()
        }
      })
      this._entryObserver.observe(this.$refs.entry)
      this._entryObserver.observe(this.$refs.actions)
    } else {
      window.addEventListener('resize', this.resizeInput)
    }
  },
  beforeDestroy () {
    this._entryObserver?.disconnect()
    window.removeEventListener('resize', this.resizeInput)
  },
  methods: {
    updateCommand (event) {
      this.command = event.target.value
    },
    finishComposition (event) {
      event.target.composing = false
      this.updateCommand(event)
    },
    resizeInput () {
      const width = this.$refs.actions?.getBoundingClientRect().width
      if (width && this.actionsWidth !== Math.ceil(width) + 12) {
        this.actionsWidth = Math.ceil(width) + 12
        this.$nextTick(this.resizeInput)
      }
      const input = this.$refs.commandInput
      if (!input || !input.getClientRects().length) return
      input.style.height = 'auto'
      input.style.height = `${Math.min(input.scrollHeight, 96)}px`
    },
    entryKeydown (event) {
      if (event.target.composing || event.isComposing || event.keyCode === 229) return
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.process()
      } else if (event.key === 'Escape' && !this.busy) {
        if (this.locked) this.$refs.assistant.cancel()
        else this.clearCommand()
      }
    },
    clearCommand () {
      this.command = ''
      this.error = ''
      this.$refs.assistant?.resetPreview()
      this.$nextTick(() => this.$refs.commandInput?.focus())
    },
    fileSelected (event) {
      this.selectFile(event.target.files?.[0])
      event.target.value = ''
    },
    fileDropped (event) {
      if (this.busy || this.locked) return
      const files = event.dataTransfer?.files
      if (files?.length > 1) {
        this.error = 'Mỗi lần chỉ chọn một chứng từ.'
        return
      }
      this.selectFile(files?.[0])
    },
    selectFile (file) {
      if (!file || this.busy || this.locked) return
      this.clearFile()
      if (!/\.(jpe?g|png|pdf)$/i.test(file.name)) {
        this.error = 'Vui lòng chọn ảnh JPG, PNG hoặc tài liệu PDF.'
        return
      }
      if (file.size > 10 * 1024 * 1024) {
        this.error = 'Chứng từ vượt quá giới hạn 10 MB.'
        return
      }
      this.$refs.assistant?.resetPreview()
      this.file = file
      this.$refs.document.file = file
      this.$refs.document.fileChanged()
    },
    clearFile () {
      this.file = null
      this.error = ''
      this.$refs.document?.reset()
    },
    async process () {
      if (!this.canProcess) return
      this.submitting = true
      this.error = ''
      try {
        if (this.file) {
          await this.$refs.document.recognize()
        } else {
          const command = this.command.trim()
          if (this.$refs.assistant.command !== command) this.$refs.assistant.resetPreview()
          this.$refs.assistant.command = command
          await this.$refs.assistant.preview()
        }
      } finally {
        this.submitting = false
      }
    },
    applied (event, result) {
      this.$emit(event, result)
      if (event === 'document-applied') this.clearFile()
      else {
        this.command = ''
        this.$refs.assistant.resetPreview()
      }
    }
  }
}
</script>

<style scoped>
.invoice-ai-tools { min-width: 0; margin-bottom: 14px; }
.ai-entry { position: relative; min-width: 0; min-height: 44px; border: 1px solid var(--ui-border, #d8dee8); border-radius: var(--ui-radius, 6px); background: #fff; transition: border-color .15s ease, box-shadow .15s ease; }
.ai-entry:focus-within { border-color: #7aa7f7; box-shadow: var(--ui-focus, 0 0 0 2px rgba(37, 99, 235, .14)); }
.ai-entry.is-disabled { background: var(--ui-bg, #f3f6fa); }
.entry-sparkle { position: absolute; top: 12px; left: 10px; width: 18px; height: 18px; color: #007bff; pointer-events: none; }
.entry-input { display: block; width: 100%; min-width: 0; height: 42px; min-height: 42px; max-height: 96px; margin: 0; padding: 11px 12px 11px 36px; border: 0; border-radius: var(--ui-radius, 6px); outline: none; background: transparent; color: var(--ui-text, #1f2937); font: inherit; font-size: var(--ui-font-size, 13px); line-height: 20px; resize: none; overflow-y: auto; }
.entry-input::placeholder { color: var(--ui-muted, #64748b); opacity: 1; }
.entry-actions { position: absolute; top: 5px; right: 5px; display: flex; align-items: center; gap: 6px; }
.attach-button { display: inline-flex; align-items: center; justify-content: center; gap: 5px; min-height: var(--ui-control-height-sm, 32px); padding: 6px 8px; border: 0; border-radius: var(--ui-radius, 6px); background: transparent; color: var(--ui-muted, #64748b); font: inherit; font-size: var(--ui-font-size, 13px); line-height: 1.25; cursor: pointer; }
.attach-button svg { width: 16px; height: 16px; }
.attach-button:hover:not(:disabled) { background: var(--ui-bg, #f3f6fa); color: #007bff; }
.attach-button:disabled { opacity: .5; cursor: default; }
.process-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: var(--ui-control-height-sm, 32px); padding: 6px 8px 6px 10px; border: 1px solid #007bff; border-radius: var(--ui-radius, 6px); background: #007bff; color: #fff; font: inherit; font-size: var(--ui-font-size, 13px); font-weight: 600; line-height: 1.25; cursor: pointer; }
.process-button:hover:not(:disabled) { border-color: #0062cc; background: #0069d9; }
.process-button:disabled { opacity: .65; cursor: default; }
.process-button kbd { display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; padding: 0; border: 1px solid rgba(255, 255, 255, .3); border-radius: 3px; background: rgba(255, 255, 255, .12); color: inherit; font: inherit; font-size: 12px; box-shadow: none; }
.process-button .spinner-border { width: 13px; height: 13px; }
.attach-button:focus-visible, .process-button:focus-visible, .remove-file:focus-visible, .clear-command:focus-visible { outline: 2px solid #7aa7f7; outline-offset: 2px; }
.clear-command { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 32px; padding: 0; border: 0; border-radius: var(--ui-radius, 6px); background: transparent; color: var(--ui-muted, #64748b); font-size: 11px; cursor: pointer; }
.clear-command:hover:not(:disabled) { background: var(--ui-bg, #f3f6fa); color: var(--ui-text, #1f2937); }
.attached-file { display: flex; align-items: center; gap: 8px; min-width: 0; min-height: 42px; padding: 3px 12px 3px 36px; color: var(--ui-muted, #64748b); }
.attached-file-text { flex: 1; min-width: 0; }
.attached-file strong { display: block; overflow: hidden; color: var(--ui-text, #1f2937); font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.attached-file-text span { display: block; color: var(--ui-muted, #64748b); font-size: 11px; }
.remove-file { flex: 0 0 22px; width: 22px; height: 22px; padding: 0; border: 0; border-radius: var(--ui-radius, 6px); background: transparent; color: var(--ui-muted, #64748b); font-size: 11px; cursor: pointer; }
.remove-file:hover:not(:disabled) { background: var(--ui-bg, #f3f6fa); color: var(--ui-text, #1f2937); }
.entry-error { margin: 7px 0 0; color: #dc3545; font-size: 12px; }
@media (max-width: 575px) {
  .entry-actions { gap: 4px; }
  .attach-button { width: 32px; padding: 6px; }
  .attach-button span, .process-button kbd { display: none; }
  .process-button { padding-right: 8px; padding-left: 8px; }
}
</style>
