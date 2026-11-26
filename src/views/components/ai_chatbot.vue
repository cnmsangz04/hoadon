<template>
  <div class="ai-chatbot">
    <b-button class="chat-toggle" variant="primary" pill @click="toggle">
      <i class="fas fa-robot mr-1"></i>{{ open ? 'Đóng trợ lý' : 'Trợ lý AI' }}
    </b-button>

    <b-card v-if="open" class="chat-panel shadow-lg" no-body>
      <div class="chat-header">
        <div>
          <strong>Trợ lý sử dụng hóa đơn</strong>
          <div class="small text-muted">Trả lời từ tài liệu đã được duyệt</div>
        </div>
        <div>
          <b-button v-if="conversationId" size="sm" variant="outline-danger" class="mr-1" title="Xóa hội thoại" @click="deleteConversation">
            <i class="fas fa-trash"></i>
          </b-button>
          <b-button size="sm" variant="outline-primary" title="Hội thoại mới" @click="newConversation">
            <i class="fas fa-plus"></i>
          </b-button>
        </div>
      </div>

      <div class="conversation-bar">
        <b-form-select v-model="conversationId" size="sm" :options="conversationOptions" @change="loadMessages" />
      </div>

      <div ref="messages" class="chat-messages">
        <div v-if="loading" class="text-center py-4"><b-spinner small /> Đang tải...</div>
        <div v-else-if="messages.length === 0" class="empty-chat">
          Hãy hỏi cách sử dụng chương trình, mã lỗi hoặc nghiệp vụ có trong tài liệu hướng dẫn.
        </div>
        <div v-for="message in messages" :key="message.id" :class="['message-row', message.role === 'USER' ? 'user' : 'assistant']">
          <div class="message-bubble">
            <div class="message-content">{{ message.content }}</div>
            <div v-if="message.sources && message.sources.length" class="sources">
              <div class="font-weight-bold mb-1"><i class="fas fa-book-open mr-1"></i>Nguồn tham khảo</div>
              <div v-for="(source, index) in message.sources" :key="`${message.id}-${source.chunkId}`" class="source-item">
                [S{{ index + 1 }}] {{ source.title }} · phiên bản {{ source.versionNumber || source.versionId }}
                <span v-if="source.heading"> · {{ source.heading }}</span>
                <span v-if="source.sourceLocation"> · {{ source.sourceLocation }}</span>
              </div>
            </div>
            <div v-if="message.role === 'ASSISTANT'" class="feedback">
              <span class="mr-2">Hữu ích?</span>
              <b-button size="sm" variant="link" :class="{ selected: message.helpful === true }" @click="rate(message, true)">
                <i class="far fa-thumbs-up"></i>
              </b-button>
              <b-button size="sm" variant="link" :class="{ selected: message.helpful === false }" @click="rate(message, false)">
                <i class="far fa-thumbs-down"></i>
              </b-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="error" class="px-3 pt-2 text-danger small">{{ error }}</div>
      <div class="chat-input">
        <b-form-textarea
          v-model.trim="question"
          rows="2"
          max-rows="4"
          maxlength="4000"
          placeholder="Nhập câu hỏi..."
          :disabled="sending"
          @keydown.enter.exact.prevent="send"
        />
        <b-button class="ml-2" variant="primary" :disabled="sending || !question" @click="send">
          <b-spinner v-if="sending" small />
          <i v-else class="fas fa-paper-plane"></i>
        </b-button>
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
      messages: []
    }
  },
  computed: {
    conversationOptions () {
      if (!this.conversations.length) return [{ value: null, text: 'Hội thoại mới' }]
      return this.conversations.map(item => ({ value: item.id, text: item.title || `Hội thoại #${item.id}` }))
    }
  },
  methods: {
    async toggle () {
      this.open = !this.open
      if (this.open && !this.conversations.length) await this.loadConversations()
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
      this.conversationId = null
      this.messages = []
      this.question = ''
      this.error = ''
    },
    async deleteConversation () {
      if (!this.conversationId || !window.confirm('Xóa hội thoại này?')) return
      try {
        await axios.delete(`/ai/chatbot/conversations/${this.conversationId}`, {
          meta: { suppressGlobalErrorToast: true }
        })
        this.conversations = this.conversations.filter(item => item.id !== this.conversationId)
        this.conversationId = this.conversations.length ? this.conversations[0].id : null
        if (this.conversationId) await this.loadMessages()
        else this.messages = []
      } catch (e) {
        this.error = e?.response?.data?.message || 'Không xóa được hội thoại'
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
.chat-toggle { position: fixed; right: 20px; bottom: 92px; z-index: 1040; }
.chat-panel { position: fixed; right: 20px; bottom: 142px; width: 390px; height: min(620px, calc(100vh - 175px)); z-index: 1040; border-radius: 12px; overflow: hidden; display: flex; flex-direction: column; }
.chat-header, .chat-input { display: flex; align-items: center; justify-content: space-between; padding: 12px; background: #fff; }
.chat-header { border-bottom: 1px solid #e7ebf0; }
.conversation-bar { padding: 8px 12px; background: #f7f9fc; border-bottom: 1px solid #e7ebf0; }
.chat-messages { flex: 1; overflow-y: auto; padding: 12px; background: #f5f7fb; }
.message-row { display: flex; margin-bottom: 10px; }
.message-row.user { justify-content: flex-end; }
.message-bubble { max-width: 88%; border-radius: 12px; padding: 9px 11px; background: #fff; border: 1px solid #e2e7ee; white-space: pre-wrap; }
.message-row.user .message-bubble { background: #1666c5; border-color: #1666c5; color: #fff; }
.sources { margin-top: 9px; padding-top: 8px; border-top: 1px solid #dde3eb; font-size: 12px; color: #4f5d6b; white-space: normal; }
.source-item { margin-top: 3px; }
.feedback { display: flex; align-items: center; margin-top: 6px; font-size: 12px; color: #6c757d; }
.feedback .btn { padding: 0 5px; color: #6c757d; }
.feedback .selected { color: #1666c5; }
.chat-input { border-top: 1px solid #e7ebf0; }
.empty-chat { color: #6c757d; text-align: center; padding: 36px 18px; }
@media (max-width: 575px) {
  .chat-panel { right: 8px; left: 8px; width: auto; bottom: 136px; }
  .chat-toggle { right: 12px; }
}
</style>
