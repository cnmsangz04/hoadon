<template>
  <div class="container py-3">
    <!-- Phần đầu trang -->
    <div class="page-header d-flex align-items-center justify-content-between mb-3">
      <div>
        <h3 class="page-title mb-0">{{ pageTitle }}</h3>
        <p class="page-subtitle text-muted mb-0">Thiết lập thông tin mẫu hóa đơn và ký hiệu phát hành</p>
      </div>
    </div>

    <!-- Nội dung chính: preview bên trái + form bên phải -->
    <div class="layout-grid layout-left-preview">
      <!-- Panel preview bên trái -->
      <aside class="layout-aside" v-if="form.photo">
        <b-card class="shadow-sm preview-card card-soft">
          <template #header>
            <div class="d-flex align-items-center justify-content-between">
              <span>Xem trước</span>
            </div>
          </template>
          <div class="preview-wrapper">
            <img :src="form.photo" alt="preview" class="img-fluid" />
          </div>
        </b-card>
      </aside>

      <!-- Nội dung form bên phải -->
      <div class="layout-main">
        <b-card class="shadow-sm card-soft">
          <b-card-body>
            <b-form @submit.prevent="onSubmit">
              <div class="section-header">Thông tin mẫu</div>

              <!-- Tên mẫu (row) -->
              <div class="form-row-item">
                <b-form-group label="Tên mẫu" label-for="fi-name" label-cols-sm="3">
                  <b-form-input id="fi-name" v-model.trim="form.name" :state="nameValidState" ref="nameInput" />
                  <b-form-invalid-feedback v-if="nameValidState === false" class="d-block">
                    {{ nameError }}
                  </b-form-invalid-feedback>
                </b-form-group>
              </div>

              <!-- Ký hiệu (row) -->
              <div class="form-row-item">
                <b-form-group label="Ký hiệu" label-for="fi-serial" label-cols-sm="3">
                  <div class="serial-builder">
                    <div class="serial-full mb-2">
                      <label class="serial-full-label">Ký hiệu đầy đủ</label>
                      <b-input-group size="sm">
                        <b-input-group-prepend is-text>
                          <i class="far fa-id-badge"></i>
                        </b-input-group-prepend>
                        <b-form-input id="fi-serial" :value="form.serial" readonly />
                      </b-input-group>
                    </div>
                    <div class="serial-grid">
                      <div class="serial-item">
                        <div class="serial-top">
                          <span class="step-badge">1</span>
                        </div>
                        <b-form-input :value="categoryChar" readonly />
                      </div>
                      <div class="serial-item">
                        <div class="serial-top">
                          <span class="step-badge">2</span>
                        </div>
                        <b-form-select v-model="serialCK" :options="serialCKOptions" />
                      </div>
                      <div class="serial-item">
                        <div class="serial-top">
                          <span class="step-badge">3</span>
                        </div>
                        <b-form-input :value="serialYear" readonly />
                      </div>
                      <div class="serial-item">
                        <div class="serial-top">
                          <span class="step-badge">4</span>
                        </div>
                        <b-form-input value="T" readonly />
                      </div>
                      <div class="serial-item serial-suffix">
                        <div class="serial-top">
                          <span class="step-badge">5</span>
                        </div>
                        <b-form-input
                          v-model.trim="serialSuffix"
                          maxlength="2"
                          placeholder="VD: HD"
                          @input="onSuffixInput"
                          :state="suffixValidState"
                          ref="suffixInput"
                        />
                        <b-form-invalid-feedback v-if="suffixValidState === false" class="d-block">
                          {{ suffixError }}
                        </b-form-invalid-feedback>
                        <small class="text-muted">Tối đa 2 chữ cái viết hoa</small>
                      </div>
                    </div>
                  </div>
                </b-form-group>
              </div>

              <!-- Loại hóa đơn (row) -->
              <div class="form-row-item">
                <b-form-group label="Loại hóa đơn" label-for="fi-category" label-cols-sm="3">
                  <b-form-input id="fi-category" :value="categoryLabel(form.category)" readonly />
                </b-form-group>
              </div>

              <!-- Loại thuế suất (row) -->
              <div class="form-row-item">
                <b-form-group label="Loại thuế suất" label-for="fi-type" label-cols-sm="3">
                  <b-form-input id="fi-type" :value="typeLabel(form.type)" readonly />
                </b-form-group>
              </div>

              <!-- Trạng thái (row) -->
              <div class="form-row-item">
                <b-form-group label="Trạng thái" label-for="fi-status" label-cols-sm="3">
                  <b-form-select id="fi-status" v-model.number="form.status" :options="statusOptions" />
                </b-form-group>
              </div>

              <!-- Hành động -->
              <div class="form-actions sticky-actions d-flex flex-column flex-sm-row justify-content-between align-items-stretch align-items-sm-center mt-3 gap-2">
                <b-button class="btn-back" variant="secondary" @click="$router.back()">
                  <i class="fas fa-arrow-left"></i>
                  <span>Trở lại</span>
                </b-button>
                <b-button class="btn-save" :disabled="submitting" variant="primary" type="submit">
                  <span v-if="submitting"><i class="fas fa-spinner fa-spin"></i> Đang lưu...</span>
                  <span v-else><i class="fas fa-save"></i> Lưu mẫu</span>
                </b-button>
              </div>
            </b-form>
          </b-card-body>
        </b-card>
      </div>
    </div>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
  name: 'CustomerFormInvoiceCreate',
  data() {
    const now = new Date()
    const yy = String(now.getFullYear()).slice(-2)
    return {
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
        form_code: '', // ký tự đầu được lưu riêng ở backend
        have_code: 0, // C -> 1, K -> 0
      },
      // Trạng thái tạo serial
      serialCK: 'C',
      serialCKOptions: [
        { value: 'C', text: 'C' },
        { value: 'K', text: 'K' }
      ],
      serialYear: yy,
      serialSuffix: '',
      suffixError: '',
      suffixValidated: false,
      nameError: '',
      nameValidated: false,
      statusOptions: [
        { value: 1, text: 'Kích hoạt' },
        { value: 0, text: 'Chưa kích hoạt' }
      ],
    }
  },
  computed: {
    pageTitle() {
      const id = this.$route.params.id
      if (id) return 'Cập nhật/Xem mẫu hóa đơn'
      return 'Thêm mẫu hóa đơn'
    },
    categoryChar() {
      const c = Number(this.form.category)
      return c === 1 || c === 2 ? String(c) : ''
    },
    suffixValidState() {
      const suf = (this.serialSuffix || '').trim()
      if (!this.suffixValidated && !suf) return null // trung lập trước khi thử validate
      return /^[A-Z]{2}$/.test(suf)
    },
    nameValidState() {
      const v = (this.form.name || '').trim()
      if (!this.nameValidated && !v) return null
      return v.length > 0
    },
  },
  watch: {
    // Ghép lại serial khi các phần thay đổi
    categoryChar() { this.composeSerial() },
    serialCK() { this.form.have_code = this.serialCK === 'C' ? 1 : 0; this.composeSerial() },
    serialYear() { this.composeSerial() },
    serialSuffix() { this.composeSerial() }
  },
  created() {
    const tid = this.$route.query.templateId
    if (tid) this.prefillFromTemplate(tid)
    const id = this.$route.params.id
    if (id) this.loadDetail(id)
    // Ghép ban đầu với giá trị mặc định
    this.form.have_code = this.serialCK === 'C' ? 1 : 0
    this.composeSerial()
  },
  methods: {
    categoryLabel(v) { return Number(v) === 1 ? 'Hóa đơn giá trị gia tăng' : Number(v) === 2 ? 'Hóa đơn bán hàng' : '' },
    typeLabel(v) { return Number(v) === 1 ? 'Một thuế suất' : Number(v) === 2 ? 'Nhiều thuế suất' : '' },
    composeSerial() {
      const c1 = this.categoryChar
      const c2 = this.serialCK || 'C'
      const c34 = this.serialYear || ''
      const c5 = 'T'
      const suf = (this.serialSuffix || '').toUpperCase().slice(0, 2)
      const serial = `${c1}${c2}${c34}${c5}${suf}`
      this.form.serial = serial
    },
    onSuffixInput() {
      // Chỉ cho phép chữ A-Z, ép viết hoa và tối đa 2 ký tự
      const raw = (this.serialSuffix || '')
      const lettersOnly = raw.replace(/[^a-zA-Z]/g, '')
      this.serialSuffix = lettersOnly.toUpperCase().slice(0, 2)
      // Xóa lỗi và cờ validate khi đang nhập; sẽ validate lại lúc submit
      this.suffixError = ''
      this.suffixValidated = false
      // Đồng thời reset trạng thái trung lập validate tên khi nhập nơi khác
      this.nameError = ''
      this.nameValidated = false
      this.composeSerial()
    },
    async prefillFromTemplate(id) {
      try {
        const { data } = await axios.get(`/form-invoices/templates/${id}`).catch(async () => {
          return await axios.get(`/form-invoices/${id}`)
        })
        const it = data && (data.item || data)
        if (it) {
          this.form.name = it.name || this.form.name
          this.form.file = it.file || it.path || this.form.file
          this.form.photo = it.photo || this.form.photo
          this.form.type = it.type != null ? Number(it.type) : this.form.type
          this.form.category = it.category != null ? Number(it.category) : this.form.category
          // Dựng lại serial đầy đủ nếu backend trả về các trường tách riêng
          const sFull = `${(it.formCode || it.form_code || '')}${(it.serial || '')}`
          const serialStr = (typeof it.serial === 'string' && it.serial.length >= 7) ? it.serial : (sFull.length >= 7 ? sFull : '')
          if (serialStr) {
            const s = serialStr
            this.serialCK = ['C','K'].includes(s[1]) ? s[1] : this.serialCK
            this.serialYear = s.substring(2,4)
            this.serialSuffix = s.substring(5,7).toUpperCase()
          }
          // Ánh xạ have_code từ item nếu có; nếu không thì suy ra từ CK
          this.form.have_code = it.have_code != null ? Number(it.have_code) : (this.serialCK === 'C' ? 1 : 0)
          this.composeSerial()
          // Đảm bảo đang tạo mới, không phải mẫu hệ thống
          this.form.system = 1
        }
      } catch (e) {
        // Đã bỏ dùng notify
      }
    },
    async loadDetail(id) {
      try {
        const { data } = await axios.get(`/form-invoices/${id}`)
        const it = data && (data.item || data)
        if (it) {
          this.form.name = it.name
          this.form.file = it.file || it.path || ''
          this.form.photo = it.photo || ''
          this.form.type = it.type != null ? Number(it.type) : null
          this.form.category = it.category != null ? Number(it.category) : null
          // Dựng lại serial đầy đủ từ formCode + serial khi có
          const sFull = `${(it.formCode || it.form_code || '')}${(it.serial || '')}`
          const serialStr = sFull.length >= 7 ? sFull : (typeof it.serial === 'string' ? it.serial : '')
          if (serialStr && serialStr.length >= 7) {
            const s = serialStr
            this.serialCK = ['C','K'].includes(s[1]) ? s[1] : this.serialCK
            this.serialYear = s.substring(2,4)
            this.serialSuffix = s.substring(5,7).toUpperCase()
            this.form.serial = s
          } else {
            this.composeSerial()
          }
          this.form.status = it.status != null ? Number(it.status) : 1
          this.form.system = it.system != null ? Number(it.system) : 1
          this.form.form_code = (it.formCode || it.form_code || '')
          this.form.have_code = it.have_code != null ? Number(it.have_code) : (this.serialCK === 'C' ? 1 : 0)
        }
      } catch (e) {
        // Đã bỏ gán notify
      }
    },
    async onSubmit() {
      this.suffixError = ''
      this.suffixValidated = true
      this.nameError = ''
      this.nameValidated = true
      // Validate phía client: bắt buộc có tên và serial
      const name = (this.form.name || '').trim()
      const suf = (this.serialSuffix || '').trim()
      // Validate tên
      if (!name) {
        this.nameError = 'Vui lòng nhập Tên mẫu (bắt buộc)'
        this.$nextTick(() => {
          if (this.$refs.nameInput && typeof this.$refs.nameInput.focus === 'function') {
            this.$refs.nameInput.focus()
          }
        })
        return
      }
      // Validate hậu tố serial: phải đúng 2 chữ in hoa
      if (!/^[A-Z]{2}$/.test(suf)) {
        this.suffixError = 'Khoảng ký tự không hợp lệ'
        this.$nextTick(() => {
          if (this.$refs.suffixInput && typeof this.$refs.suffixInput.focus === 'function') {
            this.$refs.suffixInput.focus()
          }
        })
        return
      }
      // Ghép và validate serial
      this.composeSerial()
      const serial = (this.form.serial || '').trim()
      if (!serial) {
        this.suffixError = 'Vui lòng kiểm tra và điền Ký hiệu (serial) (bắt buộc)'
        this.$nextTick(() => {
          if (this.$refs.suffixInput && typeof this.$refs.suffixInput.focus === 'function') {
            this.$refs.suffixInput.focus()
          }
        })
        return
      }

      this.submitting = true
      try {
        const payload = { ...this.form }
        const id = this.$route.params.id
        if (id) {
          await axios.put(`/form-invoices/${id}`, payload, { successMessage: 'Đã cập nhật mẫu' })
        } else {
          const templateId = this.$route.query.templateId
          if (templateId) payload.templateId = templateId
          await axios.post('/form-invoices', payload, { successMessage: 'Đã tạo mẫu' })
        }
        this.$router.push({ name: 'CustomerFormInvoiceList' })
      } catch (e) {
        // Đã bỏ gán notify khi lỗi
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
/* Container và thẻ */
.container { max-width: 1180px; }
.card-soft { border-radius: 14px; box-shadow: 0 6px 20px rgba(17, 24, 39, 0.06); }
.card.shadow-sm { border: 1px solid #e9eef5; }

/* Lưới bố cục: ảnh trái, form phải */
.layout-grid.layout-left-preview { display: grid; grid-template-columns: 360px 1fr; gap: 18px; align-items: start; }
.layout-aside { position: sticky; top: 12px; }

/* Phần đầu trang */
.page-header { padding: 4px 4px 0; }
.page-title { font-weight: 700; color: #0f172a; letter-spacing: -0.2px; }
.page-subtitle { font-size: 0.95rem; }

/* Tiêu đề khu vực */
.section-header { font-weight: 700; color: #1f2937; margin: 8px 0 14px; position: relative; }
.section-header::after { content: ""; display: block; height: 2px; background: linear-gradient(90deg, #eef2f7, #e0e7ff); margin-top: 8px; border-radius: 2px; }

/* Item một dòng */
.form-row-item { margin-bottom: 12px; }

/* Label và input của form */
::v-deep .form-group label { font-weight: 600; color: #334155; margin-bottom: var(--ui-label-gap); }
::v-deep .form-group .form-text { font-size: 0.85rem; }
::v-deep .form-control { border-color: #e5e7eb; border-radius: 10px; transition: box-shadow .12s ease, border-color .12s ease; }
::v-deep .form-control:focus { box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15); border-color: #818cf8; }
::v-deep .custom-select { border-color: #e5e7eb; border-radius: 10px; }

/* Bộ tạo serial */
.serial-builder { border: 1px dashed #e5e7eb; border-radius: 12px; padding: 12px; background: #fafafa; }
.serial-full-label { font-weight: 600; color: #6b7280; display: block; margin-bottom: var(--ui-label-gap); }
.serial-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.serial-item small { display: block; margin-top: 6px; }
.serial-label { font-weight: 600; color: #374151; margin-bottom: var(--ui-label-gap); }
.serial-top { display: flex; align-items: center; gap: 8px; margin-bottom: var(--ui-label-gap); }
.step-badge { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; font-size: 12px; border-radius: 9999px; background: #eef2ff; color: #4f46e5; font-weight: 700; border: 1px solid #e0e7ff; }
.serial-suffix { grid-column: span 4; }

/* Xem trước */
.preview-card { border-color: #e9eef5; }
.preview-wrapper { border: 1px solid #eef2f7; border-radius: 12px; overflow: hidden; background: #fff; }
.img-fluid { max-width: 100%; height: auto; display: block; }
::v-deep .card-header { background: #f9fafb; font-weight: 600; }

/* Hành động */
.form-actions { padding-top: 8px; }
.form-actions .btn { min-width: 150px; }
.sticky-actions { position: sticky; bottom: 0; background: linear-gradient(180deg, rgba(255,255,255,0.65), #fff); padding: 10px 0; z-index: 2; border-top: 1px solid #eef2f7; }

/* Nút đã tinh chỉnh */
.btn-back, .btn-save { display: inline-flex; align-items: center; gap: 8px; font-weight: 600; border-radius: 10px; padding: 10px 16px; transition: transform .08s ease, box-shadow .12s ease, background-color .12s ease; }
.btn-back i, .btn-save i { font-size: 0.95rem; }
.btn-back { background-color: #f3f4f6; color: #374151; border-color: #e5e7eb; }
.btn-back:hover { background-color: #e5e7eb; box-shadow: 0 2px 8px rgba(17,24,39,0.06); }
.btn-back:active { transform: translateY(1px); }

.btn-save { background-color: #4f46e5; border-color: #4f46e5; }
.btn-save:hover { background-color: #4338ca; border-color: #4338ca; box-shadow: 0 4px 12px rgba(79,70,229,0.35); }
.btn-save:focus { box-shadow: 0 0 0 3px rgba(99,102,241,0.35); }
.btn-save:active { transform: translateY(1px); }
.btn-save[disabled] { opacity: 0.8; cursor: not-allowed; }

/* Tinh chỉnh responsive */
@media (max-width: 992px) {
  .layout-grid.layout-left-preview { grid-template-columns: 1fr; }
  .layout-aside { position: static; }
}
@media (max-width: 768px) {
  .serial-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .serial-suffix { grid-column: span 2; }
}
</style>
