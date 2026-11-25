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
                <b-form-select
                  v-model.number="form.category"
                  :options="categoryOptions"
                  required
                  @change="onCategoryChange"
                />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Loại thuế suất" label-class="font-weight-bold">
                <b-form-select v-model.number="form.type" :options="typeOptionsForCategory" required />
              </b-form-group>
            </b-col>
            <b-col v-if="Number(form.category) === 6" cols="12" md="6">
              <b-form-group label="Loại phiếu xuất kho" label-class="font-weight-bold">
                <b-form-select v-model.number="form.stockIssueType" :options="stockIssueTypeOptions" required />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Hình thức" label-class="font-weight-bold">
                <b-form-select v-model.number="form.have_code" :options="haveCodeOptions" />
              </b-form-group>
            </b-col>
            <b-col cols="12" md="6">
              <b-form-group label="Trạng thái" label-class="font-weight-bold">
                <b-form-select
                  v-model.number="form.status"
                  :options="statusOptions"
                />
              </b-form-group>
            </b-col>
          </b-row>

          <b-form-group label="Bố cục mẫu JSON" label-class="font-weight-bold" class="mb-0" :required="!id">
            <b-form-file
              v-model="layoutJsonFile"
              class="layout-json-file"
              accept=".json,application/json"
              browse-text="Chọn file JSON"
              placeholder="Kéo thả file JSON vào đây hoặc chọn file"
              drop-placeholder="Thả file JSON vào đây"
            />
          </b-form-group>

          <div class="form-actions mt-4 pt-3 border-top">
            <b-button variant="secondary" :disabled="isSaving" @click="$router.back()">
              Hủy
            </b-button>
            <b-button type="submit" variant="primary" class="ml-2" :disabled="isSaving">
              <b-spinner v-if="isSaving" small class="mr-1" />
              {{ isSaving ? 'Đang lưu…' : 'Lưu mẫu' }}
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
    form: { name: '', category: 1, type: 1, stockIssueType: null, status: 0, have_code: 1 },
    layoutJsonFile: null,
    isSaving: false,
    error: '',
    categoryOptions: [
      { value: 1, text: 'Hóa đơn giá trị gia tăng' },
      { value: 2, text: 'Hóa đơn bán hàng' },
      { value: 6, text: 'Phiếu xuất kho' }
    ],
    typeOptions: [
      { value: -1, text: 'Đã bao gồm thuế' },
      { value: 0, text: 'Không sử dụng thuế suất' },
      { value: 1, text: 'Một thuế suất' },
      { value: 2, text: 'Nhiều thuế suất' }
    ],
    stockIssueTypeOptions: [
      { value: 1, text: 'Phiếu xuất kho kiêm vận chuyển nội bộ' },
      { value: 2, text: 'Phiếu xuất kho hàng gửi bán đại lý' }
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
  computed: {
    typeOptionsForCategory() {
      const category = Number(this.form.category)
      if (category === 2) return this.typeOptions.filter((option) => option.value === -1)
      if (category === 6) return this.typeOptions.filter((option) => option.value === 0)
      return this.typeOptions.filter((option) => option.value === 1 || option.value === 2)
    }
  },
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
          stockIssueType: data.stockIssueType,
          status: data.status,
          have_code: data.haveCode
        }
      } catch (e) {
        this.error = e.response?.data?.message || 'Không tải được mẫu hóa đơn'
      }
    },
    onCategoryChange(category) {
      const value = Number(category)
      if (value === 2) this.form.type = -1
      else if (value === 6) {
        this.form.type = 0
        this.form.stockIssueType = this.form.stockIssueType || 1
      } else {
        this.form.type = 1
        this.form.stockIssueType = null
      }
    },
    async onSubmit() {
      if (this.isSaving || !this.form.name) return
      if (!this.id && !this.layoutJsonFile) {
        this.error = 'Phải tải file JSON bố cục'
        return
      }
      if (this.layoutJsonFile) {
        const fileName = this.layoutJsonFile.name || ''
        if (!/\.json$/i.test(fileName)) {
          this.error = 'Chỉ được tải lên file có đuôi .json'
          return
        }
        if (this.layoutJsonFile.size === 0 || this.layoutJsonFile.size > 10 * 1024 * 1024) {
          this.error = 'File JSON bố cục phải lớn hơn 0 và không quá 10 MB'
          return
        }
        try {
          const layout = JSON.parse(await this.layoutJsonFile.text())
          if (!layout || Array.isArray(layout) || typeof layout !== 'object') {
            this.error = 'Bố cục phải là một JSON object'
            return
          }
        } catch (e) {
          this.error = 'File bố cục không phải JSON hợp lệ'
          return
        }
      }
      this.isSaving = true
      this.error = ''
      const body = new FormData()
      if (this.id) body.append('id', this.id)
      Object.entries(this.form).forEach(([key, value]) => {
        if (value !== null && value !== undefined) body.append(key, value)
      })
      if (this.layoutJsonFile) body.append('layoutJson', this.layoutJsonFile)
      try {
        const { data } = await axios.post('/administrator/form-invoices/saveOrUpdate', body)
        this.$router.push({ name: 'admin-form-invoice-list' })
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
::v-deep .layout-json-file .custom-file-label {
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 0 132px 0 16px;
  border: 1px dashed #a9b8cc;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  transition: border-color .15s ease, background-color .15s ease, box-shadow .15s ease;
}
::v-deep .layout-json-file .custom-file-label::after {
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 16px;
  border-left: 1px solid #dbe3ee;
  border-radius: 0 7px 7px 0;
  background: #fff;
  color: #475467;
  font-weight: 600;
}
::v-deep .layout-json-file .custom-file-input:hover ~ .custom-file-label {
  border-color: #6b8fc5;
  background: #f5f9ff;
}
::v-deep .layout-json-file .custom-file-input:focus ~ .custom-file-label {
  border-color: #4f86e8;
  box-shadow: 0 0 0 3px rgba(79, 134, 232, .15);
}
@media (max-width: 576px) {
  ::v-deep .layout-json-file .custom-file-label {
    padding-right: 112px;
  }
  ::v-deep .layout-json-file .custom-file-label::after {
    padding: 0 10px;
  }
}
</style>
