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
          Bot và Group Chat
        </b-card-header>
        <b-card-body>
          <b-form-group label="Bật gửi báo cáo Telegram" label-class="font-weight-bold">
            <b-form-checkbox v-model="form.enabled" switch>
              {{ form.enabled ? 'Đang bật' : 'Đang tắt' }}
            </b-form-checkbox>
          </b-form-group>

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

          <b-form-group label="Group Chat ID" label-class="font-weight-bold">
            <b-form-input
              v-model.trim="form.chatId"
              placeholder="Ví dụ: -1001234567890"
            />
          </b-form-group>
        </b-card-body>
      </b-card>

      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-clock mr-1"></i>
          Lịch gửi tự động
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="4">
              <b-form-group label="Giờ gửi" label-class="font-weight-bold">
                <b-form-input v-model.number="form.dailyHour" type="number" min="0" max="23" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="4">
              <b-form-group label="Phút gửi" label-class="font-weight-bold">
                <b-form-input v-model.number="form.dailyMinute" type="number" min="0" max="59" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="4">
              <b-form-group label="Thời gian hiện tại" label-class="font-weight-bold">
                <b-form-input :value="scheduleText" readonly />
              </b-form-group>
            </b-col>
          </b-row>

          <div class="text-muted small">
            Hệ thống kiểm tra mỗi phút. Khi đến đúng giờ cấu hình, báo cáo ngày hôm qua sẽ được gửi một lần.
          </div>
        </b-card-body>
      </b-card>

      <b-card no-body class="mb-3" v-if="form.id">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-history mr-1"></i>
          Lần gửi gần nhất
        </b-card-header>
        <b-card-body>
          <div>Ngày báo cáo gần nhất: <strong>{{ form.lastReportDate || '-' }}</strong></div>
          <div>Thời điểm gửi: <strong>{{ formatDateTime(form.lastSentAt) }}</strong></div>
        </b-card-body>
      </b-card>

      <div class="d-flex align-items-center flex-wrap">
        <b-button variant="primary" :disabled="saving" @click="save">
          <b-spinner v-if="saving" small class="mr-1"></b-spinner>
          <i v-else class="fas fa-save mr-1"></i>
          Lưu cấu hình
        </b-button>
        <b-button variant="outline-success" class="ml-2" :disabled="testing || !canTest" @click="sendTest">
          <b-spinner v-if="testing" small class="mr-1"></b-spinner>
          <i v-else class="fas fa-paper-plane mr-1"></i>
          Gửi báo cáo thử
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
        dailyHour: 1,
        dailyMinute: 0,
        lastReportDate: null,
        lastSentAt: null,
      },
    }
  },
  computed: {
    canTest() {
      return this.form.enabled && this.form.botTokenConfigured && !!this.form.chatId
    },
    botTokenPlaceholder() {
      return this.form.botTokenConfigured ? 'Đã lưu token, nhập token mới nếu muốn thay đổi' : 'Nhập Bot Token'
    },
    scheduleText() {
      const hh = String(Number(this.form.dailyHour || 0)).padStart(2, '0')
      const mm = String(Number(this.form.dailyMinute || 0)).padStart(2, '0')
      return `${hh}:${mm} hằng ngày`
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
        dailyHour: data.dailyHour != null ? data.dailyHour : 1,
        dailyMinute: data.dailyMinute != null ? data.dailyMinute : 0,
      }
    },
    async save() {
      if (this.form.enabled) {
        if (!this.form.botTokenConfigured && !this.form.botToken) {
          this.toast('Vui lòng nhập Bot Token', 'danger')
          return
        }
        if (!this.form.chatId) {
          this.toast('Vui lòng nhập Group Chat ID', 'danger')
          return
        }
      }
      this.saving = true
      try {
        const payload = {
          enabled: this.form.enabled,
          botToken: this.form.botToken || null,
          chatId: this.form.chatId,
          dailyHour: Number(this.form.dailyHour || 1),
          dailyMinute: Number(this.form.dailyMinute || 0),
        }
        const { data } = await axios.post('/administrator/telegram-config', payload)
        this.toast('Đã lưu cấu hình Telegram', 'success')
        this.form = { ...this.form, ...data, botToken: '' }
      } finally {
        this.saving = false
      }
    },
    async sendTest() {
      this.testing = true
      try {
        const { data } = await axios.post('/administrator/telegram-report/send')
        this.toast(data.message || 'Đã gửi báo cáo Telegram', 'success')
        this.load()
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không gửi được báo cáo Telegram'
        this.toast(msg, 'danger')
      } finally {
        this.testing = false
      }
    },
    formatDateTime(value) {
      if (!value) return '-'
      try {
        const d = new Date(value)
        if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ')
        const dd = String(d.getDate()).padStart(2, '0')
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const yyyy = d.getFullYear()
        const hh = String(d.getHours()).padStart(2, '0')
        const mi = String(d.getMinutes()).padStart(2, '0')
        return `${dd}/${mm}/${yyyy} ${hh}:${mi}`
      } catch {
        return String(value)
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

<style scoped>
.telegram-config {
  max-width: 980px;
}
</style>
