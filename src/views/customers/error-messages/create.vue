<template>
  <div class="container-fluid py-3 error-message-create">
    <div class="d-flex align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Thông báo hóa đơn điện tử có sai sót</h4>
      <b-badge variant="light">{{ isEdit ? 'Cập nhật' : 'Lập mới' }}</b-badge>
    </div>

    <b-card class="shadow-sm">
      <b-alert v-if="formErrorList.length" show variant="danger">
        <div v-for="msg in formErrorList" :key="msg">{{ msg }}</div>
      </b-alert>

      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Thông tin thông báo</b-card-header>
        <b-card-body>
          <b-row>
            <b-col md="6">
              <b-form-group label="Loại thông báo" label-class="font-weight-bold">
                <b-form-radio-group v-model.number="frm.notificationType" :options="notificationTypeOptions" />
              </b-form-group>
            </b-col>
            <b-col md="3">
              <b-form-group label="Mẫu số" label-class="font-weight-bold">
                <b-form-input v-model.trim="frm.formPattern" disabled />
              </b-form-group>
            </b-col>
            <b-col md="3">
              <b-form-group label="Ngày lập thông báo" label-class="font-weight-bold">
                <b-form-datepicker v-model="frm.noticeDate" locale="vi" :date-format-options="dateFmt" :state="state('noticeDate')" />
                <b-form-invalid-feedback :state="state('noticeDate')">{{ errors.noticeDate }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
          </b-row>
          <b-row v-if="Number(frm.notificationType) === 2">
            <b-col md="6">
              <b-form-group label="Số thông báo rà soát CQT" label-class="font-weight-bold">
                <b-form-input v-model.trim="frm.taxNoticeNumber" :state="state('taxNoticeNumber')" />
                <b-form-invalid-feedback :state="state('taxNoticeNumber')">{{ errors.taxNoticeNumber }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
            <b-col md="6">
              <b-form-group label="Ngày thông báo rà soát CQT" label-class="font-weight-bold">
                <b-form-datepicker v-model="frm.taxNoticeDate" locale="vi" :date-format-options="dateFmt" :state="state('taxNoticeDate')" />
                <b-form-invalid-feedback :state="state('taxNoticeDate')">{{ errors.taxNoticeDate }}</b-form-invalid-feedback>
              </b-form-group>
            </b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold">Thông tin người nộp thuế</b-card-header>
        <b-card-body>
          <b-row>
            <b-col md="6"><b>Người nộp thuế:</b> {{ company.taxpayerName || '—' }}</b-col>
            <b-col md="3"><b>MST:</b> {{ company.taxCode || '—' }}</b-col>
            <b-col md="3"><b>Mã CQT:</b> {{ company.taxAuthorityCode || '—' }}</b-col>
          </b-row>
          <b-row class="mt-2">
            <b-col md="12"><b>Cơ quan thuế:</b> {{ company.taxAuthorityName || '—' }}</b-col>
          </b-row>
        </b-card-body>
      </b-card>

      <b-card no-body class="mb-3">
        <b-card-header class="bg-light font-weight-bold d-flex justify-content-between align-items-center">
          <span>Danh sách hóa đơn sai sót</span>
          <div>
            <b-button size="sm" variant="outline-secondary" class="mr-2" @click="addBlankLine"><i class="fas fa-plus"></i> Thêm dòng</b-button>
            <b-button size="sm" variant="outline-primary" @click="$refs.invoiceModal.show()"><i class="fas fa-search"></i> Chọn hóa đơn</b-button>
          </div>
        </b-card-header>
        <b-card-body>
          <div class="invoice-table-wrap">
            <b-table-simple class="invoice-error-table" bordered small>
              <colgroup>
                <col class="invoice-col-index">
                <col class="invoice-col-tax">
                <col class="invoice-col-pattern">
                <col class="invoice-col-serial">
                <col class="invoice-col-no">
                <col class="invoice-col-date">
                <col class="invoice-col-type">
                <col class="invoice-col-error">
                <col class="invoice-col-option">
              </colgroup>
              <b-thead>
                <b-tr>
                  <b-th class="invoice-col-index text-center">#</b-th>
                  <b-th class="invoice-col-tax">Mã CQT</b-th>
                  <b-th class="invoice-col-pattern">Mẫu số</b-th>
                  <b-th class="invoice-col-serial">Ký hiệu</b-th>
                  <b-th class="invoice-col-no">Số HĐ</b-th>
                  <b-th class="invoice-col-date">Ngày HĐ</b-th>
                  <b-th class="invoice-col-type">Loại áp dụng</b-th>
                  <b-th class="invoice-col-error">Tính chất</b-th>
                  <b-th class="invoice-col-option text-center"></b-th>
                </b-tr>
              </b-thead>
              <b-tbody>
                <template v-for="(item, index) in frm.invoices">
                  <b-tr :key="`line-${index}`">
                    <b-td class="text-center">{{ index + 1 }}</b-td>
                    <b-td><b-form-input size="sm" v-model.trim="item.taxCode" /></b-td>
                    <b-td><b-form-input size="sm" v-model.trim="item.formSymbol" /></b-td>
                    <b-td><b-form-input size="sm" v-model.trim="item.serial" /></b-td>
                    <b-td><b-form-input size="sm" v-model.trim="item.invoiceNo" /></b-td>
                    <b-td><b-form-input size="sm" type="date" v-model="item.invoiceDate" /></b-td>
                    <b-td><b-form-select size="sm" v-model.number="item.invoiceType" :options="invoiceTypeOptions" /></b-td>
                    <b-td><b-form-select size="sm" v-model.number="item.errorType" :options="errorTypeOptions" /></b-td>
                    <b-td class="text-center">
                      <b-button size="sm" variant="outline-danger" @click="removeLine(index)">
                        <i class="far fa-trash-alt"></i>
                      </b-button>
                    </b-td>
                  </b-tr>
                  <b-tr :key="`reason-${index}`" class="invoice-reason-row">
                    <b-td></b-td>
                    <b-td colspan="8">
                      <b-form-group label="Lý do sai sót" label-class="invoice-reason-label" class="mb-0">
                        <b-form-textarea size="sm" rows="2" max-rows="4" v-model.trim="item.reason" />
                      </b-form-group>
                    </b-td>
                  </b-tr>
                </template>
                <b-tr v-if="!frm.invoices.length">
                  <b-td colspan="9" class="text-center text-muted py-4">Chưa có hóa đơn</b-td>
                </b-tr>
              </b-tbody>
            </b-table-simple>
          </div>
          <p v-if="!state('invoices')" class="text-danger small">{{ errors.invoices }}</p>
        </b-card-body>
      </b-card>

      <b-row class="pt-2 pb-2 align-items-start">
        <b-col cols="12" md="6">
          <b-form-group label="Nơi lập" label-class="font-weight-bold">
            <v-select v-model="frm.createPlace" :options="provinces" label="name" :reduce="v => String(v.id)" placeholder="Chọn nơi lập" />
            <p v-if="!state('createPlace')" class="text-danger small mt-2">{{ errors.createPlace }}</p>
          </b-form-group>
        </b-col>
        <b-col cols="12" md="6" class="text-center signer-panel">
          <p>{{ selectedPlaceName || '—' }}, ngày {{ formatDate(frm.noticeDate || new Date()) }}</p>
          <p class="text-uppercase font-weight-bold">Người nộp thuế</p>
          <p><i>(Chữ ký số, chữ ký điện tử người nộp thuế)</i></p>
          <b-card class="text-center w-75 m-auto bg-light" :border-variant="isEdit && Number(frm.status) > 0 ? 'success' : ''">
            <div class="text-danger" v-if="isEdit && Number(frm.status) > 0">
              <p class="font-weight-bold pb-1 m-0">Signature Valid <i class="fas fa-check text-success"></i></p>
              <p v-if="frm.signDate" class="font-weight-bold m-0">Ngày ký: {{ formatDateTime(frm.signDate) }}</p>
            </div>
            <div v-else>
              <i class="fas fa-signature"></i>
              <div class="d-inline-block" v-if="isEdit">
                <b-dropdown v-if="!btnSignature" size="sm" variant="link" toggle-class="text-decoration-none" text="Thực hiện ký số" boundary="window">
                  <b-dropdown-item href="#" @click.prevent="signData">
                    <i class="fas fa-usb mr-1"></i>
                    Ký USB token
                  </b-dropdown-item>
                </b-dropdown>
                <b-button class="text-decoration-none" variant="link" size="sm" v-else disabled>Đang thực hiện...</b-button>
              </div>
            </div>
          </b-card>
        </b-col>
      </b-row>

      <div class="action-bar mt-3 pt-3">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <b-button size="sm" variant="outline-secondary" @click="$router.push({ name: 'CustomerErrorMessageList' })">
              <i class="fas fa-arrow-left"></i> Quay lại
            </b-button>
          </div>
          <div>
            <b-button v-if="Number(frm.status) === 0" size="sm" variant="primary" class="mr-2" :disabled="btnLoading" @click="save">
              <i class="fas fa-save"></i> {{ isEdit ? 'Cập nhật' : 'Lưu' }}
            </b-button>
            <b-button v-if="canSend" size="sm" variant="success" :disabled="btnLoading" @click="sendData">
              <i class="far fa-paper-plane"></i> Gửi CQT
            </b-button>
          </div>
        </div>
      </div>
    </b-card>

    <b-modal ref="invoiceModal" size="xl" title="Chọn hóa đơn sai sót">
      <b-row class="mb-2">
        <b-col md="3"><b-form-datepicker v-model="invoiceSearch.dateFrom" placeholder="Từ ngày" locale="vi" size="sm" :date-format-options="dateFmt" /></b-col>
        <b-col md="3"><b-form-datepicker v-model="invoiceSearch.dateTo" placeholder="Đến ngày" locale="vi" size="sm" :date-format-options="dateFmt" /></b-col>
        <b-col md="3"><b-form-input v-model.trim="invoiceSearch.keyword" size="sm" placeholder="Khách hàng / mã CQT" @keyup.enter="searchInvoices" /></b-col>
        <b-col md="2"><b-form-input v-model.number="invoiceSearch.no" size="sm" placeholder="Số HĐ" @keyup.enter="searchInvoices" /></b-col>
        <b-col md="1"><b-button size="sm" variant="primary" @click="searchInvoices">Tìm</b-button></b-col>
      </b-row>
      <b-table bordered small responsive show-empty :busy="invoiceBusy" :items="invoiceList" :fields="searchFields" empty-text="Không có dữ liệu">
        <template #cell(select)="{ item }"><b-form-checkbox :checked="selectedInvoices.includes(item.id)" @change="v => toggleInvoice(item.id, v)" /></template>
        <template #cell(invoiceDate)="{ item }">{{ formatDate(item.invoiceDate) }}</template>
        <template #cell(amount)="{ item }">{{ formatCurrency(item.amount) }}</template>
      </b-table>
      <template #modal-footer>
        <b-button size="sm" variant="light" @click="$refs.invoiceModal.hide()">Đóng</b-button>
        <b-button size="sm" variant="primary" :disabled="!selectedInvoices.length" @click="addSelectedInvoices">Đồng ý</b-button>
      </template>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'
import { required, hasErrors, firstError } from '@/utils/validators'
import { toastError, toastSuccess, toastWarning } from '@/utils/toast'
import { confirmUsbTokenSignature, startSignaturePolling } from '@/utils/pasigner'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function defaultForm() {
  const today = todayIso()
  return {
    formPattern: '04/SS-HĐĐT',
    notificationType: 1,
    taxNoticeNumber: '',
    taxNoticeDate: today,
    createPlace: '',
    noticeDate: today,
    documentType: 'INVOICE',
    invoices: [],
    status: 0,
    signDate: null
  }
}

export default {
  name: 'ErrorMessageCreate',
  components: { 'v-select': vSelect },
  data() {
    return {
      btnLoading: false,
      btnSignature: false,
      signaturePollCancel: null,
      invoiceBusy: false,
      provinces: [],
      company: {},
      errors: {},
      dateFmt: { day: '2-digit', month: '2-digit', year: 'numeric' },
      frm: defaultForm(),
      invoiceSearch: { keyword: '', dateFrom: null, dateTo: null, no: null },
      invoiceList: [],
      selectedInvoices: [],
      notificationTypeOptions: [
        { value: 1, text: 'Thông báo của NNT' },
        { value: 2, text: 'Theo thông báo của CQT' }
      ],
      invoiceTypeOptions: [
        { value: 1, text: 'HĐĐT theo Nghị định 123' },
        { value: 2, text: 'HĐ có mã xác thực NĐ 51/04' },
        { value: 3, text: 'HĐ theo NĐ 51/04' },
        { value: 4, text: 'Hóa đơn đặt in NĐ 123' }
      ],
      errorTypeOptions: [
        { value: 0, text: 'Hóa đơn mới' },
        { value: 1, text: 'Hóa đơn hủy' },
        { value: 2, text: 'Điều chỉnh' },
        { value: 3, text: 'Thay thế' },
        { value: 4, text: 'Giải trình' },
        { value: 5, text: 'Tổng hợp' }
      ],
      searchFields: [
        { key: 'select', label: '', thStyle: { width: '4%' } },
        { key: 'formSymbol', label: 'Mẫu số', thStyle: { width: '9%' } },
        { key: 'serial', label: 'Ký hiệu', thStyle: { width: '10%' } },
        { key: 'invoiceNo', label: 'Số HĐ', thStyle: { width: '9%' } },
        { key: 'invoiceDate', label: 'Ngày HĐ', thStyle: { width: '12%' } },
        { key: 'customerName', label: 'Khách hàng', thStyle: { width: '24%' } },
        { key: 'taxCode', label: 'Mã CQT', thStyle: { width: '20%' } },
        { key: 'amount', label: 'Tổng tiền', thStyle: { width: '12%' } }
      ]
    }
  },
  computed: {
    isEdit() { return !!this.$route.params.id },
    formErrorList() { return Object.values(this.errors || {}).filter(Boolean) },
    canSend() { return this.isEdit && [1, 3, 4].includes(Number(this.frm.status)) },
    selectedPlaceName() {
      const selected = this.provinces.find(v => String(v.id) === String(this.frm.createPlace))
      return selected ? selected.name : ''
    }
  },
  created() { this.bootstrap() },
  beforeDestroy() {
    if (this.signaturePollCancel) this.signaturePollCancel()
  },
  watch: {
    '$route.params.id'(id, oldId) {
      this.errors = {}
      if (id && id !== oldId) {
        this.loadDetail(id)
      } else if (!id && oldId) {
        this.resetCreateForm()
      }
    }
  },
  methods: {
    resetCreateForm() {
      this.frm = defaultForm()
      this.invoiceList = []
      this.selectedInvoices = []
      this.loadPrefill()
    },
    async bootstrap() {
      await Promise.all([this.loadProvinces(), this.loadPrefill()])
      if (this.isEdit) await this.loadDetail(this.$route.params.id)
      const ids = String(this.$route.query.ids || '').split('_').map(Number).filter(Boolean)
      if (ids.length) {
        await this.loadInvoicesByIds(ids)
      }
    },
    async loadProvinces() {
      try { const { data } = await axios.get('/provinces'); this.provinces = Array.isArray(data) ? data : [] } catch { this.provinces = [] }
    },
    async loadPrefill() {
      const { data } = await axios.get('/error-messages/prefill')
      this.company = data || {}
      this.frm.formPattern = data.formPattern || this.frm.formPattern
      this.frm.noticeDate = data.noticeDate || this.frm.noticeDate
    },
    async loadDetail(id) {
      const { data } = await axios.get(`/error-messages/${id}`)
      this.frm = {
        ...this.frm,
        ...data,
        taxNoticeDate: data.taxNoticeDate || this.frm.taxNoticeDate,
        invoices: Array.isArray(data.invoices) ? data.invoices.map(this.normalizeLine) : [],
        status: Number(data.status) || 0
      }
    },
    normalizeLine(row) {
      return {
        invoiceId: row.invoiceId || row.id || null,
        taxCode: row.taxCode || '',
        formSymbol: row.formSymbol || '',
        serial: row.serial || '',
        invoiceNo: row.invoiceNo || '',
        invoiceDate: (row.invoiceDate || '').toString().slice(0, 10),
        invoiceType: Number(row.invoiceType) || 1,
        errorType: Number(row.errorType) || 0,
        reason: row.reason || ''
      }
    },
    state(field) { return this.errors[field] ? false : null },
    validate() {
      this.errors = {
        noticeDate: required(this.frm.noticeDate, 'Vui lòng chọn ngày lập thông báo'),
        createPlace: required(this.frm.createPlace, 'Vui lòng chọn nơi lập'),
        taxNoticeNumber: Number(this.frm.notificationType) === 2 ? required(this.frm.taxNoticeNumber, 'Vui lòng nhập số thông báo CQT') : null,
        taxNoticeDate: Number(this.frm.notificationType) === 2 ? required(this.frm.taxNoticeDate, 'Vui lòng chọn ngày thông báo CQT') : null,
        invoices: this.frm.invoices.length ? null : 'Vui lòng chọn ít nhất một hóa đơn sai sót'
      }
      Object.keys(this.errors).forEach(k => { if (!this.errors[k]) delete this.errors[k] })
      return !hasErrors(this.errors)
    },
    payload() {
      return {
        formPattern: this.frm.formPattern,
        notificationType: this.frm.notificationType,
        taxNoticeNumber: this.frm.taxNoticeNumber,
        taxNoticeDate: this.frm.taxNoticeDate,
        createPlace: this.frm.createPlace,
        noticeDate: this.frm.noticeDate,
        documentType: this.frm.documentType,
        invoices: this.frm.invoices
      }
    },
    async save() {
      if (!this.validate()) {
        toastError(firstError(Object.values(this.errors)) || 'Vui lòng kiểm tra lại dữ liệu')
        return
      }
      this.btnLoading = true
      try {
        if (this.isEdit) {
          await axios.put(`/error-messages/${this.$route.params.id}`, this.payload(), { successMessage: 'Đã cập nhật thông báo' })
          await this.loadDetail(this.$route.params.id)
        } else {
          const { data } = await axios.post('/error-messages', this.payload(), { successMessage: 'Đã lập thông báo' })
          this.$router.push({ name: 'CustomerErrorMessageEdit', params: { id: data.id } })
        }
      } finally { this.btnLoading = false }
    },
    async searchInvoices() {
      this.invoiceBusy = true
      try {
        const { data } = await axios.get('/error-messages/invoices/search', { params: { ...this.invoiceSearch, size: 50 } })
        this.invoiceList = Array.isArray(data.data) ? data.data : []
      } finally { this.invoiceBusy = false }
    },
    async loadInvoicesByIds(ids) {
      const { data } = await axios.post('/error-messages/invoices/by-ids', { ids })
      const rows = Array.isArray(data) ? data : []
      const exists = new Set(this.frm.invoices.map(x => Number(x.invoiceId)))
      rows.forEach(row => {
        this.addInvoiceLine(row, exists)
      })
    },
    toggleInvoice(id, checked) {
      if (checked && !this.selectedInvoices.includes(id)) this.selectedInvoices.push(id)
      if (!checked) this.selectedInvoices = this.selectedInvoices.filter(x => x !== id)
    },
    addSelectedInvoices() {
      const exists = new Set(this.frm.invoices.map(x => Number(x.invoiceId)))
      this.invoiceList.filter(x => this.selectedInvoices.includes(x.id)).forEach(row => {
        this.addInvoiceLine(row, exists)
      })
      this.selectedInvoices = []
      this.$refs.invoiceModal && this.$refs.invoiceModal.hide()
    },
    isBlankLine(line) {
      if (!line || line.invoiceId) return false
      return ['taxCode', 'formSymbol', 'serial', 'invoiceNo', 'invoiceDate', 'reason'].every(key => !line[key])
    },
    addInvoiceLine(row, exists) {
      const invoiceId = Number(row.id)
      if (exists.has(invoiceId)) return

      const line = this.normalizeLine({ ...row, invoiceId: row.id })
      const blankIndex = this.frm.invoices.findIndex(this.isBlankLine)
      if (blankIndex >= 0) this.$set(this.frm.invoices, blankIndex, line)
      else this.frm.invoices.push(line)
      exists.add(invoiceId)
    },
    removeLine(index) { this.frm.invoices.splice(index, 1) },
    addBlankLine() { this.frm.invoices.push(this.normalizeLine({})) },
    async signData() {
      try {
        const ok = await confirmUsbTokenSignature(`Bạn có chắc chắn muốn ký số thông báo sai sót #${this.$route.params.id} bằng USB token?`)
        if (!ok) return
        this.btnSignature = true
        const { data } = await axios.post(`/error-messages/${this.$route.params.id}/sign-token/prepare`)
        if (!data?.hash) throw new Error('Không tạo được mã hash ký số')
        location.href = 'pasigner://' + encodeURIComponent(JSON.stringify({ src: 'web', action: 'signature', pid: Math.random().toString(36).slice(2, 8), data: { hash: data.hash, domain: window.location.hostname } }))
        toastSuccess('Đã gửi yêu cầu đến ứng dụng ký số USB token')
        this.pollSignatureResult(data.hash, this.$route.params.id)
      } catch (e) {
        toastError(e?.response?.data?.message || e?.message || 'Không thể ký số')
      } finally { this.btnSignature = false }
    },
    pollSignatureResult(hash, id) {
      if (this.signaturePollCancel) this.signaturePollCancel()
      this.signaturePollCancel = startSignaturePolling({
        checkStatus: async () => {
          const { data } = await axios.get(`/error-messages/${id}/sign-token/status`, {
            params: { hash },
            meta: { suppressGlobalErrorToast: true }
          })
          return data
        },
        onSigned: async () => {
          this.signaturePollCancel = null
          toastSuccess('Ký số thông báo sai sót thành công')
          await this.loadDetail(id)
        },
        onFailed: () => {
          this.signaturePollCancel = null
          toastWarning('P.A Signer không trả về dữ liệu thông báo hợp lệ. Vui lòng ký lại.')
        },
        onTimeout: () => {
          this.signaturePollCancel = null
          toastWarning('Chưa nhận được dữ liệu ký thông báo. Hãy kiểm tra P.A Signer hoặc thử lại.')
        }
      })
    },
    async sendData() {
      const ok = await this.$bvModal.msgBoxConfirm('Gửi thông báo sai sót lên Cơ quan thuế?', { title: 'Xác nhận gửi CQT', okTitle: 'Gửi', cancelTitle: 'Hủy', okVariant: 'success' })
      if (!ok) return
      await axios.post(`/error-messages/${this.$route.params.id}/send`, null, { successMessage: 'Đã gửi thông báo lên CQT' })
      await this.loadDetail(this.$route.params.id)
    },
    formatDate(v) { return v ? new Date(v).toLocaleDateString('vi-VN') : '—' },
    formatDateTime(v) { return v ? new Date(v).toLocaleString('vi-VN') : '—' },
    formatCurrency(v) { return Number(v || 0).toLocaleString('vi-VN') }
  }
}
</script>

<style scoped>
.error-message-create::v-deep .table { table-layout: fixed; }
.error-message-create::v-deep th,
.error-message-create::v-deep td { overflow-wrap: anywhere; vertical-align: middle; }
.error-message-create::v-deep .vs__dropdown-toggle { min-height: 38px; }

.error-message-create::v-deep .invoice-error-table {
  font-size: 0.78rem;
  margin-bottom: 0;
  table-layout: fixed;
  width: 100%;
}

.invoice-table-wrap {
  border: 1px solid #dee2e6;
  border-radius: 6px;
  overflow: visible;
}

.error-message-create::v-deep .invoice-error-table th,
.error-message-create::v-deep .invoice-error-table td {
  line-height: 1.3;
  padding: 0.3rem;
}

.error-message-create::v-deep .invoice-error-table .form-control,
.error-message-create::v-deep .invoice-error-table .custom-select {
  font-size: 0.78rem;
  min-width: 0;
  padding-left: 0.3rem;
  padding-right: 0.3rem;
  width: 100%;
}

.error-message-create::v-deep .invoice-error-table .custom-select {
  background-position: right 0.25rem center;
}

.invoice-col-index {
  width: 4%;
}

.invoice-col-tax {
  width: 13%;
}

.invoice-col-pattern,
.invoice-col-serial,
.invoice-col-no {
  width: 9%;
}

.invoice-col-date {
  width: 13%;
}

.invoice-col-type {
  width: 20%;
}

.invoice-col-error {
  width: 16%;
}

.invoice-col-option {
  width: 7%;
}

.error-message-create::v-deep .invoice-error-table .invoice-col-option,
.error-message-create::v-deep .invoice-error-table td:last-child {
  padding-left: 0.35rem;
  padding-right: 0.35rem;
  white-space: nowrap;
}

.error-message-create::v-deep .invoice-error-table td:last-child .btn {
  height: 30px;
  padding: 0;
  width: 32px;
}

.invoice-reason-row td {
  background: #fafbfc;
  border-top: 0;
  padding-bottom: 0.6rem;
  padding-top: 0;
}

.invoice-reason-row textarea {
  min-height: 62px;
  resize: vertical;
}

.error-message-create::v-deep .invoice-reason-label {
  color: #495057;
  font-size: 0.8125rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.signer-panel p {
  margin-bottom: 0.35rem;
}

.action-bar {
  border-top: 1px solid #eef2f7;
}

@media (max-width: 768px) {
  .signer-panel .card {
    width: 100% !important;
  }
}
</style>
