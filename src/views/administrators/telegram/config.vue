<template>
  <div class="container-fluid py-3 telegram-config">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Cấu hình Telegram</h4>
      <b-button size="sm" variant="outline-primary" @click="load">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="shadow-sm">
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fab fa-telegram-plane mr-1"></i>
          Thông tin kết nối Telegram
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="4">
              <b-form-group label="Trạng thái" label-class="font-weight-bold">
                <b-form-checkbox v-model="form.enabled" switch>
                  {{ form.enabled ? 'Đang bật' : 'Đang tắt' }}
                </b-form-checkbox>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="8">
              <b-form-group label="Group Chat ID" label-class="font-weight-bold">
                <b-form-input
                  v-model.trim="form.chatId"
                  placeholder="Ví dụ: -1001234567890"
                />
              </b-form-group>
            </b-col>
          </b-row>

          <b-form-group label="Bot Token" label-class="font-weight-bold">
            <b-input-group>
              <b-form-input
                v-model.trim="form.botToken"
                :type="showToken ? 'text' : 'password'"
                autocomplete="new-password"
                :placeholder="botTokenPlaceholder"
              />
              <b-input-group-append>
                <b-button variant="outline-secondary" @click="showToken = !showToken" tabindex="-1">
                  <i :class="showToken ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                </b-button>
              </b-input-group-append>
            </b-input-group>
          </b-form-group>
        </b-card-body>
      </b-card>

      <div class="d-flex align-items-center">
        <b-button variant="primary" :disabled="saving" @click="save">
          <b-spinner v-if="saving" small class="mr-1"></b-spinner>
          <i v-else class="fas fa-save mr-1"></i>
          Lưu cấu hình
        </b-button>
        <b-button variant="outline-success" class="ml-2" :disabled="testing || !canTestTelegram" @click="sendTelegramTest">
          <b-spinner v-if="testing" small class="mr-1"></b-spinner>
          <i v-else class="fas fa-paper-plane mr-1"></i>
          Gửi tin nhắn thử
        </b-button>
      </div>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AdminTelegramConfig',
  data() {
    return {
      saving: false,
      testing: false,
      showToken: false,
      form: {
        id: null,
        enabled: false,
        botToken: '',
        botTokenConfigured: false,
        chatId: '',
      },
    }
  },
  computed: {
    canTestTelegram() {
      return this.form.enabled && this.form.botTokenConfigured && !!this.form.chatId
    },
    botTokenPlaceholder() {
      return this.form.botTokenConfigured ? 'Đã lưu token, nhập token mới nếu muốn thay đổi' : 'Nhập Bot Token'
    },
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const { data } = await axios.get('/administrator/telegram-config')
      this.form = {
        ...this.form,
        ...data,
        botToken: '',
      }
    },
    buildPayload() {
      return {
        enabled: this.form.enabled,
        botToken: this.form.botToken || null,
        chatId: this.form.chatId,
      }
    },
    validateTelegram() {
      if (this.form.enabled) {
        if (!this.form.botTokenConfigured && !this.form.botToken) {
          this.toast('Vui lòng nhập Bot Token', 'danger')
          return false
        }
        if (!this.form.chatId) {
          this.toast('Vui lòng nhập Group Chat ID', 'danger')
          return false
        }
      }
      return true
    },
    async save() {
      if (!this.validateTelegram()) return
      this.saving = true
      try {
        const { data } = await axios.post('/administrator/telegram-config', this.buildPayload())
        this.toast('Đã lưu cấu hình Telegram', 'success')
        this.form = { ...this.form, ...data, botToken: '' }
      } finally {
        this.saving = false
      }
    },
    async sendTelegramTest() {
      this.testing = true
      try {
        const { data } = await axios.post('/administrator/telegram-config/test')
        this.toast(data.message || 'Đã gửi tin nhắn kiểm tra Telegram', 'success')
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không gửi được tin nhắn kiểm tra Telegram'
        this.toast(msg, 'danger')
      } finally {
        this.testing = false
      }
    },
    toast(message, variant) {
      this.$bvToast.toast(message, {
        title: variant === 'success' ? 'Thành công' : 'Thông báo',
        variant,
        solid: true,
      })
    },
  },
}
</script>
