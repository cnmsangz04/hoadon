<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="brand-side">
        <div class="brand-inner">
          <img class="logo" :src="require('@/assets/images/logo/logo-hoadon.png')" alt="logo" />
          <h2>Hóa đơn điện tử</h2>
          <p class="text-muted">Gửi thông tin công ty để quản trị viên duyệt đăng ký.</p>
        </div>
      </div>

      <div class="form-side d-flex justify-content-center align-items-center">
        <b-card class="auth-card shadow-lg">
          <div class="text-center mb-4">
            <h4 class="mb-1">Đăng ký</h4>
            <small class="text-muted">Hồ sơ sẽ được lưu lại và chờ quản trị viên duyệt</small>
          </div>

          <b-form @submit.prevent="submit">
            <b-form-group label="Tên công ty" :state="state('companyName')">
              <div class="input-with-icon">
                <b-form-input v-model.trim="form.companyName" :state="state('companyName')" required placeholder="Nhập tên công ty" />
              </div>
              <b-form-invalid-feedback :state="state('companyName')">{{ errors.companyName }}</b-form-invalid-feedback>
            </b-form-group>

            <b-form-group label="Mã số thuế" :state="state('taxcode')">
              <div class="input-with-icon">
                <b-form-input v-model.trim="form.taxcode" :state="state('taxcode')" required placeholder="Nhập mã số thuế" />
              </div>
              <b-form-invalid-feedback :state="state('taxcode')">{{ errors.taxcode }}</b-form-invalid-feedback>
            </b-form-group>

            <b-form-group label="Địa chỉ" :state="state('address')">
              <div class="input-with-icon">
                <b-form-input v-model.trim="form.address" :state="state('address')" required placeholder="Nhập địa chỉ công ty" />
              </div>
              <b-form-invalid-feedback :state="state('address')">{{ errors.address }}</b-form-invalid-feedback>
            </b-form-group>

            <b-form-row>
              <b-col md="6">
                <b-form-group label="Email" :state="state('email')">
                  <div class="input-with-icon">
                    <b-form-input v-model.trim="form.email" type="email" :state="state('email')" required placeholder="Email công ty" />
                  </div>
                  <b-form-invalid-feedback :state="state('email')">{{ errors.email }}</b-form-invalid-feedback>
                </b-form-group>
              </b-col>
              <b-col md="6">
                <b-form-group label="Điện thoại" :state="state('phone')">
                  <div class="input-with-icon">
                    <b-form-input v-model.trim="form.phone" :state="state('phone')" placeholder="Số điện thoại" />
                  </div>
                  <b-form-invalid-feedback :state="state('phone')">{{ errors.phone }}</b-form-invalid-feedback>
                </b-form-group>
              </b-col>
            </b-form-row>

            <b-form-group label="Người liên hệ">
              <div class="input-with-icon">
                <b-form-input v-model.trim="form.contactName" placeholder="Tên người liên hệ" />
              </div>
            </b-form-group>

            <b-button type="submit" block variant="primary" class="submit-btn" :disabled="loading || !canSubmit">
              <span v-if="loading"><b-spinner small class="mr-2" />Đang gửi đăng ký...</span>
              <span v-else>Gửi đăng ký</span>
            </b-button>
          </b-form>

          <div class="text-center mt-3">
            <b-link @click.prevent="$router.push('/auth/login')">Quay lại đăng nhập</b-link>
          </div>
        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { email, firstError, hasErrors, phone, required, taxCode } from '@/utils/validators'

export default {
  name: 'AuthRegister',
  data() {
    return {
      loading: false,
      form: {
        companyName: '',
        taxcode: '',
        address: '',
        email: '',
        phone: '',
        contactName: '',
      },
      errors: {},
    }
  },
  computed: {
    canSubmit() {
      return this.form.companyName
        && this.form.taxcode
        && this.form.address
        && this.form.email
    },
  },
  methods: {
    state(field) {
      return this.errors[field] ? false : null
    },
    validate() {
      this.errors = {
        companyName: required(this.form.companyName, 'Vui lòng nhập tên công ty'),
        taxcode: firstError([
          required(this.form.taxcode, 'Vui lòng nhập mã số thuế'),
          taxCode(this.form.taxcode),
        ]),
        address: required(this.form.address, 'Vui lòng nhập địa chỉ công ty'),
        email: firstError([
          required(this.form.email, 'Vui lòng nhập email'),
          email(this.form.email),
        ]),
        phone: phone(this.form.phone),
      }
      Object.keys(this.errors).forEach(key => {
        if (!this.errors[key]) delete this.errors[key]
      })
      return !hasErrors(this.errors)
    },
    async submit() {
      if (!this.validate()) return
      this.loading = true
      try {
        const res = await axios.post('/auth/register', { ...this.form }, {
          meta: { suppressGlobalErrorToast: true },
        })
        const message = res?.data?.message || 'Đăng ký thành công. Vui lòng chờ quản trị viên duyệt.'
        this.$toastr && this.$toastr.success(message)
        this.$router.push('/auth/login')
      } catch (e) {
        const data = e?.response?.data
        const msg = (typeof data === 'string' && data) || data?.message || e.message || 'Đăng ký thất bại'
        this.$toastr && this.$toastr.error(msg)
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.auth-page {
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2f7 0%, #f8f9fb 50%, #f1f4f9 100%);
}
.auth-container {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 24px;
  padding: 16px;
}
.brand-side {
  background: rgba(255,255,255,0.85);
  backdrop-filter: saturate(1.2) blur(8px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.06);
  display: flex;
}
.brand-inner { margin: auto; text-align: center; padding: 28px; }
.logo { width: 72px; height: 72px; object-fit: contain; margin-bottom: 10px; }
.brand-inner h2 { margin: 0 0 6px; font-weight: 600; }
.brand-inner p { margin: 0; color: #6c757d; }
.auth-card {
  width: 100%;
  max-width: 500px;
  padding: 22px 24px;
  border-radius: 16px;
  border: 1px solid rgba(0,0,0,0.04);
  background: rgba(255,255,255,0.92);
  backdrop-filter: saturate(1.2) blur(6px);
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.08);
  animation: fadeIn .28s ease;
}
.auth-card .text-muted { color: #6c757d !important; }
.input-with-icon { display: flex; align-items: center; gap: .6rem; }
.input-with-icon i { color: #8a94a6; font-size: 1.05rem; width: 18px; text-align: center; flex: 0 0 18px; }
:deep(.form-control), :deep(input.form-control) {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  transition: box-shadow .15s ease, border-color .15s ease;
}
:deep(.form-control:focus) {
  border-color: #84a9ff;
  box-shadow: 0 0 0 3px rgba(132, 169, 255, 0.25);
}
.submit-btn { border-radius: 12px; padding: 10px 14px; }
:deep(.btn-primary) { background: linear-gradient(180deg, #4f77ff, #3b66f0); border: none; }
:deep(.btn-primary:hover) { filter: brightness(1.03); }
@media (max-width: 768px) {
  .auth-container { grid-template-columns: 1fr; gap: 12px; padding: 12px; }
  .brand-side { display: none; }
  .auth-card { width: 100%; max-width: none; max-height: calc(100vh - 24px); overflow: auto; }
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; } }
</style>
