<template>
  <div class="right_col" role="main">
    <div class="page-content">
      <div class="page-account">
        <div>
          <!-- HEADER -->
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

          <!-- LOADING -->
          <b-skeleton-wrapper v-if="loading">
            <b-card>
              <div class="p-3" v-for="n in 3" :key="n">
                <b-skeleton animation="wave" width="20%" height="12px" />
                <b-skeleton animation="wave" width="100%" height="12px" class="mt-2" />
              </div>
            </b-card>
          </b-skeleton-wrapper>

          <!-- INFO -->
          <b-card v-else class="profile-card">
            <h4 class="section-title">Thông tin cá nhân</h4>

            <!-- NAME -->
            <div class="list-info">
              <div class="list-item" v-if="!showName">
                <div>
                  <label>Tên</label>
                  <p>{{ account.name }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('name')">Sửa</b-button>
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

            <!-- EMAIL -->
            <div class="list-info">
              <div class="list-item" v-if="!showEmail">
                <div>
                  <label>Email</label>
                  <p>{{ account.email }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('email')">Sửa</b-button>
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

            <!-- PHONE -->
            <div class="list-info">
              <div class="list-item" v-if="!showPhone">
                <div>
                  <label>Điện thoại</label>
                  <p>{{ account.phone }}</p>
                </div>
                <b-button size="sm" variant="light" @click="editForm('phone')">Sửa</b-button>
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
          </b-card>
        </div>
      </div>
    </div>

    <!-- AVATAR MODAL -->
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
      errors: {}
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
      this[`show${k.charAt(0).toUpperCase() + k.slice(1)}`] = true
    },

    closeForm(k) {
      this[`show${k.charAt(0).toUpperCase() + k.slice(1)}`] = false
      this.frmInfo[k] = this.account[k]
    },

    submitForm(data, key) {
      axios.post('/setting/account/update', data).then(() => {
        Object.assign(this.account, data)
        this.closeForm(key)
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

        axios.post('/setting/account/avatar', formData)
          .then(() => {
            this.loadInfo()
            this.closelModel()
          })
      }, 'image/png')
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
/* Layout */
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

/* Card */
.profile-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 10px 30px rgba(0,0,0,.05);
}
.section-title {
  font-weight: 700;
  margin-bottom: 16px;
}

/* Info rows */
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

/* Form */
.form-profile {
  background: #f9fafb;
  padding: 12px;
  border-radius: 10px;
}

/* Buttons */
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

/* Cropper */
.cropper {
  height: 420px;
  border-radius: 12px;
  overflow: hidden;
}
</style>