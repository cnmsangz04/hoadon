<template>
  <div class="invoice-ai-tools mb-3">
    <div class="ai-tools-bar">
      <div class="ai-tools-title">
        <i class="fas fa-sparkles"></i>
        <span>Hỗ trợ nhập liệu</span>
      </div>
      <div class="ai-tools-actions" role="group" aria-label="Công cụ hỗ trợ nhập liệu">
        <b-button size="sm" :variant="activeTool === 'document' ? 'primary' : 'outline-secondary'"
                  :aria-pressed="activeTool === 'document'" @click="toggle('document')">
          <i class="far fa-file-image mr-1"></i>Nhận diện chứng từ
        </b-button>
        <b-button v-if="showAssistant" size="sm"
                  :variant="activeTool === 'assistant' ? 'primary' : 'outline-secondary'"
                  :aria-pressed="activeTool === 'assistant'" @click="toggle('assistant')">
          <i class="fas fa-magic mr-1"></i>Trợ lý AI
        </b-button>
      </div>
    </div>
    <div v-show="activeTool === 'document'" class="ai-tool-panel">
      <DocumentRecognition :category="category" @applied="$emit('document-applied', $event)" />
    </div>
    <div v-if="showAssistant" v-show="activeTool === 'assistant'" class="ai-tool-panel">
      <AiInvoiceAssistant ref="assistant" :invoice-id="invoiceId" :form-id="formId" :category="category"
                          :snapshot="snapshot" :apply-locally="category === 6"
                          @applied="$emit('assistant-applied', $event)" />
    </div>
  </div>
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
    return { activeTool: null }
  },
  methods: {
    toggle (tool) {
      if (this.activeTool === 'assistant' && this.$refs.assistant?.proposalId) this.$refs.assistant.cancel()
      this.activeTool = this.activeTool === tool ? null : tool
    }
  }
}
</script>

<style scoped>
.invoice-ai-tools { border: 1px solid #dce4ee; border-radius: 8px; background: #fff; overflow: hidden; }
.ai-tools-bar { align-items: center; display: flex; flex-wrap: wrap; gap: 12px; justify-content: space-between; min-height: 54px; padding: 9px 14px; }
.ai-tools-title { align-items: center; color: #34445c; display: flex; font-size: 13px; font-weight: 700; gap: 8px; white-space: nowrap; }
.ai-tools-title i { align-items: center; background: #eef4ff; border-radius: 6px; color: #3974e6; display: flex; height: 27px; justify-content: center; width: 27px; }
.ai-tools-actions { display: flex; flex-wrap: wrap; gap: 7px; }
.ai-tools-actions .btn { border-radius: 6px; font-size: 12px; font-weight: 600; margin: 0; padding: 5px 10px; }
.ai-tool-panel { border-top: 1px solid #e7ecf3; padding: 14px; }
@media (max-width: 575px) {
  .ai-tools-bar { align-items: stretch; flex-direction: column; }
  .ai-tools-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ai-tools-actions .btn { white-space: normal; }
  .ai-tool-panel { padding: 12px; }
}
</style>
