<template>
  <div class="container-fluid py-3 email-template-form">

    <!-- Title + actions -->
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">
        {{ isEdit ? 'Cập nhật mẫu email' : 'Tạo mẫu email mới' }}
      </h4>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i>
          Làm mới
        </b-button>
      </div>
    </div>

    <b-card class="shadow-sm">
      <b-form @submit.prevent="submit" class="template-form">

        <!-- Thông tin cơ bản -->
        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-info-circle text-primary"></i>
            <h6 class="mb-0">Thông tin cơ bản</h6>
          </div>
          <b-row>
            <b-col cols="12" md="6">
              <b-form-group label="Công ty" label-class="font-weight-500">
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-building"></i>
                  </b-input-group-prepend>
                  <b-form-input
                    :value="company ? company.name : ''"
                    readonly
                    disabled
                  />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group 
                label="Loại template"
                label-class="font-weight-500"
              >
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-layer-group"></i>
                  </b-input-group-prepend>
                  <b-form-select
                    v-model="form.system"
                    :options="templateOptions"
                    required
                  />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group 
                label="Mã template (Key)"
                label-class="font-weight-500 required-field"
                description="Dùng để xác định template trong hệ thống"
              >
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-key"></i>
                  </b-input-group-prepend>
                  <b-form-input
                    v-model="form.key"
                    :disabled="isEdit"
                    required
                    placeholder="VD: INVOICE_CREATED"
                  />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12" md="6">
              <b-form-group 
                label="Tên template"
                label-class="font-weight-500 required-field"
              >
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <i class="fas fa-heading"></i>
                  </b-input-group-prepend>
                  <b-form-input
                    v-model="form.title"
                    required
                    placeholder="Nhập tên template"
                  />
                </b-input-group>
              </b-form-group>
            </b-col>

            <b-col cols="12">
              <b-form-group label="Trạng thái" label-class="font-weight-500">
                <b-form-checkbox
                  switch
                  v-model="form.status"
                  :value="1"
                  :unchecked-value="0"
                  size="lg"
                >
                  <span class="ml-2">
                    <i :class="form.status === 1 ? 'fas fa-check-circle text-success' : 'fas fa-times-circle text-secondary'"></i>
                    {{ form.status === 1 ? 'Đang kích hoạt' : 'Ngưng hoạt động' }}
                  </span>
                </b-form-checkbox>
              </b-form-group>
            </b-col>
          </b-row>
        </div>

        <!-- Nội dung template -->
        <div class="form-section">
          <div class="section-header">
            <i class="fas fa-file-alt text-success"></i>
            <h6 class="mb-0">Nội dung template</h6>
          </div>
          <b-form-group 
            label="Nội dung email"
            label-class="font-weight-500 required-field"
            description="Soạn nội dung email template với trình soạn thảo HTML"
          >
            <Editor
              api-key="ew2nbl5d37c39mlshjicc6b0ae04a8e3fw9ezdfrqagc5pik"
              v-model="form.content"
              :init="editorConfig"
            />
          </b-form-group>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
	      <b-button size="sm" variant="secondary" @click="goBack">
	        <i class="fas fa-arrow-left"></i>
	         Quay lại
	      </b-button>
          <b-button
            type="submit"
            variant="primary"
            size="sm"
            class="btn-form-save"
          >
            <i class="fas fa-save"></i>
            {{ isEdit ? 'Cập nhật' : 'Lưu' }}
          </b-button>
        </div>

      </b-form>
    </b-card>

  </div>
</template>

<script>
import axios from '@/plugins/axios'
import Editor from '@tinymce/tinymce-vue'

export default {
  name: 'MailTemplateCreate',

  components: { Editor },

  data() {
    return {
      company: null,
      isEdit: false,

      form: {
        key: '',
        title: '',
        content: '',
        status: 1,
        system: 1
      },

      templateOptions: [
        { text: 'Template cá nhân', value: 1 },
        { text: 'Template hệ thống', value: 0 }
      ],

      editorConfig: {
        height: 500,
        menubar: false,
        plugins: [
          'advlist autolink lists link image charmap preview anchor',
          'searchreplace visualblocks code fullscreen',
          'insertdatetime media table help wordcount'
        ],
        toolbar:
          'undo redo | formatselect | ' +
          'bold italic underline strikethrough | forecolor backcolor | ' +
          'alignleft aligncenter alignright alignjustify | ' +
          'bullist numlist outdent indent | ' +
          'link image | removeformat | code | help',
        content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; }'
      }
    }
  },

  async mounted() {
    this.initCompany()
  },

  methods: {
    initCompany() {
      const timer = setInterval(() => {
        const company = this.$app?.info?.company

        if (company && company.id) {
          clearInterval(timer)
          this.company = company
          
          const id = this.$route.params.id
          if (id) {
            this.isEdit = true
            this.loadDetail(id)
          }
        }
      }, 50)
    },

    async loadDetail(id) {
      try {
        const res = await axios.get(`/administrator/mail-template/${id}`)
        const data = res.data

        this.form.key = data.key
        this.form.title = data.title
        this.form.content = data.content
        this.form.status = data.status
        this.form.system = data.system
      } catch (err) {
        const message = err.response?.data?.message || 'Không tải được dữ liệu mẫu email'
        this.$toastr && this.$toastr.error(message)
      }
    },

    async submit() {
      try {
        if (this.isEdit) {
          await axios.put(
            `/administrator/mail-template/${this.$route.params.id}`,
            {
              title: this.form.title,
              content: this.form.content,
              status: this.form.status,
              system: this.form.system
            }
          )
          this.$toastr && this.$toastr.success('Cập nhật mẫu email thành công')
        } else {
          await axios.post('/administrator/mail-template', {
            key: this.form.key,
            title: this.form.title,
            content: this.form.content,
            status: this.form.status,
            system: this.form.system,
            companyId: this.company.id
          })
          this.$toastr && this.$toastr.success('Tạo mẫu email thành công')
        }

        this.$router.push({ name: 'admin-email-template-list' })

      } catch (err) {
        console.error(err)
        const message = err.response?.data?.message || 'Thao tác thất bại'
        this.$toastr && this.$toastr.error(message)
      }
    },

    goBack() {
      this.$router.push({ name: 'admin-email-template-list' })
    },

    reload() {
      if (this.isEdit) {
        const id = this.$route.params.id
        if (id) {
          this.loadDetail(id)
        }
      } else {
        this.form = {
          key: '',
          title: '',
          content: '',
          status: 1,
          system: 1
        }
      }
    }
  }
}
</script>

<style scoped>
.email-template-form .card.shadow-sm {
  border-radius: 10px;
}

.email-template-form .btn-outline-primary {
  border-color: #dfe7ff;
}

.email-template-form .btn-outline-primary:hover {
  background: #eef3ff;
}

/* Form Sections */
.template-form .form-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e9ecef;
}

.template-form .form-section:last-of-type {
  margin-bottom: 0;
}

.template-form .section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid #f0f0f0;
}

.template-form .section-header i {
  font-size: 1.25rem;
}

.template-form .section-header h6 {
  font-weight: 600;
  color: #2d3748;
}

/* Form Groups */
.template-form .form-group {
  margin-bottom: 1rem;
}

.template-form .form-group:last-child {
  margin-bottom: 0;
}

.template-form label.font-weight-500 {
  font-weight: 500;
  color: #4a5568;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.template-form .required-field::after {
  content: ' *';
  color: #e53e3e;
}

/* Input Groups */
.template-form .input-group-text {
  background: #f7fafc;
  border-right: none;
  color: #718096;
  min-width: 40px;
  justify-content: center;
}

.template-form .input-group .form-control,
.template-form .input-group .custom-select {
  border-left: none;
}

.template-form .input-group .form-control:focus,
.template-form .input-group .custom-select:focus {
  border-color: #cbd5e0;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

.template-form .input-group:focus-within .input-group-text {
  border-color: #cbd5e0;
  background: #edf2f7;
}

.template-form input.form-control,
.template-form select.form-control,
.template-form .custom-select {
  border: 1px solid #e2e8f0;
  border-radius: 0.375rem;
  padding: 0.625rem 0.875rem;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.template-form input.form-control:focus,
.template-form select.form-control:focus,
.template-form .custom-select:focus {
  outline: none;
  border-color: #4299e1;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
}

.template-form input.form-control:disabled {
  background-color: #f7fafc;
  color: #a0aec0;
  cursor: not-allowed;
}

.template-form .form-text {
  font-size: 0.8rem;
  color: #718096;
  margin-top: 0.375rem;
}

/* Custom Switch */
.template-form .custom-switch {
  padding-left: 2.5rem;
}

.template-form .custom-switch .custom-control-label {
  font-size: 0.95rem;
  color: #4a5568;
  padding-top: 0.125rem;
}

.template-form .custom-control-input:checked ~ .custom-control-label::before {
  background-color: #48bb78;
  border-color: #48bb78;
}

/* TinyMCE Editor */
.template-form .tox-tinymce {
  border: 1px solid #e2e8f0 !important;
  border-radius: 0.375rem !important;
}

.template-form .tox-tinymce:focus-within {
  border-color: #4299e1 !important;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1) !important;
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 1.5rem;
  margin-top: 1.5rem;
  border-top: 1px solid #e2e8f0;
}

.form-actions .btn {
  font-weight: 400;
  border-radius: 0.25rem;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  transition: all 0.2s ease;
}

/* Match header button styles */
.form-actions .btn-secondary {
  background-color: #6c757d;
  border-color: #6c757d;
  color: white;
}

.form-actions .btn-secondary:hover {
  background-color: #5a6268;
  border-color: #545b62;
}

.form-actions .btn-primary {
  background-color: #007bff;
  border-color: #007bff;
  color: white;
}

.form-actions .btn-primary:hover {
  background-color: #0069d9;
  border-color: #0062cc;
}

/* Card Styling */
.card {
  border: 1px solid #e9ecef;
}

.card-body {
  padding: 1.5rem;
}

/* Responsive */
@media (max-width: 768px) {
  .card-body {
    padding: 1rem;
  }

  .template-form .form-section {
    padding: 1rem;
    margin-bottom: 1rem;
  }

  .template-form .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .form-actions {
    flex-direction: column-reverse;
    gap: 0.5rem;
  }

  .form-actions .btn {
    width: 100%;
    min-width: auto;
  }

  .template-form .input-group-text {
    min-width: 36px;
  }
}

/* Animation */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.template-form .form-section {
  animation: slideDown 0.3s ease;
}

.template-form .form-section:nth-child(1) {
  animation-delay: 0.05s;
}

.template-form .form-section:nth-child(2) {
  animation-delay: 0.1s;
}

/* Badge trong switch */
.custom-switch .ml-2 i {
  font-size: 1rem;
  margin-right: 0.25rem;
}
</style>
