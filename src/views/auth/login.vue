<template>
  <div class="auth-page">
    <div class="auth-container">
      
      <div class="brand-side">
        <div class="brand-inner">
          <img src="/logo.png" alt="Logo" class="brand-logo" />
          <h2>Hóa đơn điện tử</h2>
          <p class="text-muted">Quản lý hóa đơn nhanh chóng, an toàn và hiệu quả.</p>
        </div>
      </div>

      <div class="form-side d-flex justify-content-center align-items-center">
        <b-card class="auth-card shadow-lg">

          <div class="text-center mb-4">
            <h4 class="mb-1">Đăng nhập</h4>
            <small class="text-muted">Vào trang chủ để bắt đầu làm việc</small>
          </div>

          <b-alert v-if="error" variant="danger" show class="py-2 px-3">{{ error }}</b-alert>

          <b-form @submit.prevent="onSubmit">
            <b-form-group label="Tài khoản">
              <div class="input-with-icon">
                <i class="bi bi-person"></i>
                <b-form-input v-model.trim="username" required placeholder="Nhập tài khoản"></b-form-input>
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
      username: "",
      password: "",
      showPassword: false,
      remember: true,
      loading: false,
      error: ""
    };
  },
  methods: {
    async onSubmit() {
      if (!this.username || !this.password) return;
      this.loading = true; this.error = "";

      try {
        const res = await axios.post("/auth/login", {
          username: this.username,
          password: this.password
        });

        const token = res.data?.token || res.data?.accessToken || res.data?.data?.token;
        if (!token) throw new Error("Không tìm thấy token!");

        localStorage.setItem("token", token);
        parseJwt(token);

        if (this.remember) localStorage.setItem("last-username", this.username);

        this.$router.push({ name: "/" });
      }
      catch(e) {
        this.error = e.response?.data || "Đăng nhập thất bại!";
      }
      finally { this.loading = false; }
    }
  }
};
</script>

<style scoped>
/* Layout */
.auth-page {
  height: 100vh;
  overflow: hidden; /* no page scrollbar */
  display: flex;
  align-items: center;
  justify-content: center;
  /* soft gradient background */
  background: linear-gradient(135deg, #eef2f7 0%, #f8f9fb 50%, #f1f4f9 100%);
}

.auth-container {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 24px;
  padding: 16px; /* compact padding to avoid overflow */
}

/* Branding */
.brand-side {
  background: rgba(255,255,255,0.85);
  backdrop-filter: saturate(1.2) blur(8px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.06);
  display: flex;
}
.brand-inner { margin: auto; text-align: center; padding: 28px; }
.brand-logo { width: 72px; height: 72px; object-fit: contain; margin-bottom: 10px; }
.brand-inner h2 { margin: 0 0 6px; font-weight: 600; }
.brand-inner p { margin: 0; color: #6c757d; }

/* Card */
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

/* Form */
.input-with-icon { display: flex; align-items: center; gap: .6rem; }
.input-with-icon i { color: #8a94a6; font-size: 1.05rem; }

/* refine input aesthetics */
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

/* Button */
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

/* Alert */
:deep(.alert-danger) {
  border-radius: 10px;
}

/* Responsive */
@media (max-width: 768px) {
  .auth-container { grid-template-columns: 1fr; gap: 12px; padding: 12px; }
  .brand-side { display: none; }
  .auth-card { width: 100%; max-height: none; }
}

/* Animation */
@keyframes fadeIn { from {opacity:0; transform:translateY(10px);} to {opacity:1;} }
</style>