<template>
  <div class="container-fluid py-3">

    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div class="d-flex align-items-center">
        <h4 class="font-weight-bold">
          {{ isEdit ? 'Cập nhật mẫu email' : 'Tạo mẫu email' }}
        </h4>
      </div>
      <div>
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
          <i class="fas fa-sync-alt"></i> Làm mới
        </b-button>
        <b-button variant="secondary" @click="goBack">
          Quay lại
        </b-button>
      </div>
    </div>

    <b-card>
      <b-form @submit.prevent="submit">

        <!-- Company (read-only) -->
        <b-form-group label="Công ty">
          <b-form-input
              :value="company ? company.name : ''"
              readonly
              disabled
          />
        </b-form-group>

        <!-- Key -->
        <b-form-group
            label="Key"
            description="Dùng để xác định template trong hệ thống"
        >
          <b-form-input
              v-model="form.key"
              :disabled="isEdit"
              required
              placeholder="VD: INVOICE_CREATED"
          />
        </b-form-group>

        <!-- Title -->
        <b-form-group label="Tên template">
          <b-form-input
              v-model="form.title"
              required
              placeholder="Tên template"
          />
        </b-form-group>

        <!-- Type template -->
        <b-form-group label="Loại template">
          <b-form-select
              v-model="form.system"
              :options="templateOptions"
              required
              placeholder="Chọn loại template"
          />
        </b-form-group>

        <!-- Content (TinyMCE) -->
        <b-form-group label="Nội dung">
          <Editor
              api-key="ew2nbl5d37c39mlshjicc6b0ae04a8e3fw9ezdfrqagc5pik"
              v-model="form.content"
              :init="editorConfig"
          />
        </b-form-group>

        <!-- Status -->
        <b-form-group label="Trạng thái">
          <b-form-checkbox
              switch
              v-model="form.status"
              :value="1"
              :unchecked-value="0"
          >
            Hoạt động
          </b-form-checkbox>
        </b-form-group>

        <!-- Actions -->
        <div class="d-flex gap-2">
          <b-button type="submit" variant="success">
            {{ isEdit ? 'Cập nhật' : 'Lưu' }}
          </b-button>

          <b-button variant="secondary" @click="goBack">
            Hủy
          </b-button>
        </div>

      </b-form>
    </b-card>

  </div>
</template>

<script>
import axios from '@/plugins/axios'
import toastr from 'toastr'
import Editor from '@tinymce/tinymce-vue'

export default {
  name: 'MailTemplateCreate',

  components: {Editor},

  data() {
    return {
      company: null,
      isEdit: false,

      form: {
        key: '',
        title: '',
        content: '',
        status: 1,
        system: 0
      },
      templateOptions: [
        {text: 'Template Cá nhân', value: 1},
        {text: 'Đặt làm template hệ thống', value: 0},
      ],

      editorConfig: {
        height: 400,
        menubar: false,
        plugins: [
          'advlist autolink lists link image charmap preview anchor',
          'searchreplace visualblocks code fullscreen',
          'insertdatetime media table help wordcount'
        ],
        toolbar:
            'undo redo | formatselect | ' +
            'bold italic underline | forecolor backcolor | ' +
            'alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist outdent indent | ' +
            'removeformat | code'
      }
    }
  },

  async mounted() {
    // company đã load sẵn từ /auth/info
    this.company = this.$app.info.company

    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      await this.loadDetail(id)
    }
  },

  methods: {
    async loadDetail(id) {
      try {
        const res = await axios.get(`/administrator/mail-template/${id}`)
        const data = res.data

        this.form.key = data.key
        this.form.title = data.title
        this.form.content = data.content
        this.form.status = data.status
        this.form.system = data.system
      } catch (e) {
        toastr.error('Không tải được dữ liệu')
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
          toastr.success('Cập nhật mẫu email thành công')
        } else {
          await axios.post('/administrator/mail-template', {
            key: this.form.key,
            title: this.form.title,
            content: this.form.content,
            status: this.form.status,
            system: this.form.system,
            companyId: this.company.id
          })
          toastr.success('Tạo mẫu email thành công')
        }

        this.$router.push({name: 'admin-email-template-list'})

      } catch (e) {
        console.error(e)
        toastr.error('Thao tác thất bại')
      }
    },

    goBack() {
      this.$router.push({name: 'admin-email-template-list'})
    },

    reload() {
      if (this.isEdit) {
        // Nếu đang edit, load lại từ DB
        const id = this.$route.params.id
        if (id) {
          this.loadDetail(id)
        }
      } else {
        // Nếu tạo mới, reset về mặc định
        this.form = {
          key: '',
          title: '',
          content: '',
          status: 1,
          system: 0
        }
      }
    }
  }
}
</script>

<style scoped>
/* layout gọn gàng */
</style>
