<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="brand-side">
        <div class="brand-inner">
          <img class="logo" src="@/assets/images/logo/logo-hoadon.png" alt="logo" />
          <h2>Hóa đơn điện tử</h2>
          <p class="text-muted">Quản lý hóa đơn nhanh chóng, an toàn và hiệu quả.</p>
          <router-link class="brand-action" to="/auth/login">
            Đăng nhập hệ thống
          </router-link>
        </div>
      </div>

      <div class="form-side d-flex justify-content-center align-items-center">
        <b-card class="auth-card shadow-lg">
          <div class="text-center mb-4">
            <h4 class="mb-1">Tra cứu hóa đơn</h4>
            <small class="text-muted">Nhập mã tra cứu và mã số thuế bên bán</small>
          </div>

        <b-form @submit.prevent="lookup">
          <b-form-group label="Mã tra cứu" label-for="lookup-code" :state="state('code')">
            <b-form-input
              id="lookup-code"
              v-model.trim="form.code"
              autocomplete="off"
              placeholder="Ví dụ: 8D7F2A..."
              :disabled="loading"
              :state="state('code')"
            />
            <b-form-invalid-feedback :state="state('code')">{{ errors.code }}</b-form-invalid-feedback>
          </b-form-group>

          <b-form-group label="Mã số thuế bên bán" label-for="lookup-taxcode" :state="state('taxcode')">
            <b-form-input
              id="lookup-taxcode"
              v-model.trim="form.taxcode"
              autocomplete="off"
              placeholder="Nhập MST công ty phát hành"
              :disabled="loading"
              :state="state('taxcode')"
            />
            <b-form-invalid-feedback :state="state('taxcode')">{{ errors.taxcode }}</b-form-invalid-feedback>
          </b-form-group>

          <b-form-group label="Mã xác nhận" label-for="lookup-captcha" :state="state('captcha')">
            <div class="captcha-row">
              <button
                type="button"
                class="captcha-image"
                :disabled="captchaLoading || loading"
                :title="captchaLoading ? 'Đang tạo mã xác nhận' : 'Tạo mã xác nhận mới'"
                aria-label="Tạo mã xác nhận mới"
                @click="loadCaptcha"
              >
                <img v-if="captcha.image" :src="captcha.image" alt="Mã xác nhận" />
                <b-spinner v-else-if="captchaLoading" small></b-spinner>
              </button>
              <b-form-input
                id="lookup-captcha"
                v-model.trim="form.captchaAnswer"
                autocomplete="off"
                maxlength="5"
                placeholder="Nhập 5 ký tự"
                :disabled="loading || captchaLoading"
                :state="state('captcha')"
              />
              <button
                type="button"
                class="captcha-refresh"
                :disabled="captchaLoading || loading"
                aria-label="Tải lại mã xác nhận"
                @click="loadCaptcha"
              >
                <i class="fas fa-sync-alt"></i>
              </button>
            </div>
            <b-form-invalid-feedback :state="state('captcha')">{{ errors.captcha }}</b-form-invalid-feedback>
          </b-form-group>

          <b-alert v-if="error" show variant="danger" class="mb-3">{{ error }}</b-alert>

          <b-button type="submit" variant="primary" block :disabled="loading">
            <b-spinner v-if="loading" small class="mr-1"></b-spinner>
            Tra cứu
          </b-button>
        </b-form>

        <div v-if="result" class="lookup-result mt-4">
          <div class="d-flex align-items-center justify-content-between flex-wrap mb-3">
            <h5 class="mb-1">Kết quả tra cứu</h5>
            <b-badge :variant="statusVariant(result.status)" class="status-badge">
              {{ result.statusText }}
            </b-badge>
          </div>

          <dl class="lookup-info">
            <dt>Bên bán</dt>
            <dd>{{ result.companyName || '-' }}</dd>
            <dt>MST bên bán</dt>
            <dd>{{ result.companyTaxcode || '-' }}</dd>
            <dt>Khách hàng</dt>
            <dd>{{ result.customerName || '-' }}</dd>
            <dt>Số hóa đơn</dt>
            <dd>{{ result.invoiceNo || '-' }}</dd>
            <dt>Ký hiệu</dt>
            <dd>{{ formSerial }}</dd>
            <dt>Ngày lập</dt>
            <dd>{{ formatDate(result.dateExport) }}</dd>
            <dt>Thành tiền</dt>
            <dd>{{ formatMoney(result.amount, result.currency) }}</dd>
            <dt>Mã CQT</dt>
            <dd>{{ result.codeCqt || '-' }}</dd>
          </dl>

          <div class="lookup-actions">
            <b-button variant="outline-primary" :href="apiUrl(result.viewUrl)" target="_blank">
              <i class="fas fa-eye mr-1"></i>
              Xem hóa đơn
            </b-button>
            <b-button variant="outline-success" :href="apiUrl(result.pdfUrl)" target="_blank">
              <i class="fas fa-file-pdf mr-1"></i>
              Tải PDF
            </b-button>
            <b-button variant="outline-secondary" :href="apiUrl(result.xmlUrl)" target="_blank">
              <i class="fas fa-file-code mr-1"></i>
              Tải XML
            </b-button>
          </div>
        </div>

        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { firstError, hasErrors, required, taxCode } from '@/utils/validators'

export default {
  name: 'PublicInvoiceLookup',
  data() {
    return {
      loading: false,
      error: '',
      result: null,
      form: {
        code: '',
        taxcode: '',
        captchaAnswer: '',
      },
      errors: {},
      captchaLoading: false,
      captcha: {
        token: '',
        image: '',
      },
    }
  },
  computed: {
    formSerial() {
      if (!this.result) return '-'
      return [this.result.formCode, this.result.serial].filter(Boolean).join('') || '-'
    },
  },
  mounted() {
    const query = this.$route?.query || {}
    this.form.code = query.code || this.form.code
    this.form.taxcode = query.taxcode || this.form.taxcode
    this.loadCaptcha()
  },
  methods: {
    state(field) {
      return this.errors[field] ? false : null
    },
    validate() {
      this.errors = {
        code: required(this.form.code, 'Vui lòng nhập mã tra cứu'),
        taxcode: firstError([
          required(this.form.taxcode, 'Vui lòng nhập mã số thuế bên bán'),
          taxCode(this.form.taxcode),
        ]),
        captcha: firstError([
          required(this.form.captchaAnswer, 'Vui lòng nhập mã xác nhận'),
          this.form.captchaAnswer && this.form.captchaAnswer.length !== 5
            ? 'Mã xác nhận gồm 5 ký tự'
            : '',
        ]),
      }
      Object.keys(this.errors).forEach(key => {
        if (!this.errors[key]) delete this.errors[key]
      })
      return !hasErrors(this.errors)
    },
    async lookup() {
      this.error = ''
      this.result = null
      if (!this.validate()) {
        this.error = firstError(Object.values(this.errors)) || 'Vui lòng kiểm tra lại thông tin tra cứu'
        return
      }
      this.loading = true
      try {
        const { data } = await axios.post('/public/invoices/lookup', {
            code: this.form.code,
            taxcode: this.form.taxcode,
            captchaToken: this.captcha.token,
            captchaAnswer: this.form.captchaAnswer,
          }, {
          meta: { suppressGlobalErrorToast: true },
        })
        this.result = data
      } catch (err) {
        this.error = err?.response?.data?.message || 'Không tìm thấy hóa đơn phù hợp'
        await this.loadCaptcha()
        this.form.captchaAnswer = ''
      } finally {
        this.loading = false
      }
    },
    async loadCaptcha() {
      if (this.captchaLoading) return
      this.captchaLoading = true
      this.captcha = { token: '', image: '' }
      this.form.captchaAnswer = ''
      try {
        const { data } = await axios.get('/public/invoices/captcha', {
          meta: { suppressGlobalErrorToast: true },
        })
        this.captcha.token = data.token || ''
        this.captcha.image = data.image || ''
      } catch {
        this.error = 'Không thể tải mã xác nhận. Vui lòng thử lại.'
      } finally {
        this.captchaLoading = false
      }
    },
    apiUrl(path) {
      if (!path) return '#'
      if (/^https?:\/\//i.test(path)) return path
      const base = axios.defaults.baseURL || '/v1'
      if (path.startsWith(base)) return path
      return `${base.replace(/\/$/, '')}/${String(path).replace(/^\/v1\//, '').replace(/^\//, '')}`
    },
    statusVariant(status) {
      const value = Number(status)
      if (value === 3) return 'success'
      if (value === 7) return 'danger'
      if ([1, 2].includes(value)) return 'info'
      if ([4, 5, 6].includes(value)) return 'warning'
      return 'secondary'
    },
    formatDate(value) {
      if (!value) return '-'
      try {
        const d = new Date(value)
        if (Number.isNaN(d.getTime())) return String(value)
        return d.toLocaleDateString('vi-VN')
      } catch {
        return String(value)
      }
    },
    formatMoney(value, currency) {
      const n = Number(value || 0)
      if (!Number.isFinite(n)) return '-'
      const code = currency || 'VND'
      try {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: code }).format(n)
      } catch {
        return `${new Intl.NumberFormat('vi-VN').format(n)} ${code}`
      }
    },
  },
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  overflow-y: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2f7 0%, #f8f9fb 50%, #f1f4f9 100%);
  padding: 16px;
}

.auth-container {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 24px;
  padding: 0;
}

.brand-side {
  background: rgba(255,255,255,0.85);
  backdrop-filter: saturate(1.2) blur(8px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.06);
  display: flex;
}

.brand-inner {
  margin: auto;
  text-align: center;
  padding: 28px;
}

.logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
  margin-bottom: 10px;
}

.brand-inner h2 {
  margin: 0 0 6px;
  font-weight: 600;
}

.brand-inner p {
  margin: 0;
  color: #6c757d;
}

.brand-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 18px;
  padding: 9px 18px;
  border-radius: 999px;
  background: #eef3ff;
  color: #3b66f0;
  font-weight: 600;
  text-decoration: none;
}

.brand-action:hover {
  background: #e2eaff;
  color: #244cd7;
  text-decoration: none;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 22px 24px;
  border-radius: 16px;
  border: 1px solid rgba(0,0,0,0.04);
  background: rgba(255,255,255,0.92);
  backdrop-filter: saturate(1.2) blur(6px);
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.08);
  animation: fadeIn .28s ease;
}

.auth-card .text-muted {
  color: #6c757d !important;
}

:deep(.form-control),
:deep(input.form-control) {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  transition: box-shadow .15s ease, border-color .15s ease;
}

:deep(.form-control:focus) {
  border-color: #84a9ff;
  box-shadow: 0 0 0 3px rgba(132, 169, 255, 0.25);
}

:deep(.btn-primary) {
  background: linear-gradient(180deg, #4f77ff, #3b66f0);
  border: none;
  border-radius: 12px;
  padding: 10px 14px;
}

:deep(.btn-primary:hover) {
  filter: brightness(1.03);
}

:deep(.alert-danger) {
  border-radius: 10px;
}

.status-badge {
  font-size: 13px;
  padding: 7px 10px;
}

.lookup-info {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px 12px;
  margin-bottom: 18px;
}

.lookup-info dt {
  color: #64748b;
  font-weight: 600;
}

.lookup-info dd {
  margin: 0;
  color: #1f2937;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.lookup-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 180px 1fr 42px;
  gap: 8px;
  align-items: center;
}

.captcha-image {
  height: 58px;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f6f8fc;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}

.captcha-image img {
  width: 180px;
  height: 58px;
  object-fit: contain;
  display: block;
}

.captcha-refresh {
  width: 42px;
  height: 42px;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  outline: none;
  box-shadow: none;
}

.captcha-refresh:focus,
.captcha-refresh:focus-visible,
.captcha-refresh:active {
  outline: none;
  border-color: #84a9ff;
  box-shadow: 0 0 0 3px rgba(132, 169, 255, 0.18);
}

.captcha-row :deep(.form-control) {
  height: 42px;
  padding-top: 10px;
  padding-bottom: 10px;
}

.captcha-refresh:hover,
.captcha-image:hover {
  border-color: #84a9ff;
}

@media (max-width: 768px) {
  .auth-container {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .brand-side {
    display: none;
  }

  .auth-card {
    width: 100%;
    max-height: none;
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 42px;
  }

  .captcha-row .captcha-image {
    grid-column: 1;
    grid-row: 1;
  }

  .captcha-row :deep(.form-control) {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .captcha-row .captcha-refresh {
    grid-column: 2;
    grid-row: 1;
  }
}

@keyframes fadeIn { from {opacity:0; transform:translateY(10px);} to {opacity:1;} }
</style>
