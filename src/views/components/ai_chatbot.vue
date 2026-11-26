<template>
  <div class="ai-chatbot">
    <b-button ref="toggle" class="chat-toggle" variant="primary" pill @click="toggle">
      <i class="fas fa-robot mr-1"></i>{{ open ? 'Đóng trợ lý' : 'Trợ lý AI' }}
    </b-button>

    <b-card v-if="open" class="chat-panel" no-body role="dialog" aria-label="Trợ lý sử dụng hóa đơn">
      <div class="chat-header">
        <div class="chat-heading">
          <span class="assistant-mark" aria-hidden="true"><i class="fas fa-robot"></i></span>
          <div>
            <strong>Trợ lý sử dụng hóa đơn</strong>
            <div>Tra cứu từ tài liệu đã được duyệt</div>
          </div>
        </div>
        <button type="button" class="header-close" aria-label="Đóng trợ lý" title="Đóng" @click="toggle">
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>

      <div class="conversation-bar">
        <i class="far fa-comment-dots conversation-icon" aria-hidden="true"></i>
        <b-form-select v-model="conversationId" size="sm" :options="conversationOptions"
                       :disabled="controlsLocked" aria-label="Chọn hội thoại" @change="loadMessages" />
        <button type="button" class="toolbar-button" :disabled="controlsLocked" title="Hội thoại mới"
                aria-label="Tạo hội thoại mới" @click="newConversation">
          <i class="fas fa-plus" aria-hidden="true"></i>
        </button>
        <button v-if="conversationId" type="button" class="toolbar-button danger" :disabled="controlsLocked"
                title="Xóa hội thoại" aria-label="Xóa hội thoại" @click="deleteConversation">
          <i class="far fa-trash-alt" aria-hidden="true"></i>
        </button>
      </div>

      <div ref="messages" class="chat-messages">
        <div v-if="loading" class="loading-chat"><b-spinner small /><span>Đang tải hội thoại...</span></div>
        <div v-else-if="messages.length === 0" class="empty-chat">
          <span class="empty-icon" aria-hidden="true"><i class="far fa-comments"></i></span>
          <strong>Tôi có thể hỗ trợ gì?</strong>
          <p>Hỏi về cách sử dụng, mã lỗi hoặc nghiệp vụ hóa đơn.</p>
          <div class="suggestion-list">
            <button v-for="suggestion in suggestions" :key="suggestion" type="button"
                    :disabled="sending" @click="useSuggestion(suggestion)">{{ suggestion }}</button>
          </div>
        </div>
        <div v-for="message in messages" :key="message.id" :class="['message-row', message.role === 'USER' ? 'user' : 'assistant']">
          <div class="message-bubble">
            <div class="message-content">{{ message.content }}</div>
            <details v-if="message.sources && message.sources.length" class="sources">
              <summary><i class="fas fa-book-open" aria-hidden="true"></i>{{ message.sources.length }} nguồn tham khảo</summary>
              <div v-for="(source, index) in message.sources" :key="`${message.id}-${source.chunkId}`" class="source-item">
                [S{{ index + 1 }}] {{ source.title }} · phiên bản {{ source.versionNumber || source.versionId }}
                <span v-if="source.heading"> · {{ source.heading }}</span>
                <span v-if="source.sourceLocation"> · {{ source.sourceLocation }}</span>
              </div>
            </details>
            <div v-if="message.role === 'ASSISTANT'" class="feedback">
              <span>Hữu ích?</span>
              <button type="button" :class="{ selected: message.helpful === true }" aria-label="Câu trả lời hữu ích" @click="rate(message, true)"><i class="far fa-thumbs-up"></i></button>
              <button type="button" :class="{ selected: message.helpful === false }" aria-label="Câu trả lời chưa hữu ích" @click="rate(message, false)"><i class="far fa-thumbs-down"></i></button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="error" class="chat-error" role="alert"><i class="fas fa-exclamation-circle"></i>{{ error }}</div>
      <div class="chat-composer">
        <div class="composer-box" :class="{ 'is-disabled': sending }">
          <textarea ref="questionInput" :value="question" rows="1" maxlength="4000"
                    placeholder="Nhập câu hỏi cho trợ lý..." :disabled="sending"
                    aria-label="Nhập câu hỏi cho trợ lý" @input="updateQuestion"
                    @compositionstart="$event.target.composing = true" @compositionend="$event.target.composing = false"
                    @keydown="questionKeydown"></textarea>
          <button type="button" class="send-button" :disabled="!canSend" aria-label="Gửi câu hỏi" @click="send">
            <b-spinner v-if="sending" small />
            <i v-else class="fas fa-paper-plane" aria-hidden="true"></i>
          </button>
        </div>
        <div class="composer-hint">Enter để gửi · Shift + Enter để xuống dòng</div>
      </div>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AiChatbot',
  data () {
    return {
      open: false,
      loading: false,
      sending: false,
      error: '',
      question: '',
      conversations: [],
      conversationId: null,
      messages: [],
      suggestions: [
        'Cách lập hóa đơn mới?',
        'Xử lý hóa đơn sai sót thế nào?',
        'Tra cứu mã lỗi hóa đơn'
      ]
    }
  },
  computed: {
    controlsLocked () { return this.loading || this.sending },
    canSend () { return !this.sending && !!this.question.trim() },
    conversationOptions () {
      if (!this.conversations.length) return [{ value: null, text: 'Hội thoại mới' }]
      return this.conversations.map(item => ({ value: item.id, text: item.title || `Hội thoại #${item.id}` }))
    }
  },
  watch: {
    question () { this.$nextTick(this.resizeQuestionInput) }
  },
  methods: {
    async toggle () {
      this.open = !this.open
      if (this.open) {
        if (!this.conversations.length) await this.loadConversations()
        this.focusQuestion()
      } else {
        this.$nextTick(() => this.$refs.toggle?.focus())
      }
    },
    async loadConversations () {
      this.loading = true
      this.error = ''
      try {
        const { data } = await axios.get('/ai/chatbot/conversations', { meta: { suppressGlobalErrorToast: true } })
        this.conversations = Array.isArray(data) ? data : []
        if (!this.conversationId && this.conversations.length) this.conversationId = this.conversations[0].id
        if (this.conversationId) await this.loadMessages()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không tải được lịch sử hội thoại'
      } finally {
        this.loading = false
      }
    },
    async loadMessages () {
      if (!this.conversationId) {
        this.messages = []
        return
      }
      this.loading = true
      this.error = ''
      try {
        const { data } = await axios.get(`/ai/chatbot/conversations/${this.conversationId}/messages`, {
          meta: { suppressGlobalErrorToast: true }
        })
        this.messages = Array.isArray(data) ? data : []
        this.scrollBottom()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không tải được nội dung hội thoại'
      } finally {
        this.loading = false
      }
    },
    newConversation () {
      if (this.controlsLocked) return
      this.conversationId = null
      this.messages = []
      this.question = ''
      this.error = ''
      this.focusQuestion()
    },
    async deleteConversation () {
      const conversationId = this.conversationId
      if (this.controlsLocked || !conversationId || !window.confirm('Xóa hội thoại này?')) return
      this.loading = true
      this.error = ''
      try {
        await axios.delete(`/ai/chatbot/conversations/${conversationId}`, {
          meta: { suppressGlobalErrorToast: true }
        })
        this.conversations = this.conversations.filter(item => item.id !== conversationId)
        this.conversationId = this.conversations.length ? this.conversations[0].id : null
        if (this.conversationId) await this.loadMessages()
        else this.messages = []
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không xóa được hội thoại'
      } finally {
        this.loading = false
      }
    },
    async send () {
      const question = String(this.question || '').trim()
      if (!question || this.sending) return
      this.sending = true
      this.error = ''
      try {
        const { data } = await axios.post('/ai/chatbot/messages', {
          conversationId: this.conversationId,
          question
        }, { meta: { suppressGlobalErrorToast: true } })
        this.conversationId = data.conversation.id
        this.messages.push(data.userMessage, data.answer)
        this.question = ''
        const index = this.conversations.findIndex(item => item.id === data.conversation.id)
        if (index >= 0) this.$set(this.conversations, index, data.conversation)
        else this.conversations.unshift(data.conversation)
        this.scrollBottom()
      } catch (e) {
        this.error = e?.response?.data?.message || 'Trợ lý AI tạm thời không khả dụng'
      } finally {
        this.sending = false
      }
    },
    questionKeydown (event) {
      if (event.target.composing || event.isComposing || event.keyCode === 229) return
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.send()
      }
    },
    updateQuestion (event) {
      this.question = event.target.value
    },
    resizeQuestionInput () {
      const input = this.$refs.questionInput
      if (!input || !input.getClientRects().length) return
      input.style.height = 'auto'
      input.style.height = `${Math.min(input.scrollHeight, 96)}px`
    },
    useSuggestion (suggestion) {
      this.question = suggestion
      this.focusQuestion()
    },
    focusQuestion () {
      this.$nextTick(() => this.$refs.questionInput?.focus())
    },
    async rate (message, helpful) {
      if (!this.conversationId || !message?.id) return
      try {
        const { data } = await axios.post(
          `/ai/chatbot/conversations/${this.conversationId}/messages/${message.id}/feedback`,
          { helpful },
          { meta: { suppressGlobalErrorToast: true } }
        )
        message.helpful = data.helpful
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không lưu được đánh giá'
      }
    },
    scrollBottom () {
      this.$nextTick(() => {
        const el = this.$refs.messages
        if (el) el.scrollTop = el.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.chat-toggle { position: fixed; right: 20px; bottom: 92px; z-index: 1040; box-shadow: 0 4px 14px rgba(0, 123, 255, .22); }
.chat-panel { position: fixed; right: 20px; bottom: 142px; z-index: 1040; display: flex; width: 400px; height: min(640px, calc(100vh - 170px)); overflow: hidden; flex-direction: column; border: 1px solid var(--ui-border, #d8dee8); border-radius: 10px; background: #fff; box-shadow: 0 16px 42px rgba(31, 41, 55, .18); color: var(--ui-text, #1f2937); }
.chat-header { display: flex; align-items: center; justify-content: space-between; min-height: 64px; padding: 10px 12px; border-bottom: 1px solid var(--ui-border-soft, #e6ebf2); background: #fff; }
.chat-heading { display: flex; min-width: 0; align-items: center; gap: 9px; }
.chat-heading strong { display: block; color: var(--ui-text, #1f2937); font-size: 14px; font-weight: 700; line-height: 1.35; }
.chat-heading > div > div { margin-top: 1px; color: var(--ui-muted, #64748b); font-size: 11px; line-height: 1.35; }
.assistant-mark { display: inline-flex; flex: 0 0 34px; width: 34px; height: 34px; align-items: center; justify-content: center; border-radius: 9px; background: #eaf3ff; color: #007bff; font-size: 15px; }
.header-close, .toolbar-button, .feedback button { display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 0; background: transparent; color: var(--ui-muted, #64748b); cursor: pointer; }
.header-close { flex: 0 0 30px; width: 30px; height: 30px; border-radius: var(--ui-radius, 6px); }
.header-close:hover { background: var(--ui-bg, #f3f6fa); color: var(--ui-text, #1f2937); }
.conversation-bar { display: flex; min-width: 0; align-items: center; gap: 6px; padding: 8px 10px; border-bottom: 1px solid var(--ui-border-soft, #e6ebf2); background: #fbfcfe; }
.conversation-icon { flex: 0 0 auto; margin-left: 2px; color: var(--ui-muted, #64748b); }
.conversation-bar .custom-select { min-width: 0; height: 32px; min-height: 32px; flex: 1; padding-top: 4px; padding-bottom: 4px; background-color: #fff; font-size: 12px !important; }
.toolbar-button { flex: 0 0 32px; width: 32px; height: 32px; border: 1px solid var(--ui-border, #d8dee8); border-radius: var(--ui-radius, 6px); background: #fff; }
.toolbar-button:hover:not(:disabled) { border-color: #7aa7f7; background: #f5f9ff; color: #007bff; }
.toolbar-button.danger:hover:not(:disabled) { border-color: #f1aeb5; background: #fff5f5; color: #dc3545; }
.toolbar-button:disabled { opacity: .5; cursor: default; }
.chat-messages { flex: 1; min-height: 0; overflow-x: hidden; overflow-y: auto; padding: 14px 12px; background: var(--ui-bg, #f3f6fa); scrollbar-width: thin; }
.loading-chat { display: flex; height: 100%; min-height: 120px; align-items: center; justify-content: center; gap: 8px; color: var(--ui-muted, #64748b); font-size: 12px; }
.message-row { display: flex; margin-bottom: 12px; }
.message-row.user { justify-content: flex-end; }
.message-bubble { max-width: 88%; padding: 9px 11px; border: 1px solid var(--ui-border-soft, #e6ebf2); border-radius: 4px 12px 12px; background: #fff; box-shadow: 0 1px 2px rgba(31, 41, 55, .035); }
.message-row.user .message-bubble { border-color: #007bff; border-radius: 12px 4px 12px 12px; background: #007bff; color: #fff; }
.message-content, .source-item { overflow-wrap: anywhere; word-break: break-word; white-space: pre-wrap; }
.sources { margin-top: 9px; padding-top: 8px; border-top: 1px solid var(--ui-border-soft, #e6ebf2); color: var(--ui-muted, #64748b); font-size: 11px; white-space: normal; }
.sources summary { display: flex; align-items: center; gap: 6px; color: #42638d; font-weight: 600; cursor: pointer; list-style: none; }
.sources summary::-webkit-details-marker { display: none; }
.source-item { margin-top: 6px; padding-left: 18px; line-height: 1.45; }
.feedback { display: flex; align-items: center; gap: 2px; margin-top: 7px; color: var(--ui-muted, #64748b); font-size: 11px; }
.feedback button { width: 25px; height: 25px; border-radius: 5px; font-size: 11px; }
.feedback button:hover { background: var(--ui-bg, #f3f6fa); color: #007bff; }
.feedback button.selected { background: #eaf3ff; color: #007bff; }
.empty-chat { display: flex; min-height: 100%; align-items: center; justify-content: center; flex-direction: column; padding: 24px 20px; color: var(--ui-muted, #64748b); text-align: center; }
.empty-icon { display: inline-flex; width: 46px; height: 46px; align-items: center; justify-content: center; margin-bottom: 10px; border-radius: 14px; background: #eaf3ff; color: #007bff; font-size: 19px; }
.empty-chat strong { color: var(--ui-text, #1f2937); font-size: 14px; }
.empty-chat p { max-width: 270px; margin: 4px 0 13px; font-size: 12px; line-height: 1.5; }
.suggestion-list { display: flex; max-width: 300px; align-items: center; justify-content: center; flex-wrap: wrap; gap: 6px; }
.suggestion-list button { padding: 5px 9px; border: 1px solid var(--ui-border, #d8dee8); border-radius: 14px; background: #fff; color: #52637a; font: inherit; font-size: 11px; cursor: pointer; }
.suggestion-list button:hover { border-color: #7aa7f7; background: #f5f9ff; color: #0069d9; }
.chat-error { display: flex; align-items: flex-start; gap: 6px; padding: 7px 12px 0; background: #fff; color: #dc3545; font-size: 11px; }
.chat-error i { margin-top: 2px; }
.chat-composer { padding: 10px 10px 8px; border-top: 1px solid var(--ui-border-soft, #e6ebf2); background: #fff; }
.composer-box { display: flex; min-width: 0; align-items: flex-end; gap: 7px; padding: 5px 5px 5px 10px; border: 1px solid var(--ui-border, #d8dee8); border-radius: 8px; background: #fff; transition: border-color .15s ease, box-shadow .15s ease; }
.composer-box:focus-within { border-color: #7aa7f7; box-shadow: var(--ui-focus, 0 0 0 2px rgba(37, 99, 235, .14)); }
.composer-box.is-disabled { background: var(--ui-bg, #f3f6fa); }
.composer-box textarea { display: block; min-width: 0; height: 34px; min-height: 34px; max-height: 96px; flex: 1; padding: 7px 0 5px; border: 0; outline: 0; background: transparent; color: var(--ui-text, #1f2937); font: inherit; font-size: 13px; line-height: 20px; resize: none; }
.composer-box textarea::placeholder { color: #8a96a8; }
.send-button { display: inline-flex; flex: 0 0 34px; width: 34px; height: 34px; align-items: center; justify-content: center; padding: 0; border: 1px solid #007bff; border-radius: var(--ui-radius, 6px); background: #007bff; color: #fff; cursor: pointer; }
.send-button:hover:not(:disabled) { border-color: #0062cc; background: #0069d9; }
.send-button:disabled { border-color: #b8d8fa; background: #b8d8fa; cursor: default; }
.composer-hint { margin-top: 5px; color: #8a96a8; font-size: 10px; text-align: right; }
.header-close:focus-visible, .toolbar-button:focus-visible, .suggestion-list button:focus-visible, .feedback button:focus-visible, .send-button:focus-visible { outline: 2px solid #7aa7f7; outline-offset: 2px; }
@media (max-width: 575px) {
  .chat-panel { right: 8px; bottom: 136px; left: 8px; width: auto; height: min(620px, calc(100vh - 150px)); }
  .chat-toggle { right: 12px; }
  .chat-heading > div > div { display: none; }
  .composer-hint { display: none; }
}
</style>
