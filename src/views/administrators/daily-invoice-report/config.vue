<template>
  <div class="container-fluid py-3 daily-report-config">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Báo cáo hóa đơn ngày</h4>
      <b-button size="sm" variant="outline-primary" @click="load">
        <i class="fas fa-sync-alt mr-1"></i>
        Làm mới
      </b-button>
    </div>

    <b-card class="shadow-sm">
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-calendar-check mr-1"></i>
          Lịch gửi tự động
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="4">
              <b-form-group label="Trạng thái" label-class="font-weight-bold">
                <b-form-checkbox v-model="form.reportEnabled" switch>
                  {{ form.reportEnabled ? 'Đang bật' : 'Đang tắt' }}
                </b-form-checkbox>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="2">
              <b-form-group label="Giờ gửi" label-class="font-weight-bold">
                <b-form-input v-model.number="form.dailyHour" type="number" min="0" max="23" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="2">
              <b-form-group label="Phút gửi" label-class="font-weight-bold">
                <b-form-input v-model.number="form.dailyMinute" type="number" min="0" max="59" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="4">
              <b-form-group label="Thời gian gửi" label-class="font-weight-bold">
                <b-form-input :value="scheduleText" readonly />
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <b-card v-if="form.id" no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-history mr-1"></i>
          Lần gửi gần nhất
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <div class="text-muted small">Ngày báo cáo gần nhất</div>
              <div class="font-weight-bold">{{ form.lastReportDate || '-' }}</div>
            </b-col>
            <b-col cols="12" md="6" class="mt-3 mt-md-0">
              <div class="text-muted small">Thời điểm gửi</div>
              <div class="font-weight-bold">{{ formatDateTime(form.lastSentAt) }}</div>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <div class="d-flex align-items-center">
        <b-button variant="primary" :disabled="saving" @click="save">
          <b-spinner v-if="saving" small class="mr-1"></b-spinner>
          <i v-else class="fas fa-save mr-1"></i>
          Lưu lịch gửi
        </b-button>
        <b-button variant="outline-success" class="ml-2" :disabled="testing" @click="sendReportTest">
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
  name: 'AdminDailyInvoiceReportConfig',
  data() {
    return {
      saving: false,
      testing: false,
      form: {
        id: null,
        reportEnabled: false,
        dailyHour: 1,
        dailyMinute: 0,
        lastReportDate: null,
        lastSentAt: null,
      },
    }
  },
  computed: {
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
      const { data } = await axios.get('/administrator/daily-invoice-report/config')
      this.form = {
        ...this.form,
        ...data,
        reportEnabled: data.reportEnabled != null ? data.reportEnabled : false,
        dailyHour: data.dailyHour != null ? data.dailyHour : 1,
        dailyMinute: data.dailyMinute != null ? data.dailyMinute : 0,
      }
    },
    buildPayload() {
      return {
        reportEnabled: this.form.reportEnabled,
        dailyHour: this.toNumber(this.form.dailyHour, 1),
        dailyMinute: this.toNumber(this.form.dailyMinute, 0),
      }
    },
    toNumber(value, fallback) {
      const number = Number(value)
      return Number.isFinite(number) ? number : fallback
    },
    async save() {
      this.saving = true
      try {
        const { data } = await axios.post('/administrator/daily-invoice-report/config', this.buildPayload())
        this.toast('Đã lưu lịch gửi báo cáo hóa đơn ngày', 'success')
        this.form = { ...this.form, ...data }
      } finally {
        this.saving = false
      }
    },
    async sendReportTest() {
      this.testing = true
      try {
        const { data } = await axios.post('/administrator/daily-invoice-report/send')
        this.toast(data.message || 'Đã gửi báo cáo hóa đơn ngày', 'success')
        this.load()
      } catch (e) {
        const msg = e?.response?.data?.message || 'Không gửi được báo cáo hóa đơn ngày'
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
