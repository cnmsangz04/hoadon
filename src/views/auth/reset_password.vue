<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="brand-side">
        <div class="brand-inner">
          <img class="logo" :src="require('@/assets/images/logo/logo-hoadon.png')" alt="logo" />
          <h2>Hóa đơn điện tử</h2>
          <p class="text-muted">Quản lý hóa đơn nhanh chóng, an toàn và hiệu quả.</p>
        </div>
      </div>

      <div class="form-side d-flex justify-content-center align-items-center">
        <b-card class="auth-card shadow-lg">
          <div class="text-center mb-4">
            <h4 class="mb-1">Đặt lại mật khẩu</h4>
            <small class="text-muted">Nhập mật khẩu mới cho tài khoản của bạn</small>
          </div>

          <b-form @submit.prevent="submit">
            <b-form-group label="Mật khẩu mới">
              <div class="input-with-icon">
                <i class="bi bi-lock"></i>
                <b-form-input v-model="password" :type="show ? 'text' : 'password'" required placeholder="Mật khẩu mới"></b-form-input>
               </div>
            </b-form-group>
            <b-form-group label="Nhập lại mật khẩu mới">
              <div class="input-with-icon">
                <i class="bi bi-lock"></i>
                <b-form-input v-model="confirm" :type="show ? 'text' : 'password'" required placeholder="Nhập lại mật khẩu"></b-form-input>
              </div>
              <small v-if="password && confirm && password !== confirm" class="text-danger">Mật khẩu không khớp</small>
            </b-form-group>
            <div class="d-flex justify-content-between mb-3">
              <b-form-checkbox v-model="show">Hiển thị mật khẩu</b-form-checkbox>
            </div>
            <b-button type="submit" block variant="primary" class="submit-btn"
                      :disabled="loading || !password || !confirm || password !== confirm">
              <span v-if="loading"><b-spinner small class="me-2"/>Đang cập nhật...</span>
              <span v-else>Cập nhật mật khẩu</span>
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

export default {
  data() {
    return {
      password: '',
      confirm: '',
      show: false,
      loading: false,
      token: this.$route.params.token || ''
    }
  },
  methods: {
    async submit() {
      if (!this.token) {
        try { this.$toastr.error('Thiếu hoặc token không hợp lệ') } catch(_) {}
        return
      }
      if (!this.password || this.password !== this.confirm) return
      this.loading = true
      try {
        await axios.post('/auth/reset-password', { token: this.token, password: this.password }, {
          meta: { suppressGlobalErrorToast: true },
          successMessage: 'Cập nhật mật khẩu thành công'
        })
        this.$router.push('/auth/login')
      } catch (e) {
        const respData = e?.response?.data
        const msg = (typeof respData === 'string' && respData)
          || (respData && respData.message)
          || e.message
          || 'Cập nhật mật khẩu thất bại'
        try { this.$toastr.error(msg) } catch(_) {}
      } finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
/* Bố cục sao chép từ login.vue để giữ nhất quán */
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
.brand-logo, .logo { width: 72px; height: 72px; object-fit: contain; margin-bottom: 10px; }
.brand-inner h2 { margin: 0 0 6px; font-weight: 600; }
.brand-inner p { margin: 0; color: #6c757d; }

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

.input-with-icon { display: flex; align-items: center; gap: .6rem; }
.input-with-icon i { color: #8a94a6; font-size: 1.05rem; }

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

.toggle-btn { margin-left: auto; border-radius: 10px; }
.submit-btn { border-radius: 12px; padding: 10px 14px; }
:deep(.btn-primary) { background: linear-gradient(180deg, #4f77ff, #3b66f0); border: none; }
:deep(.btn-primary:hover) { filter: brightness(1.03); }

@media (max-width: 768px) {
  .auth-container { grid-template-columns: 1fr; gap: 12px; padding: 12px; }
  .brand-side { display: none; }
  .auth-card { width: 100%; max-height: none; }
}

@keyframes fadeIn { from {opacity:0; transform:translateY(10px);} to {opacity:1;} }
</style>
