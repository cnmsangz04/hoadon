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
            <h4 class="mb-1">Đăng nhập</h4>
            <small class="text-muted">Vào trang chủ để bắt đầu làm việc</small>
          </div>

          <b-form @submit.prevent="onSubmit">
            <b-form-group label="Tài khoản">
              <div class="input-with-icon">
                <i class="bi bi-person"></i>
                <b-form-input v-model.trim="account" type="text" required placeholder="Nhập tài khoản"></b-form-input>
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
              :disabled="loading || !account || !password">
              <span v-if="loading"><b-spinner small class="me-2"/>Đang đăng nhập...</span>
              <span v-else>Đăng nhập</span>
            </b-button>
          </b-form>

          <div v-if="error" class="mt-3 text-danger small text-center">{{ error }}</div>

          <div class="text-center mt-3">
            <small class="text-muted">© {{ new Date().getFullYear() }} hoadon.vn</small>
          </div>

        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios';
import { parseJwt } from "@/utils/jwt";

export default {
  data() {
    return {
      account: "",
      password: "",
      showPassword: false,
      remember: true,
      loading: false,
      error: ""
    };
  },
  mounted() {
    // Prefill last used account (username) to ease login
    try {
      const last = localStorage.getItem('last-account')
      if (last) this.account = last
    } catch {}
  },
  methods: {
    async onSubmit() {
      if (!this.account || !this.password) return;
      // Disallow email-style input
      if (/@/.test(this.account)) {
        const msg = 'Hệ thống chỉ hỗ trợ đăng nhập bằng Tài khoản, không phải email.';
        this.error = msg;
        try { this.$toastr?.warning(msg) } catch {}
        return;
      }

      this.loading = true; this.error = "";

      try {
        // Always login with username
        const payload = { username: this.account.trim(), password: this.password };

        const res = await axios.post("/auth/login", payload, { meta: { suppressGlobalErrorToast: true } });
        const token = res.data?.token || res.data?.accessToken || res.data?.data?.token;
        if (!token) throw new Error("Không tìm thấy token!");
        localStorage.setItem("token", token);
        parseJwt(token);
        if (this.remember) localStorage.setItem("last-account", this.account);
        // Optionally fetch user info after login
        try {
          const infoRes = await axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } })
          const data = infoRes?.data || {}
          if (this.$app) {
            this.$app.info = this.$app.info || {}
            this.$app.info.user = data.user || null
            this.$app.info.company = data.company || null
          }
        } catch {}
        // Navigate to home
        this.$router.push({ name: '/' });
      }
      catch(e) {
        const msg = e?.response?.data?.message || e?.message || 'Đăng nhập thất bại!';
        try { this.$toastr?.error(msg) } catch {}
        this.error = msg
      }
      finally {
        this.loading = false
      }
    }
  }
};
</script>

<style scoped>
/* keep existing styles unchanged */
</style>