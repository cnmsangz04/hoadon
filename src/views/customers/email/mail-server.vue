<template>
  <div class="container-fluid py-3">
    <!-- Tiêu đề -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="d-flex align-items-center">
        <h4 class="mb-0 font-weight-bold">
          Máy chủ gửi mail
        </h4>
      </div>
    </div>

    <b-card class="shadow-sm">
      <!-- Máy chủ SMTP -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-server mr-1"></i> Thông tin máy chủ SMTP
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="8">
              <b-form-group label="Máy chủ SMTP" label-class="font-weight-bold" :state="state('host')">
                <b-form-input
                  v-model.trim="form.host"
                  placeholder="smtp.gmail.com"
                  :state="state('host')"
                />
                <b-form-invalid-feedback :state="state('host')">{{ errors.host }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="4">
              <b-form-group label="Cổng (Port)" label-class="font-weight-bold" :state="state('port')">
                <b-form-input
                  v-model.number="form.port"
                  type="number"
                  min="1"
                  max="65535"
                  placeholder="587"
                  :state="state('port')"
                />
                <b-form-invalid-feedback :state="state('port')">{{ errors.port }}</b-form-invalid-feedback>
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

      <!-- Xác thực -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-key mr-1"></i> Thông tin đăng nhập
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Tên đăng nhập (Email SMTP)" label-class="font-weight-bold" :state="state('username')">
                <b-form-input
                  v-model.trim="form.username"
                  type="email"
                  placeholder="Nhập email dùng để đăng nhập SMTP"
                  :state="state('username')"
                />
                <b-form-invalid-feedback :state="state('username')">{{ errors.username }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label-class="font-weight-bold" :state="state('password')">
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
                    :state="state('password')"
                  />
                  <b-input-group-append>
                    <b-button variant="outline-secondary" @click="showPw = !showPw" tabindex="-1">
                      <i :class="showPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
                    </b-button>
                  </b-input-group-append>
                </b-input-group>
                <b-form-invalid-feedback :state="state('password')">{{ errors.password }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Người gửi -->
      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">
          <i class="fas fa-user-circle mr-1"></i> Thông tin người gửi hiển thị
        </b-card-header>
        <b-card-body>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Tên người gửi" label-class="font-weight-bold" :state="state('fromName')">
                <b-form-input
                  v-model.trim="form.fromName"
                  placeholder="Nhập tên hiển thị trong trường From của email gửi đi"
                  :state="state('fromName')"
                />
                <b-form-invalid-feedback :state="state('fromName')">{{ errors.fromName }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Địa chỉ email gửi (From)" label-class="font-weight-bold" :state="state('fromEmail')">
                <b-form-input
                  v-model.trim="form.fromEmail"
                  type="email"
                  placeholder="Nhập email hiển thị trong trường From của email gửi đi"
                  :state="state('fromEmail')"
                />
                <b-form-invalid-feedback :state="state('fromEmail')">{{ errors.fromEmail }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <!-- Thao tác -->
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

    <!-- Hộp thoại kiểm tra -->
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
      <b-form-group label="Gửi đến địa chỉ email" label-class="font-weight-bold" :state="state('testEmail')">
        <b-form-input
          v-model.trim="testEmail"
          type="email"
          placeholder="example@gmail.com"
          :state="state('testEmail')"
          @keyup.enter="sendTest"
        />
        <b-form-invalid-feedback :state="state('testEmail')">{{ errors.testEmail }}</b-form-invalid-feedback>
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
import { email, firstError, hasErrors, numberRange, required } from '@/utils/validators'

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
      errors:        {},
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      // Tải hồ sơ công ty và mail-server song song
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

    state(field) {
      return this.errors[field] ? false : null
    },

    validateSave() {
      this.errors = {
        ...this.errors,
        host: required(this.form.host, 'Vui lòng nhập máy chủ SMTP'),
        port: firstError([
          required(this.form.port, 'Vui lòng nhập cổng SMTP'),
          numberRange(this.form.port, 1, 65535, 'Cổng SMTP phải trong khoảng 1-65535'),
        ]),
        username: firstError([
          required(this.form.username, 'Vui lòng nhập email đăng nhập SMTP'),
          email(this.form.username),
        ]),
        password: !this.hasSaved ? required(this.form.password, 'Vui lòng nhập mật khẩu ứng dụng') : null,
        fromName: required(this.form.fromName, 'Vui lòng nhập tên người gửi'),
        fromEmail: firstError([
          required(this.form.fromEmail, 'Vui lòng nhập email người gửi'),
          email(this.form.fromEmail),
        ]),
        testEmail: this.errors.testEmail || null,
      }
      Object.keys(this.errors).forEach(key => {
        if (!this.errors[key]) delete this.errors[key]
      })
      return !['host', 'port', 'username', 'password', 'fromName', 'fromEmail'].some(key => this.errors[key])
    },

    validateTest() {
      this.errors = {
        ...this.errors,
        testEmail: firstError([
          required(this.testEmail, 'Vui lòng nhập email nhận thử'),
          email(this.testEmail),
        ]),
      }
      Object.keys(this.errors).forEach(key => {
        if (!this.errors[key]) delete this.errors[key]
      })
      return !this.errors.testEmail
    },

    async save() {
      if (!this.validateSave()) {
        this.$bvToast.toast(firstError(Object.values(this.errors)) || 'Vui lòng kiểm tra lại cấu hình', { title: 'Lỗi', variant: 'danger', solid: true })
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
      if (!this.validateTest()) {
        this.$bvToast.toast(this.errors.testEmail || 'Vui lòng nhập địa chỉ email hợp lệ', { title: 'Lỗi', variant: 'danger', solid: true })
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
