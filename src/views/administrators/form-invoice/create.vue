<template>
  <div class="container-fluid py-3 form-invoice-create">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Tạo mẫu hóa đơn</h4>
      <b-button size="sm" variant="outline-secondary" :disabled="isSaving" @click="$router.back()">
        <i class="fas fa-arrow-left mr-1"></i>
        Quay lại
      </b-button>
    </div>

    <b-card class="shadow-sm">
      <b-card-header class="bg-light font-weight-bold">Thông tin mẫu hóa đơn</b-card-header>
      <b-card-body>
        <b-alert v-if="error" show variant="danger" class="mb-4">{{ error }}</b-alert>
        <b-form @submit.prevent="onSubmit">
          <b-form-group label="Tên mẫu" label-class="font-weight-bold">
            <b-form-input
              v-model.trim="form.name"
              required
              maxlength="255"
              placeholder="Nhập tên mẫu hóa đơn"
            />
          </b-form-group>

          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Loại hóa đơn" label-class="font-weight-bold">
                <b-form-select v-model.number="form.category" :options="categoryOptions" required />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Loại thuế suất" label-class="font-weight-bold">
                <b-form-select v-model.number="form.type" :options="typeOptions" required />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Hình thức" label-class="font-weight-bold">
                <b-form-select v-model.number="form.have_code" :options="haveCodeOptions" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Trạng thái" label-class="font-weight-bold">
                <b-form-select v-model.number="form.status" :options="statusOptions" />
              </b-form-group>
            </b-col>
          </b-row>

          <b-form-group label="Bố cục mẫu JSON" label-class="font-weight-bold" class="mb-0">
            <b-form-file
              v-model="layoutJsonFile"
              accept=".json,application/json"
              browse-text="Chọn file JSON"
              placeholder="Chưa chọn file bố cục"
              drop-placeholder="Thả file JSON vào đây"
            />
          </b-form-group>

          <div class="form-actions mt-4 pt-3 border-top">
            <b-button variant="secondary" :disabled="isSaving" @click="$router.back()">
              Hủy
            </b-button>
            <b-button type="submit" variant="primary" class="ml-2" :disabled="isSaving">
              <b-spinner v-if="isSaving" small class="mr-1" />
              {{ isSaving ? 'Đang lưu…' : 'Lưu và thiết kế mẫu' }}
            </b-button>
          </div>
        </b-form>
      </b-card-body>
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
    layoutJsonFile: null,
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
  methods: {
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
      if (this.layoutJsonFile) body.append('layoutJson', this.layoutJsonFile)
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
.form-invoice-create .card-header { padding: .75rem 1.25rem; }
.form-invoice-create .card-body { padding: 1.25rem; }
.form-actions { display: flex; justify-content: flex-end; }
</style>
