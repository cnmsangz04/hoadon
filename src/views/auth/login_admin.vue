<template>
  <div class="auth-page">
    <div class="auth-container">
      
      <div class="brand-side">
        <div class="brand-inner">
          <img class="logo" :src="require('@/assets/images/logo/logo-hoadon.png')" alt="logo" />
          <h2>Hóa đơn điện tử</h2>
          <p class="text-muted">Khu vực Quản trị hệ thống.</p>
          <router-link class="brand-action" :to="{ name: 'PublicInvoiceLookup' }">
            Tra cứu hóa đơn
          </router-link>
        </div>
      </div>

      <div class="form-side d-flex justify-content-center align-items-center">
        <b-card class="auth-card shadow-lg">

          <div class="text-center mb-4">
            <h4 class="mb-1">Đăng nhập</h4>
            <small class="text-muted">Vào trang quản trị để bắt đầu làm việc</small>
          </div>

          <b-form @submit.prevent="onSubmit">
            <b-form-group label="Tài khoản">
              <div class="input-with-icon">
                <i class="bi bi-person"></i>
                <b-form-input v-model.trim="username" type="text" required placeholder="Nhập tài khoản"></b-form-input>
              </div>
            </b-form-group>

            <b-form-group label="Mật khẩu">
              <div class="input-with-icon">
                <i class="bi bi-lock"></i>
                <b-form-input 
                  :type="showPassword ? 'text' : 'password'"
                  v-model="password"
                  required 
                  placeholder="Nhập mật khẩu"
                ></b-form-input>

                <b-button variant="outline-secondary" size="sm"
                  class="toggle-btn"
                  @click="showPassword = !showPassword"
                >{{ showPassword ? 'Ẩn' : 'Hiện' }}</b-button>
              </div>
            </b-form-group>

            <div class="d-flex justify-content-between mb-3">
              <b-form-checkbox v-model="remember">Ghi nhớ đăng nhập</b-form-checkbox>
            </div>

            <div class="text-center mb-3">
              <b-link :to="{ name: 'login' }" @click.prevent="$router.push({ name: 'login' })">
                Quay về đăng nhập người dùng
              </b-link>
            </div>

            <b-button type="submit" block variant="primary"
              class="submit-btn"
              :disabled="loading || !username || !password">
              <span v-if="loading"><b-spinner small class="me-2"/>Đang đăng nhập...</span>
              <span v-else>Đăng nhập</span>
            </b-button>
          </b-form>

          <div class="text-center mt-3">
            <small class="text-muted">© {{ new Date().getFullYear() }} hoadon.vn</small>
          </div>

        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { parseJwt } from '@/utils/jwt'

export default {
  data() {
    return {
      username: '',
      password: '',
      showPassword: false,
      remember: true,
      loading: false,
      error: ''
    }
  },
  methods: {
    async onSubmit() {
      if (!this.username || !this.password) return
      this.loading = true; this.error = ''
      try {
        const payload = { username: this.username, password: this.password }
        const res = await axios.post('/auth/login-admin', payload, { meta: { suppressGlobalErrorToast: true } })
        const token = res.data?.token || res.data?.accessToken || res.data?.data?.token
        if (!token) throw new Error('Không tìm thấy token!')
        localStorage.setItem('token-admin', token)
        const payloadJwt = parseJwt(token)
        const role = Number(payloadJwt?.role)
        const companyId = Number(payloadJwt?.companyId ?? payloadJwt?.company_id)
        const rootCompanyClaim = payloadJwt?.rootCompanyAdmin ?? payloadJwt?.root_company_admin ?? payloadJwt?.isRootCompanyAdmin
        const isRoot = role === 0
        const isRootCompanyAdmin = role === 1 && (
          rootCompanyClaim === true ||
          rootCompanyClaim === 'true' ||
          companyId === 1
        )
        if (!isRoot && !isRootCompanyAdmin) {
          localStorage.removeItem('token-admin')
          throw new Error('Tài khoản không có quyền vào khu Quản trị')
        }
        if (this.remember) localStorage.setItem('last-admin-account', this.username)
        // Lấy thông tin app ngay cho ngữ cảnh admin
        try {
          const infoRes = await axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } })
          const data = infoRes?.data || {}
          if (this.$app) {
            this.$app.info.user = data.user || null
            this.$app.info.company = data.company || null
          }
        } catch {}
        this.$router.push({ name: 'admin' })
      }
      catch(e) {
        const msg = e.response?.data?.message || e.response?.data || e.message || 'Đăng nhập thất bại!'
        try { this.$toastr.error(msg) } catch(_) { this.error = msg }
      }
      finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
/* Bố cục */
.auth-page {
  height: 100vh;
  overflow: hidden; /* không hiện thanh cuộn trang */
  display: flex;
  align-items: center;
  justify-content: center;
  /* nền gradient nhẹ */
  background: linear-gradient(135deg, #eef2f7 0%, #f8f9fb 50%, #f1f4f9 100%);
}

.auth-container {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 24px;
  padding: 16px; /* padding gọn để tránh tràn */
}

/* Thương hiệu */
.brand-side {
  background: rgba(255,255,255,0.85);
  backdrop-filter: saturate(1.2) blur(8px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.06);
  display: flex;
}
.brand-inner { margin: auto; text-align: center; padding: 28px; }
.brand-logo, .logo { width: 72px; height: 72px; object-fit: contain; margin-bottom: 10px; }
.brand-inner h2 { margin: 0 0 6px; font-weight: 600; }
.brand-inner p { margin: 0; color: #6c757d; }
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

/* Thẻ */
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
.auth-card .text-muted { color: #6c757d !important; }

/* Biểu mẫu */
.input-with-icon { display: flex; align-items: center; gap: .6rem; }
.input-with-icon i { color: #8a94a6; font-size: 1.05rem; }

/* tinh chỉnh giao diện input */
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

.toggle-btn {
  margin-left: auto;
  border-radius: 10px;
}

/* Nút */
.submit-btn {
  border-radius: 12px;
  padding: 10px 14px;
}
:deep(.btn-primary) {
  background: linear-gradient(180deg, #4f77ff, #3b66f0);
  border: none;
}
:deep(.btn-primary:hover) {
  filter: brightness(1.03);
}

/* Cảnh báo */
:deep(.alert-danger) {
  border-radius: 10px;
}

/* Tương thích màn hình */
@media (max-width: 768px) {
  .auth-container { grid-template-columns: 1fr; gap: 12px; padding: 12px; }
  .brand-side { display: none; }
  .auth-card { width: 100%; max-height: none; }
}

/* Hiệu ứng */
@keyframes fadeIn { from {opacity:0; transform:translateY(10px);} to {opacity:1;} }
</style>
