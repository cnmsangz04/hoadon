<template>
  <div class="container-fluid py-3 form-invoice-create">
    <b-card title="Thông tin mẫu hóa đơn">
      <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
      <b-form @submit.prevent="onSubmit">
        <b-form-group label="Tên mẫu">
          <b-form-input
            v-model.trim="form.name"
            required
            maxlength="255"
            placeholder="Nhập tên mẫu hóa đơn"
          />
        </b-form-group>
        <div class="row">
          <div class="col-md-6">
            <b-form-group label="Loại hóa đơn">
              <b-form-select v-model.number="form.category" :options="categoryOptions" required />
            </b-form-group>
          </div>
          <div class="col-md-6">
            <b-form-group label="Thuế suất">
              <b-form-select v-model.number="form.type" :options="typeOptions" required />
            </b-form-group>
          </div>
          <div class="col-md-6">
            <b-form-group label="Hình thức">
              <b-form-select v-model.number="form.have_code" :options="haveCodeOptions" />
            </b-form-group>
          </div>
          <div class="col-md-6">
            <b-form-group label="Trạng thái">
              <b-form-select v-model.number="form.status" :options="statusOptions" />
            </b-form-group>
          </div>
        </div>
        <b-form-group label="Ảnh đại diện mẫu">
          <input ref="photo" type="file" accept="image/png,image/jpeg" @change="onPhotoChange" />
          <div v-if="photoPreviewUrl" class="mt-3">
            <img :src="photoPreviewUrl" alt="Ảnh đại diện mẫu hóa đơn" class="template-photo" />
          </div>
        </b-form-group>
        <p class="text-muted">
          Lưu thông tin để mở trang thiết kế. Bạn có thể chỉnh nội dung và kéo thả các phần tử trực
          tiếp trên trang hóa đơn.
        </p>
        <div class="text-right">
          <b-button variant="secondary" :disabled="isSaving" @click="$router.back()"
            >Trở lại</b-button
          >
          <b-button type="submit" variant="primary" class="ml-2" :disabled="isSaving">
            {{ isSaving ? 'Đang lưu…' : 'Lưu và thiết kế mẫu' }}
          </b-button>
        </div>
      </b-form>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'AdminFormInvoiceCreate',
  data: () => ({
    id: null,
    form: { name: '', category: 1, type: 1, status: 0, have_code: 1 },
    photoInputObj: null,
    photoPreviewUrl: '',
    photoObjectUrl: '',
    isSaving: false,
    error: '',
    categoryOptions: [
      { value: 1, text: 'Hóa đơn giá trị gia tăng' },
      { value: 2, text: 'Hóa đơn bán hàng' }
    ],
    typeOptions: [
      { value: 1, text: 'Một thuế suất' },
      { value: 2, text: 'Nhiều thuế suất' }
    ],
    haveCodeOptions: [
      { value: 1, text: 'Có mã' },
      { value: 0, text: 'Không mã' }
    ],
    statusOptions: [
      { value: 1, text: 'Kích hoạt' },
      { value: 0, text: 'Chưa kích hoạt' }
    ]
  }),
  created() {
    if (this.$route.params.id) this.load(this.$route.params.id)
  },
  beforeDestroy() {
    this.releasePhoto()
  },
  methods: {
    releasePhoto() {
      if (this.photoObjectUrl) URL.revokeObjectURL(this.photoObjectUrl)
      this.photoObjectUrl = ''
    },
    onPhotoChange(event) {
      this.releasePhoto()
      this.photoInputObj = event.target.files[0] || null
      if (this.photoInputObj) {
        this.photoObjectUrl = URL.createObjectURL(this.photoInputObj)
        this.photoPreviewUrl = this.photoObjectUrl
      }
    },
    async load(id) {
      try {
        const { data } = await axios.get(`/administrator/form-invoices/${id}`)
        this.id = data.id
        this.form = {
          name: data.name,
          category: data.category,
          type: data.type,
          status: data.status,
          have_code: data.haveCode
        }
        this.photoPreviewUrl = data.photo || ''
      } catch (e) {
        this.error = e.response?.data?.message || 'Không tải được mẫu hóa đơn'
      }
    },
    async onSubmit() {
      if (this.isSaving || !this.form.name) return
      this.isSaving = true
      this.error = ''
      const body = new FormData()
      if (this.id) body.append('id', this.id)
      Object.entries(this.form).forEach(([key, value]) => body.append(key, value))
      if (this.photoInputObj) body.append('photo', this.photoInputObj)
      try {
        const { data } = await axios.post('/administrator/form-invoices/saveOrUpdate', body)
        this.$router.push({ name: 'admin-invoice-template-designer', query: { formId: data.id } })
      } catch (e) {
        this.error = e.response?.data?.message || 'Không lưu được mẫu hóa đơn'
      } finally {
        this.isSaving = false
      }
    }
  }
}
</script>

<style scoped>
.form-invoice-create {
  max-width: 1000px;
}
.template-photo {
  max-width: 240px;
  max-height: 300px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}
</style>
