<template>
  <div class="container-fluid py-3">
    <!-- Header -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">
          Máy chủ gửi mail
        </h4>
      </div>
    </div>

    <b-card class="shadow-sm">
      <!-- SMTP Server -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-server mr-1"></i> Thông tin máy chủ SMTP
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="8">
              <b-form-group label="Máy chủ SMTP" label-class="font-weight-bold">
                <b-form-input
                  v-model="form.host"
                  placeholder="smtp.gmail.com"
                />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="4">
              <b-form-group label="Cổng (Port)" label-class="font-weight-bold">
                <b-form-input
                  v-model.number="form.port"
                  type="number"
                  placeholder="587"
                />
              </b-form-group>
            </b-col>
          </b-row>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Kiểu mã hóa kết nối" label-class="font-weight-bold">
                <b-form-select v-model.number="form.encryption" :options="encryptionOptions" />
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Auth -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-key mr-1"></i> Thông tin đăng nhập
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Tên đăng nhập (Email SMTP)" label-class="font-weight-bold">
                <b-form-input
                  v-model="form.username"
                  type="email"
                  placeholder="Nhập email dùng để đăng nhập SMTP"
                />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label-class="font-weight-bold">
                <template #label>
                  Mật khẩu ứng dụng
                  <a
                    href="https://myaccount.google.com/apppasswords"
                    target="_blank"
                    rel="noopener noreferrer"
                    v-b-tooltip.hover.html
                    title="<b>Bước 1:</b> myaccount.google.com → Security → <em>2-Step Verification</em> → bật lên.<br><b>Bước 2:</b> Quay lại → <em>App passwords</em> → đặt tên bất kỳ → sao chép 16 ký tự."
                    class="ml-1 text-muted small"
                  ><i class="fas fa-question-circle"></i> Hướng dẫn</a>
                </template>
                <b-input-group>
                  <b-form-input
                    v-model="form.password"
                    :type="showPw ? 'text' : 'password'"
                    placeholder="Nhập App Password"
                    autocomplete="new-password"
                  />
                  <b-input-group-append>
                    <b-button variant="outline-secondary" @click="showPw = !showPw" tabindex="-1">
                      <i :class="showPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </b-button>
                  </b-input-group-append>
                </b-input-group>
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- From -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-user-circle mr-1"></i> Thông tin người gửi hiển thị
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Tên người gửi" label-class="font-weight-bold">
                <b-form-input
                  v-model="form.fromName"
                  placeholder="Nhập tên hiển thị trong trường From của email gửi đi"
                />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Địa chỉ email gửi (From)" label-class="font-weight-bold">
                <b-form-input
                  v-model="form.fromEmail"
                  type="email"
                  placeholder="Nhập email hiển thị trong trường From của email gửi đi"
                />
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Actions -->
      <div class="d-flex align-items-center">
        <b-button variant="primary" @click="save" :disabled="saving">
          <b-spinner small v-if="saving" class="mr-1" />
          <i v-else class="fas fa-save mr-1"></i>
          {{ saving ? 'Đang lưu...' : 'Lưu cấu hình' }}
        </b-button>
        <b-button
          variant="outline-success"
          class="ml-2"
          @click="showTestModal = true"
          :disabled="!hasSaved"
        >
          <i class="fas fa-paper-plane mr-1"></i> Gửi mail kiểm tra
        </b-button>
        <span v-if="!hasSaved" class="ml-2 text-muted small">
          <i class="fas fa-info-circle"></i> Lưu cấu hình trước khi kiểm tra
        </span>
      </div>
    </b-card>

    <!-- Modal test -->
    <b-modal
      v-model="showTestModal"
      title="Gửi mail kiểm tra"
      ok-title="Gửi ngay"
      cancel-title="Hủy"
      ok-variant="success"
      :ok-disabled="testing"
      @ok.prevent="sendTest"
      centered
    >
      <b-form-group label="Gửi đến địa chỉ email" label-class="font-weight-bold">
        <b-form-input
          v-model="testEmail"
          type="email"
          placeholder="example@gmail.com"
          @keyup.enter="sendTest"
        />
      </b-form-group>
      <p class="text-muted small mb-0">
        <i class="fas fa-info-circle mr-1"></i>
        Hệ thống sẽ gửi một email kiểm tra đến địa chỉ trên bằng cấu hình SMTP đã lưu.
      </p>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'EmailMailServer',
  data() {
    return {
      form: {
        host:       'smtp.gmail.com',
        port:       587,
        username:   '',
        password:   '',
        fromName:   '',
        fromEmail:  '',
        encryption: 0,
      },
      encryptionOptions: [
        { value: 1, text: 'STARTTLS' },
        { value: 2, text: 'SSL / TLS' },
        { value: 0, text: 'Không mã hóa' },
      ],
      hasSaved:      false,
      saving:        false,
      showPw:        false,
      showTestModal: false,
      testEmail:     '',
      testing:       false,
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      // Load company profile và mail-server song song
      const [mailRes, profileRes] = await Promise.allSettled([
        axios.get('/mail-servers'),
        axios.post('/setting/profile/get'),
      ])

      const mail    = mailRes.status    === 'fulfilled' ? mailRes.value.data    : null
      const profile = profileRes.status === 'fulfilled' ? profileRes.value.data : null

      this.form = {
        host:       mail?.host       || 'smtp.gmail.com',
        port:       mail?.port       || 587,
        username:   mail?.username   || '',
        password:   mail?.password   || '',
        fromName:   mail?.fromName   || profile?.companyName  || '',
        fromEmail:  mail?.fromEmail  || profile?.invoiceEmail || profile?.contactMail || '',
        encryption: mail?.encryption != null ? mail.encryption : 1,
      }
      this.hasSaved = !!mail?.id
    },

    async save() {
      if (!this.form.host || !this.form.username) {
        this.$bvToast.toast('Vui lòng nhập máy chủ và tên đăng nhập', { title: 'Lỗi', variant: 'danger', solid: true })
        return
      }
      if (!this.hasSaved && (!this.form.password || this.form.password.startsWith('•'))) {
        this.$bvToast.toast('Vui lòng nhập mật khẩu ứng dụng', { title: 'Lỗi', variant: 'danger', solid: true })
        return
      }
      this.saving = true
      try {
        await axios.post('/mail-servers', this.form)
        this.$bvToast.toast('Đã lưu cấu hình máy chủ gửi mail', { title: 'Thành công', variant: 'success', solid: true })
        this.hasSaved = true
        this.load()
      } catch (e) {
        const msg = e?.response?.data?.message || 'Lưu thất bại'
        this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true })
      } finally {
        this.saving = false
      }
    },

    async sendTest() {
      if (!this.testEmail || !this.testEmail.includes('@')) {
        this.$bvToast.toast('Vui lòng nhập địa chỉ email hợp lệ', { title: 'Lỗi', variant: 'danger', solid: true })
        return
      }
      this.testing = true
      try {
        const { data } = await axios.post('/mail-servers/test', { email: this.testEmail })
        this.$bvToast.toast(data.message || 'Gửi thành công', { title: 'Thành công', variant: 'success', solid: true })
        this.showTestModal = false
        this.testEmail = ''
      } catch (e) {
        const msg = e?.response?.data?.message || 'Gửi thất bại'
        this.$bvToast.toast(msg, { title: 'Lỗi', variant: 'danger', solid: true })
      } finally {
        this.testing = false
      }
    },
  },
}
</script>

