<template>
  <div class="right_col" role="main">
    <div class="page-content">
      <div class="page-account">
        <div>
          <!-- Header -->
          <div class="profile-avatar text-center">
            <div class="img-avatar">
              <b-avatar
                v-if="account.avatar"
                :src="avatarSrc"
                size="6rem"
              />
              <b-avatar
                v-else
                :text="account.nameAvatar"
                size="6rem"
              />
              <div class="ladiui-button" @click="showModal">
                <i class="fas fa-camera"></i>
              </div>
            </div>
            <h1>Xin chào {{ account.name }}</h1>
            <p>{{ $t('account.description') }}</p>
          </div>

          <!-- �ang t?i -->
          <b-skeleton-wrapper v-if="loading">
            <b-card>
              <div class="p-3" v-for="n in 3" :key="n">
                <b-skeleton animation="wave" width="20%" height="12px" />
                <b-skeleton animation="wave" width="100%" height="12px" class="mt-2" />
              </div>
            </b-card>
          </b-skeleton-wrapper>

          <!-- Th�ng tin -->
          <b-card v-else class="profile-card">
            <h4 class="section-title">Thông tin cá nhân</h4>

            <!-- Username (ch? d?c) -->
            <div class="list-info">
              <div class="list-item">
                <div>
                  <label>Tài khoản</label>
                  <p class="font-weight-bold text-primary">{{ account.username || '—' }}</p>
                </div>
              </div>
            </div>

            <!-- T�n -->
            <div class="list-info">
              <div class="list-item" v-if="!showName">
                <div>
                  <label>Tên</label>
                  <p>{{ account.name }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('name')">Cập nhật</b-button>
              </div>
              <b-form v-else @submit="nameSubmit" class="form-profile">
                <b-input-group>
                  <b-form-input
                    v-model="frmInfo.name"
                    placeholder="Họ và tên"
                    required
                    :state="state('name')"
                  />
                  <b-input-group-append>
                    <b-button variant="outline-secondary" @click="closeForm('name')">Hủy</b-button>
                    <b-button type="submit" class="btn-primary">Lưu</b-button>
                  </b-input-group-append>
                </b-input-group>
                <b-form-invalid-feedback>
                  {{ invalidFeedback('name') }}
                </b-form-invalid-feedback>
              </b-form>
            </div>

            <!-- Email -->
            <div class="list-info">
              <div class="list-item" v-if="!showEmail">
                <div>
                  <label>Email</label>
                  <p>{{ account.email }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('email')">Cập nhật</b-button>
              </div>
              <b-form v-else @submit="emailSubmit" class="form-profile">
                <b-input-group>
                  <b-form-input
                    v-model="frmInfo.email"
                    placeholder="Email"
                    required
                    :state="state('email')"
                  />
                  <b-input-group-append>
                    <b-button variant="outline-secondary" @click="closeForm('email')">Hủy</b-button>
                    <b-button type="submit" class="btn-primary">Lưu</b-button>
                  </b-input-group-append>
                </b-input-group>
                <b-form-invalid-feedback>
                  {{ invalidFeedback('email') }}
                </b-form-invalid-feedback>
              </b-form>
            </div>

            <!-- �i?n tho?i -->
            <div class="list-info">
              <div class="list-item" v-if="!showPhone">
                <div>
                  <label>Điện thoại</label>
                  <p>{{ account.phone }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('phone')">Cập nhật</b-button>
              </div>
              <b-form v-else @submit="phoneSubmit" class="form-profile">
                <b-input-group>
                  <b-form-input
                    v-model="frmInfo.phone"
                    placeholder="Số điện thoại"
                    required
                    :state="state('phone')"
                  />
                  <b-input-group-append>
                    <b-button variant="outline-secondary" @click="closeForm('phone')">Hủy</b-button>
                    <b-button type="submit" class="btn-primary">Lưu</b-button>
                  </b-input-group-append>
                </b-input-group>
                <b-form-invalid-feedback>
                  {{ invalidFeedback('phone') }}
                </b-form-invalid-feedback>
              </b-form>
            </div>

            <!-- �?i m?t kh?u -->
            <div class="list-info">
              <div class="list-item" v-if="!showPassword">
                <div>
                  <label>Đổi mật khẩu</label>
                  <p class="text-muted">••••••••</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('password')">Cập nhật</b-button>
              </div>
              <b-form v-else @submit.prevent="passwordSubmit" class="form-profile">
                <b-form-group label="Mật khẩu hiện tại">
                  <b-form-input v-model.trim="frmPassword.current" type="password" required :state="pwdState('current')" />
                </b-form-group>
                <b-form-row>
                  <b-col md="6">
                    <b-form-group label="Mật khẩu mới">
                      <b-form-input v-model="frmPassword.new" type="password" required :state="pwdState('new')" />
                      <small class="text-muted">Tối thiểu 8 ký tự</small>
                    </b-form-group>
                  </b-col>
                  <b-col md="6">
                    <b-form-group label="Nhập lại mật khẩu mới">
                      <b-form-input v-model="frmPassword.confirm" type="password" required :state="pwdState('confirm')" />
                      <small v-if="pwdState('confirm') === false" class="text-danger">Mật khẩu không khớp</small>
                    </b-form-group>
                  </b-col>
                </b-form-row>
                <div class="text-right">
                  <b-button type="submit" class="btn-primary" :disabled="!canSubmitPassword">Đổi mật khẩu</b-button>
                  <b-button variant="outline-secondary" class="ml-2" @click="closeForm('password')">Hủy</b-button>
                </div>
              </b-form>
            </div>
          </b-card>
        </div>
      </div>
    </div>

    <!-- Modal avatar -->
    <b-modal
      ref="profile-photo"
      centered
      hide-header
      size="lg"
      no-close-on-backdrop
      @hidden="onHiddenMol"
    >
      <p class="text-muted text-center mb-2">
        Ảnh đại diện sẽ hiển thị trên hồ sơ của bạn
      </p>

      <div v-if="!imgLogo" class="upload-btn-wrapper">
        <i class="fas fa-cloud-upload-alt"></i>
        <p>Chọn ảnh từ máy tính</p>
        <input type="file" @change="onFileChange" accept="image/*" />
      </div>

      <cropper
        v-else
        class="cropper"
        :src="imgLogo"
        ref="cropper"
        stencil-component="circle-stencil"
      />

      <template #modal-footer>
        <b-button variant="light" @click="closelModel">Hủy</b-button>
        <b-button class="btn-primary" v-if="imgLogo" @click="submitModel">Cập nhật</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import { Cropper } from 'vue-advanced-cropper'

export default {
  name: 'YourAccount',
  components: { Cropper },
  data() {
    return {
      loading: true,
      account: {},
      frmInfo: {},
      showName: false,
      showEmail: false,
      showPhone: false,
      imgLogo: null,
      errors: {},
      showPassword: false,
      frmPassword: { current: '', new: '', confirm: '' }
    }
  },
  mounted() {
    this.loadInfo()
  },
  computed: {
    avatarSrc() {
      const src = this.account?.avatar
      if (!src) return null
      try {
        // If absolute URL to localhost:8081, rewrite to relative so dev proxy handles it
        const u = new URL(src, window.location.origin)
        if (u.hostname === 'localhost' && u.port === '8081' && u.pathname.startsWith('/uploads/')) {
          return u.pathname + u.search
        }
        return src
      } catch {
        // If parsing fails, return as-is
        return src
      }
    },
    canSubmitPassword() {
      const cur = (this.frmPassword.current || '').trim()
      const npw = this.frmPassword.new || ''
      const cf = this.frmPassword.confirm || ''
      const minLenOk = npw.length >= 8
      const matchOk = npw === cf
      return !!cur && minLenOk && matchOk
    }
  },
  methods: {
    async loadInfo() {
      const { data } = await axios.get('/setting/account/info')
      this.account = data
      this.frmInfo = { ...data }
      this.loading = false
    },

    editForm(k) {
      const key = k.charAt(0).toUpperCase() + k.slice(1)
      if (k === 'password') {
        this.showPassword = true
        return
      }
      this[`show${key}`] = true
    },

    closeForm(k) {
      const key = k.charAt(0).toUpperCase() + k.slice(1)
      if (k === 'password') {
        this.showPassword = false
        this.frmPassword = { current: '', new: '', confirm: '' }
        return
      }
      this[`show${key}`] = false
      this.frmInfo[k] = this.account[k]
    },

    submitForm(data, key) {
      axios.post('/setting/account/update', data, { meta: { suppressGlobalErrorToast: true } }).then(() => {
        Object.assign(this.account, data)
        this.closeForm(key)
        this.$toastr && this.$toastr.success('Đã cập nhật thông tin tài khoản')
      })
      .catch(err => {
        const msg = err?.response?.data?.message || err?.response?.data || 'Cập nhật thông tin thất bại'
        if (key) { this.$set(this.errors, key, [msg]) }
        this.$toastr && this.$toastr.error(msg)
      })
    },

    nameSubmit(e) {
      e.preventDefault()
      this.submitForm({ name: this.frmInfo.name }, 'name')
    },

    emailSubmit(e) {
      e.preventDefault()
      this.submitForm({ email: this.frmInfo.email }, 'email')
    },

    phoneSubmit(e) {
      e.preventDefault()
      this.submitForm({ phone: this.frmInfo.phone }, 'phone')
    },

    showModal() {
      this.$refs['profile-photo'].show()
    },

    onHiddenMol() {
      this.imgLogo = null
    },

    onFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      this.imgLogo = URL.createObjectURL(file)
    },

    closelModel() {
      this.$refs['profile-photo'].hide()
    },

    submitModel() {
      const { canvas } = this.$refs.cropper.getResult()
      canvas.toBlob(blob => {
        const formData = new FormData()
        formData.append('avatar', blob, 'avatar.png')
        axios.post('/setting/account/avatar', formData, { meta: { suppressGlobalErrorToast: true } })
          .then(async () => {
            this.loadInfo()
            
            // Also refresh global $app user info so header avatar updates immediately
            try {
              const infoRes = await axios.get('/auth/info', { meta: { suppressGlobalErrorToast: true } })
              const info = infoRes?.data || {}
              if (this.$app) {
                this.$app.info.user = info.user || null
                this.$app.info.company = info.company || null
              }
            } catch (error) {
              console.warn('Failed to refresh global user info after avatar update:', error)
            }
            
            this.closelModel()
            this.$toastr && this.$toastr.success('Đã cập nhật ảnh đại diện')
          })
      }, 'image/png')
    },

    passwordSubmit() {
      if (!this.canSubmitPassword) return
      const payload = { currentPassword: this.frmPassword.current, newPassword: this.frmPassword.new }
      axios.post('/setting/account/change-password', payload, { meta: { suppressGlobalErrorToast: true } })
        .then(() => {
          this.frmPassword = { current: '', new: '', confirm: '' }
          this.$toastr && this.$toastr.success('Đã đổi mật khẩu thành công')
        })
        .catch(err => {
          const msg = err?.response?.data?.message || err?.response?.data || 'Đổi mật khẩu thất bại'
          this.$toastr && this.$toastr.error(msg)
        })
    },

    pwdState(k) {
      const cur = (this.frmPassword.current || '').trim()
      const npw = this.frmPassword.new || ''
      const cf = this.frmPassword.confirm || ''
      if (k === 'current') return cur ? null : false
      if (k === 'new') return npw.length === 0 ? null : (npw.length >= 8)
      if (k === 'confirm') return cf.length === 0 ? null : (npw === cf)
      return null
    },

    state(f) {
      return this.errors[f] ? false : null
    },

    invalidFeedback(f) {
      return (this.errors[f] || []).join(', ')
    }
  }
}
</script>


<style scoped>
/* B? c?c */
.right_col {
  background: #f4f6f9;
  min-height: 100vh;
  padding: 32px 16px;
}
.page-content {
  max-width: 900px;
  margin: auto;
}

/* Header */
.profile-avatar {
  margin-bottom: 24px;
}
.profile-avatar h1 {
  font-size: 24px;
  font-weight: 700;
  margin-top: 12px;
}
.profile-avatar p {
  color: #6b7280;
}

/* Avatar */
.img-avatar {
  position: relative;
  display: inline-block;
}
.ladiui-button {
  position: absolute;
  bottom: -6px;
  right: -6px;
  width: 40px;
  height: 40px;
  background: #2563eb;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(37,99,235,.35);
}

/* Th? */
.profile-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 10px 30px rgba(0,0,0,.05);
}
.section-title {
  font-weight: 700;
  margin-bottom: 16px;
}

/* D�ng th�ng tin */
.list-info {
  padding: 16px 0;
  border-top: 1px solid #eee;
}
.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.list-item label {
  font-size: 13px;
  color: #6b7280;
}
.list-item p {
  margin: 0;
  font-weight: 500;
}

/* Bi?u m?u */
.form-profile {
  background: #f9fafb;
  padding: 12px;
  border-radius: 10px;
}

/* N�t */
.btn-primary {
  background: #2563eb;
  border: none;
}

/* Upload */
.upload-btn-wrapper {
  border: 2px dashed #cbd5e1;
  padding: 40px;
  text-align: center;
  border-radius: 12px;
  color: #64748b;
}
.upload-btn-wrapper i {
  font-size: 32px;
  margin-bottom: 8px;
}
.upload-btn-wrapper input {
  opacity: 0;
  position: absolute;
  inset: 0;
}

/* B? c?t ?nh */
.cropper {
  height: 420px;
  border-radius: 12px;
  overflow: hidden;
}

/* Khu v?c d?i m?t kh?u */
.list-info h5 {
  font-weight: 700;
  color: #111827;
}
.form-profile .form-group label,
.form-profile label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
:deep(.form-profile .form-control),
:deep(.form-profile input.form-control) {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  padding: 10px 12px;
  transition: box-shadow .15s ease, border-color .15s ease;
}
:deep(.form-profile .form-control:focus) {
  border-color: #84a9ff;
  box-shadow: 0 0 0 3px rgba(132, 169, 255, 0.25);
}
.form-profile small.text-muted { color: #9aa3b2 !important; }
.form-profile .text-danger { font-size: 12px; }
.form-profile .btn-primary {
  min-width: 160px;
  border-radius: 10px;
}
.form-profile .b-form-row, .form-profile .row {
  margin-left: -6px;
  margin-right: -6px;
}
.form-profile .col-md-6, .form-profile [class*="col-"] {
  padding-left: 6px;
  padding-right: 6px;
}

/* �?ng b? kho?ng c�ch */
.profile-card .list-info + .list-info { border-top: 1px dashed #eef2f7; }
.profile-card .list-info { padding-top: 14px; padding-bottom: 14px; }

/* �?ng b? m�u n�t */
:deep(.btn-primary) {
  background: linear-gradient(180deg, #4f77ff, #3b66f0);
}
:deep(.btn-primary:hover) { filter: brightness(1.03); }

/* Tinh ch?nh responsive */
@media (max-width: 576px) {
  .form-profile .btn-primary { width: 100%; }
}
</style>