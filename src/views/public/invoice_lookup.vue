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
      },
      errors: {},
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
    if (this.form.code && this.form.taxcode) this.lookup()
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
        const { data } = await axios.get('/public/invoices/lookup', {
          params: {
            code: this.form.code,
            taxcode: this.form.taxcode,
          },
          meta: { suppressGlobalErrorToast: true },
        })
        this.result = data
      } catch (err) {
        this.error = err?.response?.data?.message || 'Không tìm thấy hóa đơn phù hợp'
      } finally {
        this.loading = false
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
}

@keyframes fadeIn { from {opacity:0; transform:translateY(10px);} to {opacity:1;} }
</style>
