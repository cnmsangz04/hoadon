<template>
  <div class="container py-3">
    <h4 class="mb-3">{{ pageTitle }}</h4>

    <b-alert v-if="notify" variant="danger" show class="mb-3">{{ notify }}</b-alert>

    <b-card class="shadow-sm">
      <b-form @submit.prevent="onSubmit">
        <b-row>
          <b-col md="6" class="mb-3">
            <b-form-group label="Tên mẫu" label-for="fi-name" label-cols-sm="4">
              <b-form-input id="fi-name" v-model.trim="form.name" required placeholder="Nhập tên mẫu" />
            </b-form-group>
          </b-col>
          <b-col md="6" class="mb-3">
            <b-form-group label="Ký hiệu" label-for="fi-serial" label-cols-sm="4">
              <b-form-input id="fi-serial" v-model.trim="form.serial" placeholder="Nhập ký hiệu" />
            </b-form-group>
          </b-col>
        </b-row>

        <b-row>
          <b-col md="6" class="mb-3">
            <b-form-group label="Loại hóa đơn" label-for="fi-category" label-cols-sm="4">
              <b-form-select id="fi-category" v-model.number="form.category" :options="categoryOptions">
                <template #first>
                  <b-form-select-option :value="null">Chọn loại hóa đơn</b-form-select-option>
                </template>
              </b-form-select>
            </b-form-group>
          </b-col>
          <b-col md="6" class="mb-3">
            <b-form-group label="Loại thuế suất" label-for="fi-type" label-cols-sm="4">
              <b-form-select id="fi-type" v-model.number="form.type" :options="typeOptions">
                <template #first>
                  <b-form-select-option :value="null">Chọn loại thuế suất</b-form-select-option>
                </template>
              </b-form-select>
            </b-form-group>
          </b-col>
        </b-row>

        <b-row>
          <b-col md="6" class="mb-3">
            <b-form-group label="Đường dẫn file XSLT" label-for="fi-file" label-cols-sm="4">
              <b-form-input id="fi-file" v-model.trim="form.file" placeholder="/uploads/companies.id/photo/xxx.xslt" />
            </b-form-group>
          </b-col>
          <b-col md="6" class="mb-3">
            <b-form-group label="Ảnh mẫu" label-for="fi-photo" label-cols-sm="4">
              <b-form-input id="fi-photo" v-model.trim="form.photo" placeholder="/uploads/companies.id/template/xxx.png" />
            </b-form-group>
          </b-col>
        </b-row>

        <b-row>
          <b-col md="6" class="mb-3" v-if="form.photo">
            <b-card header="Xem trước ảnh">
              <img :src="form.photo" alt="preview" class="img-fluid" />
            </b-card>
          </b-col>
        </b-row>

        <div class="d-flex justify-content-between mt-2">
          <div>
            <b-button variant="secondary" @click="$router.back()">Trở lại</b-button>
          </div>
          <div>
            <b-button :disabled="submitting" variant="primary" type="submit">
              <span v-if="submitting"><i class="fas fa-spinner fa-spin"></i> Đang lưu...</span>
              <span v-else>Lưu mẫu</span>
            </b-button>
          </div>
        </div>
      </b-form>
    </b-card>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerFormInvoiceCreate',
  data() {
    return {
      notify: '',
      submitting: false,
      form: {
        name: '',
        serial: '',
        file: '',
        photo: '',
        type: null,
        category: null,
        status: 1,
        system: 1,
      },
      categoryOptions: [
        { value: 1, text: 'Hóa đơn giá trị gia tăng' },
        { value: 2, text: 'Hóa đơn bán hàng' },
      ],
      typeOptions: [
        { value: 1, text: 'Một thuế suất' },
        { value: 2, text: 'Nhiều thuế suất' },
      ],
    }
  },
  computed: {
    pageTitle() {
      const id = this.$route.params.id
      if (id) return 'Cập nhật/Xem mẫu hóa đơn'
      return 'Thêm mẫu hóa đơn'
    }
  },
  created() {
    const tid = this.$route.query.templateId
    if (tid) this.prefillFromTemplate(tid)
    const id = this.$route.params.id
    if (id) this.loadDetail(id)
  },
  methods: {
    async prefillFromTemplate(id) {
      this.notify = ''
      try {
        // Try template endpoint first; fallback to normal detail
        const { data } = await axios.get(`/form-invoices/templates/${id}`).catch(async () => {
          return await axios.get(`/form-invoices/${id}`)
        })
        const it = data && (data.item || data)
        if (it) {
          this.form.name = it.name || this.form.name
          this.form.serial = it.serial || this.form.serial
          this.form.file = it.file || it.path || this.form.file
          this.form.photo = it.photo || this.form.photo
          this.form.type = it.type != null ? Number(it.type) : this.form.type
          this.form.category = it.category != null ? Number(it.category) : this.form.category
          // Ensure creating new, not system template
          this.form.system = 1
        }
      } catch (e) {
        this.notify = 'Không tải được dữ liệu mẫu. Bạn có thể nhập thủ công.'
      }
    },
    async loadDetail(id) {
      try {
        const { data } = await axios.get(`/form-invoices/${id}`)
        const it = data && (data.item || data)
        if (it) {
          this.form.name = it.name
          this.form.serial = it.serial
          this.form.file = it.file || it.path || ''
          this.form.photo = it.photo || ''
          this.form.type = it.type != null ? Number(it.type) : null
          this.form.category = it.category != null ? Number(it.category) : null
          this.form.status = it.status != null ? Number(it.status) : 1
          this.form.system = it.system != null ? Number(it.system) : 1
        }
      } catch (e) {
        this.notify = 'Không tải được chi tiết mẫu.'
      }
    },
    async onSubmit() {
      this.notify = ''
      this.submitting = true
      try {
        const payload = { ...this.form }
        // company_id is auto-detected by backend via user; do not send
        const id = this.$route.params.id
        if (id) {
          await axios.put(`/form-invoices/${id}`, payload, { successMessage: 'Đã cập nhật mẫu' })
        } else {
          await axios.post('/form-invoices', payload, { successMessage: 'Đã tạo mẫu' })
        }
        this.$router.push({ name: 'CustomerFormInvoiceList' })
      } catch (e) {
        this.notify = 'Lưu mẫu thất bại. Vui lòng kiểm tra và thử lại.'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.img-fluid { max-width: 100%; height: auto; }
</style>
